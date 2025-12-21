package com.his.service;

import com.his.dto.NurseWorkstationDTO;
import com.his.vo.NurseRegistrationVO;

import java.util.List;

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
}
