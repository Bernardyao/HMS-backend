package com.his.service.impl;

import com.his.dto.CreateChargeDTO;
import com.his.dto.PaymentDTO;
import com.his.entity.*;
import com.his.enums.ChargeStatusEnum;
import com.his.enums.PrescriptionStatusEnum;
import com.his.enums.RegStatusEnum;
import com.his.repository.*;
import com.his.service.ChargeService;
import com.his.vo.ChargeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 收费服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChargeServiceImpl implements ChargeService {

    private final ChargeRepository chargeRepository;
    private final ChargeDetailRepository chargeDetailRepository;
    private final RegistrationRepository registrationRepository;
    private final PrescriptionRepository prescriptionRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChargeVO createCharge(CreateChargeDTO dto) {
        log.info("开始创建收费单，挂号单ID: {}", dto.getRegistrationId());

        // 1. 验证挂号单
        Registration registration = registrationRepository.findById(dto.getRegistrationId())
                .orElseThrow(() -> new IllegalArgumentException("挂号单不存在，ID: " + dto.getRegistrationId()));

        if (registration.getIsDeleted() == 1) {
            throw new IllegalArgumentException("挂号单已被删除");
        }

        if (!RegStatusEnum.COMPLETED.getCode().equals(registration.getStatus())) {
            throw new IllegalArgumentException("只有已就诊的挂号单才能进行收费");
        }

        // 2. 验证处方（如果有）
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
            }
        }

        // 3. 计算金额并创建明细
        List<ChargeDetail> details = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 挂号费
        if (registration.getRegistrationFee() != null && registration.getRegistrationFee().compareTo(BigDecimal.ZERO) > 0) {
            ChargeDetail regDetail = new ChargeDetail();
            regDetail.setItemType("REGISTRATION");
            regDetail.setItemId(registration.getMainId());
            regDetail.setItemName("挂号费");
            regDetail.setItemAmount(registration.getRegistrationFee());
            details.add(regDetail);
            totalAmount = totalAmount.add(registration.getRegistrationFee());
        }

        // 处方费
        for (Prescription p : prescriptions) {
            ChargeDetail pDetail = new ChargeDetail();
            pDetail.setItemType("PRESCRIPTION");
            pDetail.setItemId(p.getMainId());
            pDetail.setItemName("处方药费 (" + p.getPrescriptionNo() + ")");
            pDetail.setItemAmount(p.getTotalAmount());
            details.add(pDetail);
            totalAmount = totalAmount.add(p.getTotalAmount());
        }

        // 4. 保存主表
        Charge charge = new Charge();
        charge.setPatient(registration.getPatient());
        charge.setRegistration(registration);
        charge.setChargeNo(generateChargeNo());
        charge.setChargeType((short) (prescriptions.isEmpty() ? 1 : 2)); // 1=挂号费, 2=药费... 简单处理
        charge.setTotalAmount(totalAmount);
        charge.setActualAmount(totalAmount);
        charge.setStatus(ChargeStatusEnum.UNPAID.getCode());
        charge.setIsDeleted((short) 0);
        
        Charge savedCharge = chargeRepository.save(charge);

        // 5. 保存明细
        for (ChargeDetail d : details) {
            d.setCharge(savedCharge);
        }
        chargeDetailRepository.saveAll(details);
        savedCharge.setDetails(details);

        log.info("收费单创建成功，ID: {}, 单号: {}, 总金额: {}", savedCharge.getMainId(), savedCharge.getChargeNo(), totalAmount);

        return mapToVO(savedCharge);
    }

    @Override
    public ChargeVO getById(Long id) {
        Charge charge = chargeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("收费单不存在，ID: " + id));
        return mapToVO(charge);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChargeVO processPayment(Long id, PaymentDTO dto) {
        log.info("开始处理支付，收费单ID: {}, 支付金额: {}", id, dto.getPaidAmount());

        // 1. 幂等性校验：如果交易流水号已存在
        if (dto.getTransactionNo() != null && !dto.getTransactionNo().isEmpty()) {
            var existingCharge = chargeRepository.findByTransactionNo(dto.getTransactionNo());
            if (existingCharge.isPresent()) {
                Charge charge = existingCharge.get();
                // 如果已支付且金额一致，直接返回成功（幂等）
                if (ChargeStatusEnum.PAID.getCode().equals(charge.getStatus())) {
                    log.info("检测到重复支付请求（幂等），交易流水号: {}", dto.getTransactionNo());
                    return mapToVO(charge);
                } else {
                    throw new IllegalArgumentException("交易流水号已存在但状态不一致，流水号: " + dto.getTransactionNo());
                }
            }
        }

        // 2. 查询收费单
        Charge charge = chargeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("收费单不存在，ID: " + id));

        // 3. 状态校验
        if (!ChargeStatusEnum.UNPAID.getCode().equals(charge.getStatus())) {
            throw new IllegalArgumentException("收费单状态不正确，当前状态: " + charge.getStatus());
        }

        // 4. 金额校验 (误差允许 0.01)
        if (charge.getActualAmount().subtract(dto.getPaidAmount()).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new IllegalArgumentException("支付金额不匹配，应付: " + charge.getActualAmount() + ", 实付: " + dto.getPaidAmount());
        }

        // 5. 模拟调用第三方支付 (Mock)
        // 在这里可以添加日志模拟调用过程
        log.info("正在调用第三方支付接口验证... 支付方式: {}, 流水号: {}", dto.getPaymentMethod(), dto.getTransactionNo());
        // Mock: 默认成功

        // 6. 更新收费单状态
        charge.setStatus(ChargeStatusEnum.PAID.getCode());
        charge.setPaymentMethod(dto.getPaymentMethod());
        charge.setTransactionNo(dto.getTransactionNo());
        charge.setChargeTime(LocalDateTime.now());
        charge.setUpdatedBy(null); // TODO: 获取当前登录用户ID
        
        Charge savedCharge = chargeRepository.save(charge);

        // 7. 更新关联处方状态为已缴费
        if (charge.getDetails() != null) {
            for (ChargeDetail detail : charge.getDetails()) {
                if ("PRESCRIPTION".equals(detail.getItemType())) {
                    Prescription prescription = prescriptionRepository.findById(detail.getItemId())
                            .orElseThrow(() -> new IllegalArgumentException("处方不存在，ID: " + detail.getItemId()));
                    
                    // 只有 REVIEWED 状态才能更新为 PAID
                    // 或者这里可以更宽容一点，防止并发问题？暂时严格处理
                    if (PrescriptionStatusEnum.REVIEWED.getCode().equals(prescription.getStatus())) {
                         prescription.setStatus(PrescriptionStatusEnum.PAID.getCode());
                         prescription.setUpdatedAt(LocalDateTime.now());
                         prescriptionRepository.save(prescription);
                         log.info("处方状态已更新为已缴费，处方ID: {}", prescription.getMainId());
                    }
                }
            }
        }

        log.info("支付成功，收费单ID: {}", id);
        return mapToVO(savedCharge);
    }

    @Override
    public ChargeVO processRefund(Long id, String refundReason) {
        // TODO: 实现退费逻辑
        return null;
    }

    @Override
    public Page<ChargeVO> queryCharges(String chargeNo, Long patientId, Integer status, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        // TODO: 实现查询逻辑
        return null;
    }

    private String generateChargeNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 900) + 100;
        return "CHG" + timestamp + random;
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
}
