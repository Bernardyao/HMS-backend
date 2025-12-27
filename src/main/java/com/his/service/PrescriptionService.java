package com.his.service;

import com.his.dto.PrescriptionDTO;
import com.his.entity.Prescription;

/**
 * 处方服务接口
 */
public interface PrescriptionService {

    /**
     * 创建处方
     * 1. 接收挂号单ID和药品列表
     * 2. 遍历药品列表，从数据库查出当前单价
     * 3. 计算总金额
     * 4. 组装并保存处方主表和详情表（事务确保原子性）
     * 注意：暂时不扣减库存，库存扣减在发药阶段进行
     *
     * @param dto 处方数据
     * @return 处方实体
     */
    Prescription createPrescription(PrescriptionDTO dto);

    /**
     * 根据ID查询处方
     *
     * @param id 处方ID
     * @return 处方信息
     */
    Prescription getById(Long id);

    /**
     * 根据病历ID查询处方列表
     *
     * @param recordId 病历ID
     * @return 处方列表
     */
    java.util.List<Prescription> getByRecordId(Long recordId);

    /**
     * 审核处方
     *
     * @param id 处方ID
     * @param reviewDoctorId 审核医生ID
     * @param remark 审核备注
     */
    void review(Long id, Long reviewDoctorId, String remark);

    /**
     * 获取待发药处方列表
     * 已审核(status=2)且未发药的处方
     * 
     * @return 处方列表
     */
    java.util.List<Prescription> getPendingDispenseList();

    /**
     * 发药
     * 1. 检查状态是否为已审核
     * 2. 更新状态为已发药(status=3)
     * 3. 记录发药人和发药时间
     * 4. 扣减药品库存
     * 
     * @param id 处方ID
     * @param dispenseBy 发药人ID
     */
    void dispense(Long id, Long dispenseBy);

    /**
     * 退药
     * 1. 检查状态是否为已发药(status=3)
     * 2. 更新状态为已退药(status=4)
     * 3. 记录退药原因和时间
     * 4. 恢复药品库存
     *
     * @param id 处方ID
     * @param reason 退药原因
     */
    void returnMedicine(Long id, String reason);

    /**
     * 仅恢复库存（用于退费时调用）
     *
     * @param id 处方ID
     */
    void restoreInventoryOnly(Long id);

    /**
     * 获取药师今日工作统计
     * 
     * @param pharmacistId 药师ID
     * @return 统计数据
     */
    com.his.dto.PharmacistStatisticsDTO getPharmacistStatistics(Long pharmacistId);
}
