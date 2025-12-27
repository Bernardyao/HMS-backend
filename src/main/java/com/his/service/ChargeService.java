package com.his.service;

import com.his.dto.CreateChargeDTO;
import com.his.dto.PaymentDTO;
import com.his.vo.ChargeVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * 收费服务接口
 */
public interface ChargeService {

    /**
     * 创建收费单
     * 1. 验证挂号单状态（需 COMPLETED）
     * 2. 验证处方状态（需 REVIEWED）
     * 3. 计算总金额（挂号费 + 处方费）
     * 4. 保存收费单及明细
     *
     * @param dto 创建请求
     * @return 收费单信息
     */
    ChargeVO createCharge(CreateChargeDTO dto);

    /**
     * 根据ID获取收费单信息
     *
     * @param id 收费单ID
     * @return 收费单信息
     */
    ChargeVO getById(Long id);

    /**
     * 支付收费单
     * 1. 验证收费单状态
     * 2. 验证支付金额
     * 3. 幂等性校验（通过 transactionNo）
     * 4. 更新收费单状态为 PAID
     * 5. 更新处方状态为 PAID
     *
     * @param id 收费单ID
     * @param dto 支付信息
     * @return 更新后的收费单信息
     */
    ChargeVO processPayment(Long id, PaymentDTO dto);

    /**
     * 退费处理
     * 1. 验证收费单状态
     * 2. 更新收费单状态为 REFUNDED
     * 3. 根据处方状态决定是否恢复库存
     * 4. 更新处方状态
     *
     * @param id 收费单ID
     * @param refundReason 退费原因
     * @return 收费单信息
     */
    ChargeVO processRefund(Long id, String refundReason);

    /**
     * 查询收费单列表（支持分页和条件查询）
     */
    Page<ChargeVO> queryCharges(String chargeNo, Long patientId, Integer status, LocalDate startDate, LocalDate endDate, Pageable pageable);
}
