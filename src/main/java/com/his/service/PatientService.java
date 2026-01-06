package com.his.service;

import java.util.List;

import com.his.vo.PatientSearchVO;

/**
 * 患者服务接口
 *
 * <p>提供患者信息的查询和管理功能</p>
 *
 * @author HIS 开发团队
 * @since 1.0.0
 */
public interface PatientService {

    /**
     * 搜索患者信息（护士工作站专用）
     *
     * <p>根据关键字搜索患者，支持姓名、身份证号、手机号的模糊匹配</p>
     *
     * <h3>搜索策略</h3>
     * <ul>
     *   <li>支持姓名、身份证号、手机号的 OR 模糊匹配</li>
     *   <li>仅查询未删除的患者记录</li>
     *   <li>按最后更新时间降序排列</li>
     *   <li>限制返回前 15 条记录</li>
     * </ul>
     *
     * <h3>输入校验</h3>
     * <ul>
     *   <li>关键字不能为空</li>
     *   <li>关键字长度限制：2-20 个字符</li>
     *   <li>自动过滤特殊字符（%、_等）</li>
     * </ul>
     *
     * @param keyword 搜索关键字
     * @return 匹配的患者列表（不脱敏）
     * @throws IllegalArgumentException 当关键字为空或长度不符合要求时
     */
    List<PatientSearchVO> searchPatients(String keyword);
}
