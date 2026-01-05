package com.his.controller;

import com.his.common.Result;
import com.his.dto.NurseWorkstationDTO;
import com.his.log.annotation.AuditLog;
import com.his.log.annotation.AuditType;
import com.his.service.NurseWorkstationService;
import com.his.service.PatientService;
import com.his.vo.NurseRegistrationVO;
import com.his.vo.PatientSearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 护士工作站控制器
 *
 * <p>为护士工作站提供今日挂号列表查询、患者信息搜索等功能</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>今日挂号查询</b>：查询今日所有挂号记录</li>
 *   <li><b>多条件筛选</b>：支持按科室、状态、就诊类型、关键字筛选</li>
 *   <li><b>患者信息搜索</b>：支持按姓名、身份证号、手机号搜索患者</li>
 *   <li><b>动态查询</b>：所有筛选条件都是可选的</li>
 * </ul>
 *
 * <h3>角色权限</h3>
 * <p>本控制器所有接口需要NURSE（护士）或ADMIN（管理员）角色</p>
 *
 * <h3>查询维度</h3>
 * <ul>
 *   <li>科室筛选：仅查看指定科室的挂号</li>
 *   <li>状态筛选：待就诊、就诊中、已完成、已取消</li>
 *   <li>就诊类型：初诊、复诊</li>
 *   <li>关键字搜索：患者姓名、手机号等</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.NurseWorkstationService
 * @see com.his.service.PatientService
 */
@Tag(name = "护士工作站", description = "护士工作站的挂号管理和患者查询接口")
@Slf4j
@RestController
@RequestMapping("/api/nurse")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('NURSE', 'ADMIN')")
public class NurseWorkstationController {

    private final NurseWorkstationService nurseWorkstationService;
    private final PatientService patientService;

    /**
     * 查询今日挂号列表
     */
    @Operation(
            summary = "查询今日挂号列表",
            description = "护士查看今日挂号列表，支持按科室、状态、就诊类型、关键字筛选。默认查询当天所有挂号记录"
    )
    @PostMapping("/registrations/today")
    public Result<List<NurseRegistrationVO>> getTodayRegistrations(
            @RequestBody(required = false) NurseWorkstationDTO dto
    ) {
        try {
            log.info("护士查询今日挂号列表，查询条件: {}", dto);
            List<NurseRegistrationVO> registrations = nurseWorkstationService.getTodayRegistrations(dto);
            String message = String.format("查询成功，共 %d 条挂号记录", registrations.size());
            return Result.success(message, registrations);
        } catch (IllegalArgumentException e) {
            log.warn("查询参数错误: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("查询今日挂号列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 搜索患者信息（用于自动补全）
     *
     * <p>根据关键字搜索患者档案，支持姓名、身份证号、手机号的模糊匹配</p>
     *
     * <h3>使用场景</h3>
     * <ul>
     *   <li>挂号时输入患者信息的自动补全</li>
     *   <li>快速查找已有患者档案</li>
     *   <li>避免重复建档</li>
     * </ul>
     *
     * <h3>搜索规则</h3>
     * <ul>
     *   <li>关键字至少 2 个字符</li>
     *   <li>支持姓名、身份证号、手机号的模糊匹配</li>
     *   <li>返回最多 15 条记录</li>
     *   <li>按最后更新时间降序排列</li>
     * </ul>
     *
     * @param keyword 搜索关键字（姓名、身份证号或手机号）
     * @return 匹配的患者列表（身份证号和手机号不脱敏）
     */
    @Operation(
            summary = "搜索患者信息",
            description = "根据关键字搜索患者档案，用于挂号时的自动补全功能。支持姓名、身份证号、手机号的模糊匹配"
    )
    @GetMapping("/patients/search")
    @AuditLog(
            module = "护士工作站",
            action = "搜索患者",
            description = "根据关键字搜索患者信息",
            auditType = AuditType.DATA_ACCESS
    )
    public Result<List<PatientSearchVO>> searchPatients(
            @Parameter(description = "搜索关键字（姓名、身份证号或手机号）", required = true, example = "张三")
            @RequestParam String keyword
    ) {
        try {
            log.info("护士搜索患者，关键字: [{}]", keyword);
            List<PatientSearchVO> patients = patientService.searchPatients(keyword);
            String message = String.format("查询成功，共 %d 条患者记录", patients.size());
            return Result.success(message, patients);
        } catch (IllegalArgumentException e) {
            log.warn("搜索参数错误: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("搜索患者失败", e);
            return Result.error("搜索失败: " + e.getMessage());
        }
    }
}
