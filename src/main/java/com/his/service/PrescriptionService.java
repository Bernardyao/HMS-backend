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
}
