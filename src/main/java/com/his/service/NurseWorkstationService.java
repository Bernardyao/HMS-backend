package com.his.service;

import java.util.List;

import com.his.dto.NurseWorkstationDTO;
import com.his.dto.PaymentDTO;
import com.his.vo.ChargeVO;
import com.his.vo.NurseRegistrationVO;

/**
 * 护士工作站服务接口
 */
public interface NurseWorkstationService {

    /**
     * 查询今日挂号列表
     *
     * @param dto 查询条件
     * @return 挂号列表
     */
    List<NurseRegistrationVO> getTodayRegistrations(NurseWorkstationDTO dto);

    /**
     * 护士站收取挂号费
     *
     * <p>为护士站提供一键收取挂号费的功能，无需患者到收费窗口</p>
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>仅可为状态为 WAITING 的挂号缴费</li>
     *   <li>自动检查是否已支付，防止重复收费</li>
     *   <li>复用已存在的未支付收费单，避免创建重复记录</li>
     *   <li>自动填充支付金额，确保与收费单金额匹配</li>
     *   <li>使用状态机更新挂号状态，确保审计日志</li>
     * </ul>
     *
     * @param registrationId 挂号单ID
     * @param paymentDTO 支付信息（包含支付方式，可选流水号）
     * @return 支付后的收费单信息
     * @throws IllegalArgumentException 如果挂号单不存在或状态不正确
     * @throws IllegalStateException 如果挂号费已支付
     */
    ChargeVO payRegistrationFee(Long registrationId, PaymentDTO paymentDTO);
}
