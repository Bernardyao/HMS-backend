package com.his.service.impl;

import com.his.dto.CreateChargeDTO;
import com.his.dto.PaymentDTO;
import com.his.entity.*;
import com.his.enums.ChargeStatusEnum;
import com.his.enums.PrescriptionStatusEnum;
import com.his.enums.RegStatusEnum;
import com.his.repository.*;
import com.his.service.ChargeService;
import com.his.service.PrescriptionService;
import com.his.vo.ChargeVO;
import com.his.vo.DailySettlementVO;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final PrescriptionService prescriptionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChargeVO createCharge(CreateChargeDTO dto) {
        log.info("开始创建收费单，挂号单ID: {}", dto.getRegistrationId());

        Registration registration = registrationRepository.findById(dto.getRegistrationId())
                .orElseThrow(() -> new IllegalArgumentException("挂号单不存在，ID: " + dto.getRegistrationId()));

        if (registration.getIsDeleted() == 1) {
            throw new IllegalArgumentException("挂号单已被删除");
        }

        if (!RegStatusEnum.COMPLETED.getCode().equals(registration.getStatus())) {
            throw new IllegalArgumentException("只有已就诊的挂号单才能进行收费");
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
            }
        }

        List<ChargeDetail> details = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (registration.getRegistrationFee() != null && registration.getRegistrationFee().compareTo(BigDecimal.ZERO) > 0) {
            ChargeDetail regDetail = new ChargeDetail();
            regDetail.setCharge(null); // Will set later
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
        charge.setChargeType((short) (prescriptions.isEmpty() ? 1 : 2));
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

    @Override
    @Transactional(readOnly = true)
    public ChargeVO getById(Long id) {
        Charge charge = chargeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("收费单不存在，ID: " + id));
        return mapToVO(charge);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChargeVO processPayment(Long id, PaymentDTO dto) {
        log.info("开始处理支付，收费单ID: {}, 支付金额: {}", id, dto.getPaidAmount());

        if (dto.getTransactionNo() != null && !dto.getTransactionNo().isEmpty()) {
            var existingCharge = chargeRepository.findByTransactionNo(dto.getTransactionNo());
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

        Charge charge = chargeRepository.findById(id)
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

        if (charge.getDetails() != null) {
            for (ChargeDetail detail : charge.getDetails()) {
                if ("PRESCRIPTION".equals(detail.getItemType())) {
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChargeVO processRefund(Long id, String refundReason) {
        log.info("开始处理退费，收费单ID: {}, 原因: {}", id, refundReason);

        Charge charge = chargeRepository.findById(id)
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
