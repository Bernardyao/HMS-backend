package com.his.service;

import com.his.dto.RegistrationDTO;
import com.his.vo.RegistrationVO;

/**
 * 挂号服务接口
 */
public interface RegistrationService {

    /**
     * 挂号（老患查找 + 新患建档 + 创建挂号单）
     *
     * @param dto 挂号请求数据
     * @return 挂号结果
     */
    RegistrationVO register(RegistrationDTO dto);

    /**
     * 根据 ID 查询挂号记录
     *
     * @param id 挂号记录 ID
     * @return 挂号信息
     */
    RegistrationVO getById(Long id);

    /**
     * 取消挂号
     *
     * @param id 挂号记录 ID
     * @param reason 取消原因
     */
    void cancel(Long id, String reason);

    /**
     * 退费（将已取消的挂号标记为已退费）
     *
     * @param id 挂号记录 ID
     */
    void refund(Long id);
}
