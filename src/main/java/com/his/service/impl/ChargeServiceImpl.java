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
    public ChargeVO processPayment(Long id, PaymentDTO dto) {
        // TODO: 实现支付逻辑
        return null;
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
