package com.his.service.impl;

import com.his.dto.CreateChargeDTO;
import com.his.dto.PaymentDTO;
import com.his.entity.*;
import com.his.enums.ChargeStatusEnum;
import com.his.enums.PrescriptionStatusEnum;
import com.his.enums.RegStatusEnum;
import com.his.monitoring.SequenceGenerationMetrics;
import com.his.repository.*;
import com.his.service.ChargeService;
import com.his.service.PrescriptionService;
import com.his.vo.ChargeVO;
import com.his.vo.DailySettlementVO;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 收费服务实现类
 *
 * <p>负责医院收费相关的核心业务逻辑，包括挂号收费、处方收费、退费、结算等</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li>创建收费单：支持挂号费、处方费、混合收费等多种收费类型</li>
 *   <li>支付处理：支持现金、医保、银行卡等多种支付方式，包含幂等性保证</li>
 *   <li>退费处理：按原支付路径退费，自动恢复处方库存</li>
 *   <li>日结算：生成收费员的日结算报表，包含支付方式统计和退费统计</li>
 *   <li>分阶段收费：支持先缴挂号费、后缴处方费的灵活收费模式</li>
 * </ul>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li>收费单号使用PostgreSQL序列生成，保证唯一性和并发安全性</li>
 *   <li>支付后不可修改，只能退费</li>
 *   <li>退费金额不超过原收费金额</li>
 *   <li>处方收费要求处方必须已审核通过（REVIEWED状态）</li>
 *   <li>防止重复收费：挂号费和处方费都进行支付状态检查</li>
 *   <li>日结算包含已支付和已退费的收费单</li>
 * </ul>
 *
 * <h3>收费类型</h3>
 * <ul>
 *   <li>类型1：仅挂号费</li>
 *   <li>类型2：仅处方费</li>
 *   <li>类型3：混合收费（挂号费+处方费）</li>
 * </ul>
 *
 * <h3>状态流转</h3>
 * <ul>
 *   <li>UNPAID（未支付）→ PAID（已支付）</li>
 *   <li>PAID（已支付）→ REFUNDED（已退费）</li>
 * </ul>
 *
 * <h3>相关实体</h3>
 * <ul>
 *   <li>{@link com.his.entity.Charge} - 收费单主表</li>
 *   <li>{@link com.his.entity.ChargeDetail} - 收费明细表</li>
 *   <li>{@link com.his.entity.Registration} - 挂号单（支付后更新状态）</li>
 *   <li>{@link com.his.entity.Prescription} - 处方（支付后更新状态）</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.ChargeService
 * @see com.his.repository.ChargeRepository
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChargeServiceImpl implements ChargeService {

    private final ChargeRepository chargeRepository;
    private final ChargeDetailRepository chargeDetailRepository;
    private final RegistrationRepository registrationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionService prescriptionService;

    // 监控指标
    private final SequenceGenerationMetrics sequenceMetrics;

    /**
     * 创建收费单
     *
     * <p>根据挂号单和处方信息创建收费记录，支持灵活的分阶段收费模式</p>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>仅挂号收费：挂号单状态必须为 WAITING 或 PAID_REGISTRATION</li>
     *   <li>处方收费：挂号单状态必须为 COMPLETED 或 PAID_REGISTRATION</li>
     *   <li>处方必须为已审核状态（REVIEWED）才能收费</li>
     *   <li>防止重复收费：已缴费的挂号费或处方费不能再次收费</li>
     *   <li>混合收费：如果挂号费未支付，处方收费时会一并收取挂号费</li>
     * </ul>
     *
     * <p><b>收费类型判断：</b></p>
     * <ul>
     *   <li>类型1（仅挂号费）：仅传 registrationId，不传 prescriptionIds</li>
     *   <li>类型2（仅处方费）：传 registrationId 和 prescriptionIds，且挂号费已支付</li>
     *   <li>类型3（混合收费）：传 registrationId 和 prescriptionIds，且挂号费未支付</li>
     * </ul>
     *
     * <p><b>前置条件：</b></p>
     * <ul>
     *   <li>挂号单存在且未删除</li>
     *   <li>挂号单状态符合收费条件</li>
     *   <li>处方（如果有）存在且已审核</li>
     * </ul>
     *
     * <p><b>后置条件：</b></p>
     * <ul>
     *   <li>创建收费单记录（状态为UNPAID）</li>
     *   <li>创建收费明细记录（挂号费、处方费）</li>
     *   <li>生成唯一的收费单号</li>
     * </ul>
     *
     * @param dto 收费单创建DTO
     *            <ul>
     *              <li>registrationId: 挂号单ID（必填）</li>
     *              <li>prescriptionIds: 处方ID列表（可选，为空则仅收挂号费）</li>
     *            </ul>
     * @return 收费单视图对象（ChargeVO），包含收费单号、总金额、明细等
     * @throws IllegalArgumentException 如果挂号单不存在、已被删除、状态不正确
     * @throws IllegalArgumentException 如果处方不存在、未审核
     * @throws IllegalStateException 如果挂号费或处方费已支付
     * @since 1.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChargeVO createCharge(CreateChargeDTO dto) {
        log.info("开始创建收费单，挂号单ID: {}", dto.getRegistrationId());

        Registration registration = registrationRepository.findById(dto.getRegistrationId())
                .orElseThrow(() -> new IllegalArgumentException("挂号单不存在，ID: " + dto.getRegistrationId()));

        if (registration.getIsDeleted() == 1) {
            throw new IllegalArgumentException("挂号单已被删除");
        }

        // 【改造】支持分阶段收费：根据是否包含处方决定状态检查逻辑
        boolean isPrescriptionCharge = dto.getPrescriptionIds() != null && !dto.getPrescriptionIds().isEmpty();

        if (isPrescriptionCharge) {
            // 处方收费：要求已就诊（COMPLETED）或已缴挂号费（PAID_REGISTRATION，允许混合收费）
            Short status = registration.getStatus();
            if (!RegStatusEnum.COMPLETED.getCode().equals(status) &&
                !RegStatusEnum.PAID_REGISTRATION.getCode().equals(status)) {
                throw new IllegalArgumentException("处方收费需要挂号单状态为已缴挂号费或已就诊");
            }
        } else {
            // 仅挂号收费：允许 WAITING 或 PAID_REGISTRATION 状态
            Short status = registration.getStatus();
            if (!RegStatusEnum.WAITING.getCode().equals(status) &&
                !RegStatusEnum.PAID_REGISTRATION.getCode().equals(status)) {
                throw new IllegalArgumentException("挂号收费需要挂号单状态为待就诊或已缴挂号费");
            }

            // 【新增】防止重复收取挂号费
            if (isRegistrationFeePaidInternal(registration.getMainId())) {
                throw new IllegalStateException("该挂号单的挂号费已支付，请勿重复收费");
            }
        }

        List<Prescription> prescriptions = new ArrayList<>();
        if (dto.getPrescriptionIds() != null && !dto.getPrescriptionIds().isEmpty()) {
            prescriptions = prescriptionRepository.findAllById(dto.getPrescriptionIds());
            if (prescriptions.size() != dto.getPrescriptionIds().size()) {
                throw new IllegalArgumentException("部分处方不存在");
            }
            for (Prescription p : prescriptions) {
                if (!PrescriptionStatusEnum.REVIEWED.getCode().equals(p.getStatus())) {
                    throw new IllegalArgumentException("处方 [" + p.getPrescriptionNo() + "] 未通过审核，无法收费");
                }
                // 【新增】防止重复收取处方费
                if (isPrescriptionFeePaid(p.getMainId())) {
                    throw new IllegalStateException("处方 [" + p.getPrescriptionNo() + "] 的费用已支付，请勿重复收费");
                }
            }
        }

        List<ChargeDetail> details = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 【改造】根据是否已支付挂号费决定是否添加挂号费明细
        boolean includeRegistrationFee = false;
        if (!isPrescriptionCharge) {
            // 仅挂号收费场景：添加挂号费
            includeRegistrationFee = true;
        } else {
            // 处方收费场景：检查挂号费是否已支付，如果未支付则一并收取（混合收费）
            if (!isRegistrationFeePaidInternal(registration.getMainId())) {
                includeRegistrationFee = true;
            }
        }

        if (includeRegistrationFee && registration.getRegistrationFee() != null
            && registration.getRegistrationFee().compareTo(BigDecimal.ZERO) > 0) {
            ChargeDetail regDetail = new ChargeDetail();
            regDetail.setCharge(null);
            regDetail.setItemType("REGISTRATION");
            regDetail.setItemId(registration.getMainId());
            regDetail.setItemName("挂号费");
            regDetail.setItemAmount(registration.getRegistrationFee());
            details.add(regDetail);
            totalAmount = totalAmount.add(registration.getRegistrationFee());
        }

        for (Prescription p : prescriptions) {
            ChargeDetail pDetail = new ChargeDetail();
            pDetail.setCharge(null);
            pDetail.setItemType("PRESCRIPTION");
            pDetail.setItemId(p.getMainId());
            pDetail.setItemName("处方药费 (" + p.getPrescriptionNo() + ")");
            pDetail.setItemAmount(p.getTotalAmount());
            details.add(pDetail);
            totalAmount = totalAmount.add(p.getTotalAmount());
        }

        Charge charge = new Charge();
        charge.setPatient(registration.getPatient());
        charge.setRegistration(registration);
        charge.setChargeNo(generateChargeNo());
        // 【改造】区分收费类型：1=仅挂号费, 2=仅处方费, 3=混合收费（向后兼容）
        if (isPrescriptionCharge && includeRegistrationFee) {
            charge.setChargeType((short) 3); // 混合收费（挂号费+处方费）
        } else if (isPrescriptionCharge) {
            charge.setChargeType((short) 2); // 仅处方费
        } else {
            charge.setChargeType((short) 1); // 仅挂号费
        }
        charge.setTotalAmount(totalAmount);
        charge.setActualAmount(totalAmount);
        charge.setStatus(ChargeStatusEnum.UNPAID.getCode());
        charge.setIsDeleted((short) 0);
        
        Charge savedCharge = chargeRepository.save(charge);

        for (ChargeDetail d : details) {
            d.setCharge(savedCharge);
        }
        chargeDetailRepository.saveAll(details);
        savedCharge.setDetails(details);

        log.info("收费单创建成功，ID: {}, 单号: {}, 总金额: {}", savedCharge.getMainId(), savedCharge.getChargeNo(), totalAmount);

        return mapToVO(savedCharge);
    }

    /**
     * 根据ID查询收费单详情
     *
     * <p>查询指定ID的收费单及其明细信息</p>
     *
     * <p><b>查询内容：</b></p>
     * <ul>
     *   <li>收费单基本信息（单号、金额、状态、时间等）</li>
     *   <li>收费明细列表（挂号费、处方费等）</li>
     *   <li>患者基本信息（姓名）</li>
     * </ul>
     *
     * @param id 收费单ID
     * @return 收费单视图对象（ChargeVO）
     * @throws IllegalArgumentException 如果收费单不存在
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public ChargeVO getById(Long id) {
        Charge charge = chargeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("收费单不存在，ID: " + id));
        return mapToVO(charge);
    }

    /**
     * 处理收费单支付
     *
     * <p>处理收费单的支付请求，包含幂等性保证和状态同步更新</p>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>只处理状态为 UNPAID 的收费单</li>
     *   <li>支付金额必须与应付金额一致（允许0.01元的误差）</li>
     *   <li>支持交易流水号幂等性检查：相同流水号重复请求直接返回原支付结果</li>
     *   <li>支付成功后自动更新关联实体状态</li>
     * </ul>
     *
     * <p><b>幂等性保证：</b></p>
     * <ul>
     *   <li>如果交易流水号已存在且状态为PAID，直接返回原支付记录（幂等）</li>
     *   <li>如果交易流水号已存在但状态不一致，抛出异常</li>
     * </ul>
     *
     * <p><b>状态同步：</b></p>
     * <ul>
     *   <li>挂号费支付：挂号单状态从 WAITING 更新为 PAID_REGISTRATION</li>
     *   <li>处方费支付：处方状态从 REVIEWED 更新为 PAID</li>
     * </ul>
     *
     * <p><b>前置条件：</b></p>
     * <ul>
     *   <li>收费单存在且状态为 UNPAID</li>
     *   <li>支付金额与应付金额匹配</li>
     *   <li>交易流水号（如果有）未被使用</li>
     * </ul>
     *
     * <p><b>后置条件：</b></p>
     * <ul>
     *   <li>收费单状态更新为 PAID</li>
     *   <li>记录支付方式、交易流水号、支付时间</li>
     *   <li>关联的挂号单/处方状态同步更新</li>
     * </ul>
     *
     * @param id 收费单ID
     * @param dto 支付信息DTO
     *            <ul>
     *              <li>paidAmount: 实付金额（必须与应付金额一致）</li>
     *              <li>paymentMethod: 支付方式（现金、医保、银行卡等）</li>
     *              <li>transactionNo: 交易流水号（可选，用于幂等性控制）</li>
     *            </ul>
     * @return 支付后的收费单视图对象（ChargeVO）
     * @throws IllegalArgumentException 如果收费单不存在、状态不正确、金额不匹配
     * @throws IllegalArgumentException 如果交易流水号已存在但状态不一致
     * @since 1.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChargeVO processPayment(Long id, PaymentDTO dto) {
        log.info("开始处理支付，收费单ID: {}, 支付金额: {}", id, dto.getPaidAmount());

        if (dto.getTransactionNo() != null && !dto.getTransactionNo().isEmpty()) {
            var existingCharge = chargeRepository.findByTransactionNoWithDetails(dto.getTransactionNo());
            if (existingCharge.isPresent()) {
                Charge charge = existingCharge.get();
                if (ChargeStatusEnum.PAID.getCode().equals(charge.getStatus())) {
                    log.info("检测到重复支付请求（幂等），交易流水号: {}", dto.getTransactionNo());
                    return mapToVO(charge);
                } else {
                    throw new IllegalArgumentException("交易流水号已存在但状态不一致，流水号: " + dto.getTransactionNo());
                }
            }
        }

        Charge charge = chargeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("收费单不存在，ID: " + id));

        if (!ChargeStatusEnum.UNPAID.getCode().equals(charge.getStatus())) {
            throw new IllegalArgumentException("收费单状态不正确，当前状态: " + charge.getStatus());
        }

        if (charge.getActualAmount().subtract(dto.getPaidAmount()).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new IllegalArgumentException("支付金额不匹配，应付: " + charge.getActualAmount() + ", 实付: " + dto.getPaidAmount());
        }

        charge.setStatus(ChargeStatusEnum.PAID.getCode());
        charge.setPaymentMethod(dto.getPaymentMethod());
        charge.setTransactionNo(dto.getTransactionNo());
        charge.setChargeTime(LocalDateTime.now());
        
        Charge savedCharge = chargeRepository.save(charge);

        // 【改造】支付成功后更新关联实体状态
        if (charge.getDetails() != null) {
            for (ChargeDetail detail : charge.getDetails()) {
                if ("REGISTRATION".equals(detail.getItemType())) {
                    // 【新增】更新挂号状态为 PAID_REGISTRATION
                    Registration registration = charge.getRegistration();
                    if (registration != null && RegStatusEnum.WAITING.getCode().equals(registration.getStatus())) {
                        registration.setStatus(RegStatusEnum.PAID_REGISTRATION.getCode());
                        registration.setUpdatedAt(LocalDateTime.now());
                        registrationRepository.save(registration);
                        log.info("挂号状态已更新为已缴挂号费，挂号ID: {}", registration.getMainId());
                    }
                } else if ("PRESCRIPTION".equals(detail.getItemType())) {
                    Prescription prescription = prescriptionRepository.findById(detail.getItemId())
                            .orElseThrow(() -> new IllegalArgumentException("处方不存在，ID: " + detail.getItemId()));

                    if (PrescriptionStatusEnum.REVIEWED.getCode().equals(prescription.getStatus())) {
                         prescription.setStatus(PrescriptionStatusEnum.PAID.getCode());
                         prescription.setUpdatedAt(LocalDateTime.now());
                         prescriptionRepository.save(prescription);
                    }
                }
            }
        }

        log.info("支付成功，收费单ID: {}", id);
        return mapToVO(savedCharge);
    }

    /**
     * 处理收费单退费
     *
     * <p>处理已支付收费单的退费请求，自动恢复处方库存</p>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>只处理状态为 PAID 的收费单</li>
     *   <li>退费金额等于原实付金额</li>
     *   <li>退费后自动恢复关联实体的状态</li>
     *   <li>如果处方已发药，退费时恢复库存</li>
     * </ul>
     *
     * <p><b>状态回滚：</b></p>
     * <ul>
     *   <li>处方状态从 PAID 回滚到 REVIEWED（允许重新收费）</li>
     *   <li>处方状态从 DISPENSED 变更为 REFUNDED，并恢复库存</li>
     *   <li>挂号费不回滚状态（挂号有效）</li>
     * </ul>
     *
     * <p><b>前置条件：</b></p>
     * <ul>
     *   <li>收费单存在且状态为 PAID</li>
     *   <li>退费原因已提供</li>
     * </ul>
     *
     * <p><b>后置条件：</b></p>
     * <ul>
     *   <li>收费单状态更新为 REFUNDED</li>
     *   <li>记录退费原因、退费时间、退费金额</li>
     *   <li>关联的处方状态回滚或标记为已退费</li>
     *   <li>已发药处方的库存自动恢复</li>
     * </ul>
     *
     * @param id 收费单ID
     * @param refundReason 退费原因
     * @return 退费后的收费单视图对象（ChargeVO）
     * @throws IllegalArgumentException 如果收费单不存在
     * @throws IllegalStateException 如果收费单状态不是已支付
     * @since 1.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChargeVO processRefund(Long id, String refundReason) {
        log.info("开始处理退费，收费单ID: {}, 原因: {}", id, refundReason);

        Charge charge = chargeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("收费单不存在，ID: " + id));

        if (!ChargeStatusEnum.PAID.getCode().equals(charge.getStatus())) {
            throw new IllegalStateException("只有已缴费状态的收费单才能退费");
        }

        charge.setStatus(ChargeStatusEnum.REFUNDED.getCode());
        charge.setRefundReason(refundReason);
        charge.setRefundTime(LocalDateTime.now());
        charge.setRefundAmount(charge.getActualAmount());
        
        Charge savedCharge = chargeRepository.save(charge);

        if (charge.getDetails() != null) {
            for (ChargeDetail detail : charge.getDetails()) {
                if ("PRESCRIPTION".equals(detail.getItemType())) {
                    Prescription prescription = prescriptionRepository.findById(detail.getItemId())
                            .orElseThrow(() -> new IllegalArgumentException("处方不存在，ID: " + detail.getItemId()));
                    
                    if (PrescriptionStatusEnum.PAID.getCode().equals(prescription.getStatus())) {
                        prescription.setStatus(PrescriptionStatusEnum.REVIEWED.getCode());
                        prescription.setUpdatedAt(LocalDateTime.now());
                        prescriptionRepository.save(prescription);
                    } else if (PrescriptionStatusEnum.DISPENSED.getCode().equals(prescription.getStatus())) {
                        prescription.setStatus(PrescriptionStatusEnum.REFUNDED.getCode());
                        prescription.setUpdatedAt(LocalDateTime.now());
                        prescriptionRepository.save(prescription);
                        prescriptionService.restoreInventoryOnly(prescription.getMainId());
                    }
                }
            }
        }

        log.info("退费成功，收费单ID: {}", id);
        return mapToVO(savedCharge);
    }

    /**
     * 查询收费单列表（支持多条件分页查询）
     *
     * <p>根据多个条件组合查询收费单，所有条件都是可选的</p>
     *
     * <p><b>支持的查询条件：</b></p>
     * <ul>
     *   <li>收费单号：精确匹配</li>
     *   <li>患者ID：精确匹配</li>
     *   <li>收费状态：精确匹配（UNPAID/PAID/REFUNDED）</li>
     *   <li>创建时间范围：起止日期（闭区间）</li>
     * </ul>
     *
     * <p><b>默认过滤：</b></p>
     * <ul>
     *   <li>自动过滤已删除的收费单（isDeleted = 0）</li>
     * </ul>
     *
     * <p><b>排序：</b></p>
     * <ul>
     *   <li>通过 Pageable 参数控制排序字段和方向</li>
     *   <li>建议按创建时间倒序排列（最新的在前）</li>
     * </ul>
     *
     * @param chargeNo 收费单号（可选，精确匹配）
     * @param patientId 患者ID（可选）
     * @param status 收费状态（可选，0=未支付, 1=已支付, 2=已退费）
     * @param startDate 起始日期（可选，查询该日期及之后创建的收费单）
     * @param endDate 结束日期（可选，查询该日期及之前创建的收费单）
     * @param pageable 分页参数（包含页码、每页大小、排序）
     * @return 分页的收费单视图对象列表
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ChargeVO> queryCharges(String chargeNo, Long patientId, Integer status, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Specification<Charge> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), (short) 0));
            
            if (chargeNo != null && !chargeNo.isEmpty()) {
                predicates.add(cb.equal(root.get("chargeNo"), chargeNo));
            }
            if (patientId != null) {
                predicates.add(cb.equal(root.get("patient").get("mainId"), patientId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status.shortValue()));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(23, 59, 59)));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return chargeRepository.findAll(spec, pageable).map(this::mapToVO);
    }

    /**
     * 生成每日结算报表
     *
     * <p>统计指定日期的收费数据，生成收费员的日结算报表</p>
     *
     * <p><b>统计范围：</b></p>
     * <ul>
     *   <li>时间范围：指定日期的 00:00:00 至 23:59:59</li>
     *   <li>收费单状态：已支付（PAID）和已退费（REFUNDED）</li>
     *   <li>所有支付方式：现金、医保、银行卡、微信、支付宝等</li>
     * </ul>
     *
     * <p><b>报表内容：</b></p>
     * <ul>
     *   <li>总收费笔数和金额</li>
     *   <li>按支付方式分组统计（笔数、金额）</li>
     *   <li>退费统计（笔数、金额）</li>
     *   <li>净收金额（总收费 - 总退费）</li>
     * </ul>
     *
     * <p><b>应用场景：</b></p>
     * <ul>
     *   <li>收费员日终结账</li>
     *   <li>财务对账</li>
     *   <li>收费数据分析</li>
     * </ul>
     *
     * @param date 结算日期
     * @return 日结算报表视图对象，包含收费汇总、支付方式分布、退费统计等
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public DailySettlementVO getDailySettlement(LocalDate date) {
        log.info("生成每日结算报表，日期: {}", date);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);

        List<Charge> charges = chargeRepository.findByChargeTimeRange(start, end);
        
        long totalCharges = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalRefundAmount = BigDecimal.ZERO;
        long totalRefundCount = 0;
        
        Map<String, DailySettlementVO.PaymentBreakdownVO> breakdown = new HashMap<>();
        for (com.his.enums.PaymentMethodEnum method : com.his.enums.PaymentMethodEnum.values()) {
            DailySettlementVO.PaymentBreakdownVO b = new DailySettlementVO.PaymentBreakdownVO();
            b.setCount(0L);
            b.setAmount(BigDecimal.ZERO);
            breakdown.put(method.name(), b);
        }

        for (Charge c : charges) {
            if (ChargeStatusEnum.PAID.getCode().equals(c.getStatus()) || ChargeStatusEnum.REFUNDED.getCode().equals(c.getStatus())) {
                totalCharges++;
                totalAmount = totalAmount.add(c.getActualAmount());
                
                if (c.getPaymentMethod() != null) {
                    String methodName = com.his.enums.PaymentMethodEnum.fromCode(c.getPaymentMethod()).name();
                    DailySettlementVO.PaymentBreakdownVO b = breakdown.get(methodName);
                    b.setCount(b.getCount() + 1);
                    b.setAmount(b.getAmount().add(c.getActualAmount()));
                }
            }
            
            if (ChargeStatusEnum.REFUNDED.getCode().equals(c.getStatus())) {
                totalRefundCount++;
                totalRefundAmount = totalRefundAmount.add(c.getRefundAmount() != null ? c.getRefundAmount() : BigDecimal.ZERO);
            }
        }

        DailySettlementVO vo = new DailySettlementVO();
        vo.setDate(date);
        vo.setCashierName("当前收费员"); 
        vo.setTotalCharges(totalCharges);
        vo.setTotalAmount(totalAmount);
        vo.setPaymentBreakdown(breakdown);
        
        DailySettlementVO.RefundStatsVO rStats = new DailySettlementVO.RefundStatsVO();
        rStats.setCount(totalRefundCount);
        rStats.setAmount(totalRefundAmount);
        vo.setRefunds(rStats);

        vo.setNetCollection(totalAmount.subtract(totalRefundAmount));

        return vo;
    }

    /**
     * 生成收费单号（线程安全）
     *
     * <p>使用数据库序列生成唯一编号，避免并发冲突</p>
     * <p>格式：CHG + yyyyMMdd + 6位序列号（如：CHG20260103000001）</p>
     *
     * @return 唯一的收费单号
     * @throws IllegalStateException 如果数据库访问失败或序列生成失败
     */
    private String generateChargeNo() {
        Timer.Sample sample = sequenceMetrics.startTimer();
        try {
            String chargeNo = chargeRepository.generateChargeNo();
            log.debug("生成收费单号: {}", chargeNo);

            // 记录成功指标
            sequenceMetrics.recordSuccess("charge_no");
            return chargeNo;

        } catch (org.springframework.dao.DataAccessException e) {
            // 数据库访问异常（连接失败、SQL错误等）
            log.error("数据库访问失败，无法生成收费单号", e);
            sequenceMetrics.recordFailure("charge_no", "DataAccessException");
            throw new IllegalStateException("生成收费单号失败：数据库错误 - " + e.getMostSpecificCause().getMessage(), e);

        } catch (RuntimeException e) {
            // 其他运行时异常
            log.error("生成收费单号失败", e);
            sequenceMetrics.recordFailure("charge_no", e.getClass().getSimpleName());
            throw new IllegalStateException("生成收费单号失败：" + e.getMessage(), e);

        } finally {
            sequenceMetrics.stopTimer(sample);
        }
    }

    private ChargeVO mapToVO(Charge charge) {
        ChargeVO vo = new ChargeVO();
        vo.setId(charge.getMainId());
        vo.setChargeNo(charge.getChargeNo());
        vo.setPatientId(charge.getPatient().getMainId());
        vo.setPatientName(charge.getPatient().getName());
        vo.setTotalAmount(charge.getTotalAmount());
        vo.setStatus(charge.getStatus());
        vo.setStatusDesc(ChargeStatusEnum.fromCode(charge.getStatus()).getDescription());
        vo.setCreatedAt(charge.getCreatedAt());

        if (charge.getDetails() != null) {
            vo.setDetails(charge.getDetails().stream().map(d -> {
                ChargeVO.ChargeDetailVO dvo = new ChargeVO.ChargeDetailVO();
                dvo.setItemType(d.getItemType());
                dvo.setItemName(d.getItemName());
                dvo.setItemAmount(d.getItemAmount());
                return dvo;
            }).collect(Collectors.toList()));
        }

        return vo;
    }

    // ========== 分阶段收费方法实现 ==========

    /**
     * 创建挂号收费单（分阶段收费）
     *
     * <p>仅收取挂号费，不包含处方费</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>患者先缴纳挂号费，医生开处方后再缴纳处方费</li>
     *   <li>支持"先诊疗后付费"模式</li>
     * </ul>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>挂号单状态必须为 WAITING 或 PAID_REGISTRATION</li>
     *   <li>防止重复收费：如果挂号费已支付则抛出异常</li>
     *   <li>收费类型为 1（仅挂号费）</li>
     * </ul>
     *
     * @param registrationId 挂号单ID
     * @return 收费单视图对象
     * @throws IllegalArgumentException 如果挂号单不存在、状态不正确
     * @throws IllegalStateException 如果挂号费已支付
     * @see #createPrescriptionCharge(Long, List)
     * @see #isRegistrationFeePaid(Long)
     * @since 1.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChargeVO createRegistrationCharge(Long registrationId) {
        log.info("创建挂号收费单，挂号ID: {}", registrationId);
        CreateChargeDTO dto = new CreateChargeDTO();
        dto.setRegistrationId(registrationId);
        // 不传处方ID，仅收挂号费
        return createCharge(dto);
    }

    /**
     * 创建处方收费单（分阶段收费）
     *
     * <p>收取处方费，如果挂号费未支付则一并收取（混合收费）</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>患者已缴挂号费，现在缴纳处方费</li>
     *   <li>患者挂号费和处方费一并缴纳（混合收费）</li>
     * </ul>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>挂号单状态必须为 COMPLETED 或 PAID_REGISTRATION</li>
     *   <li>处方必须已审核通过（REVIEWED状态）</li>
     *   <li>防止重复收费：已缴费的处方不能再次收费</li>
     *   <li>智能判断：如果挂号费未支付，自动升级为混合收费（类型3）</li>
     *   <li>如果挂号费已支付，仅收取处方费（类型2）</li>
     * </ul>
     *
     * @param registrationId 挂号单ID
     * @param prescriptionIds 处方ID列表
     * @return 收费单视图对象
     * @throws IllegalArgumentException 如果挂号单不存在、状态不正确
     * @throws IllegalArgumentException 如果处方不存在、未审核
     * @throws IllegalStateException 如果处方费已支付
     * @see #createRegistrationCharge(Long)
     * @see #isPrescriptionFeePaid(Long)
     * @since 1.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChargeVO createPrescriptionCharge(Long registrationId, List<Long> prescriptionIds) {
        log.info("创建处方收费单，挂号ID: {}, 处方数: {}", registrationId, prescriptionIds.size());
        CreateChargeDTO dto = new CreateChargeDTO();
        dto.setRegistrationId(registrationId);
        dto.setPrescriptionIds(prescriptionIds);
        return createCharge(dto);
    }

    /**
     * 检查挂号费是否已支付
     *
     * <p>查询指定挂号单的挂号费是否已经缴纳</p>
     *
     * <p><b>检查逻辑：</b></p>
     * <ul>
     *   <li>查询是否存在状态为 PAID 或 REFUNDED 的挂号收费单</li>
     *   <li>使用优化的 EXISTS 查询，避免 N+1 查询问题</li>
     * </ul>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>分阶段收费时判断是否需要收取挂号费</li>
     *   <li>防止重复收费</li>
     * </ul>
     *
     * @param registrationId 挂号单ID
     * @return true表示挂号费已支付，false表示未支付
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isRegistrationFeePaid(Long registrationId) {
        return isRegistrationFeePaidInternal(registrationId);
    }

    /**
     * 按收费类型分组查询收费单
     *
     * <p>查询指定挂号单的所有收费单，并按收费类型分组返回</p>
     *
     * <p><b>分组类型：</b></p>
     * <ul>
     *   <li>"registration"：仅挂号费的收费单（chargeType = 1）</li>
     *   <li>"prescription"：仅处方费的收费单（chargeType = 2）</li>
     *   <li>"combined"：混合收费单（挂号费+处方费，chargeType = 3）</li>
     * </ul>
     *
     * <p><b>查询规则：</b></p>
     * <ul>
     *   <li>返回指定挂号单的所有未删除收费单</li>
     *   <li>按创建时间倒序排列（最新的在前）</li>
     *   <li>每种类型可能包含0个或多个收费单</li>
     * </ul>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>查看患者的收费历史</li>
     *   <li>对账和审计</li>
     *   <li>分阶段收费时查询已缴费用</li>
     * </ul>
     *
     * @param registrationId 挂号单ID
     * @return 分组的收费单列表，key为类型名称，value为该类型的收费单列表
     * @since 1.0
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, List<ChargeVO>> getChargesByType(Long registrationId) {
        List<Charge> charges = chargeRepository.findByRegistrationIdWithDetailsOrderByCreatedAtDesc(
                registrationId, (short) 0);

        Map<String, List<ChargeVO>> result = new HashMap<>();
        result.put("registration", charges.stream()
                .filter(c -> c.getChargeType() == 1)
                .map(this::mapToVO)
                .collect(Collectors.toList()));
        result.put("prescription", charges.stream()
                .filter(c -> c.getChargeType() == 2)
                .map(this::mapToVO)
                .collect(Collectors.toList()));
        result.put("combined", charges.stream()
                .filter(c -> c.getChargeType() == 3)
                .map(this::mapToVO)
                .collect(Collectors.toList()));

        return result;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 检查挂号费是否已支付（内部方法 - 优化版本）
     *
     * <p>使用优化的 EXISTS 查询，避免 N+1 查询问题</p>
     */
    private boolean isRegistrationFeePaidInternal(Long registrationId) {
        return chargeRepository.isRegistrationFeePaidOptimized(registrationId);
    }

    /**
     * 检查处方费是否已支付（内部方法 - 优化版本）
     *
     * <p>使用优化的 EXISTS 查询，避免 N+1 查询问题</p>
     */
    private boolean isPrescriptionFeePaid(Long prescriptionId) {
        return chargeRepository.isPrescriptionFeePaidOptimized(prescriptionId);
    }
}
