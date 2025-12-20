package com.his.service;

import com.his.dto.MedicalRecordDTO;
import com.his.entity.MedicalRecord;

/**
 * 病历服务接口
 */
public interface MedicalRecordService {

    /**
     * 保存或更新病历
     * 如果该挂号单ID已存在病历，则更新；否则新建
     *
     * @param dto 病历数据
     * @return 病历实体
     */
    MedicalRecord saveOrUpdate(MedicalRecordDTO dto);

    /**
     * 根据ID查询病历
     *
     * @param id 病历ID
     * @return 病历信息
     */
    MedicalRecord getById(Long id);

    /**
     * 根据挂号单ID查询病历
     *
     * @param registrationId 挂号单ID
     * @return 病历信息（可能为null）
     */
    MedicalRecord getByRegistrationId(Long registrationId);

    /**
     * 提交病历（状态改为已提交）
     *
     * @param id 病历ID
     */
    void submit(Long id);
}
