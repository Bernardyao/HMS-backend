package com.his.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.his.common.Result;
import com.his.converter.VoConverter;
import com.his.dto.MedicalRecordDTO;
import com.his.entity.MedicalRecord;
import com.his.service.MedicalRecordService;
import com.his.vo.MedicalRecordVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 病历管理控制器（医生工作站）
 *
 * <p>负责电子病历的创建、更新、查询、提交等全生命周期管理</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>保存或更新病历</b>：根据挂号单ID创建新病历或更新已有病历</li>
 *   <li><b>查询病历</b>：根据病历ID或挂号单ID查询病历详情</li>
 *   <li><b>提交病历</b>：将草稿状态的病历提交为正式病历</li>
 * </ul>
 *
 * <h3>角色权限</h3>
 * <p>本控制器所有接口需要DOCTOR（医生）或ADMIN（管理员）角色</p>
 *
 * <h3>业务规则</h3>
 * <ul>
 *   <li>病历必须关联有效的挂号单</li>
 *   <li>已提交的病历不能修改</li>
 *   <li>每个挂号单只能有一条有效病历</li>
 *   <li>病历编号自动生成</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.MedicalRecordService
 */
@Tag(name = "医生工作站-病历管理", description = "医生工作站的电子病历相关接口")
@Slf4j
@RestController
@RequestMapping("/api/doctor/medical-records")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    /**
     * 保存或更新病历
     *
     * @param dto 病历数据
     * @return 病历信息
     */
    @Operation(summary = "保存或更新病历", description = "根据挂号单ID保存或更新病历，如果已存在则更新")
    @PostMapping("/save")
    public Result<MedicalRecordVO> saveOrUpdate(@RequestBody MedicalRecordDTO dto) {
        try {
            log.info("收到保存/更新病历请求，挂号单ID: {}", dto.getRegistrationId());
            MedicalRecord record = medicalRecordService.saveOrUpdate(dto);
            MedicalRecordVO vo = VoConverter.toMedicalRecordVO(record);
            return Result.success("保存成功", vo);
        } catch (IllegalArgumentException e) {
            log.warn("保存病历失败: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("保存病历失败", e);
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID查询病历
     *
     * @param id 病历ID
     * @return 病历信息
     */
    @Operation(summary = "查询病历详情", description = "根据病历ID查询详细信息")
    @GetMapping("/{id}")
    public Result<MedicalRecordVO> getById(
            @Parameter(description = "病历ID", required = true, example = "1")
            @PathVariable("id") Long id) {
        try {
            log.info("收到查询病历请求，ID: {}", id);
            MedicalRecord record = medicalRecordService.getById(id);
            MedicalRecordVO vo = VoConverter.toMedicalRecordVO(record);
            return Result.success("查询成功", vo);
        } catch (IllegalArgumentException e) {
            log.warn("查询病历失败: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("查询病历失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据挂号单ID查询病历
     *
     * @param registrationId 挂号单ID
     * @return 病历信息
     */
    @Operation(summary = "根据挂号单查询病历", description = "根据挂号单ID查询对应的病历")
    @GetMapping("/by-registration/{registrationId}")
    public Result<MedicalRecordVO> getByRegistrationId(
            @Parameter(description = "挂号单ID", required = true, example = "1")
            @PathVariable("registrationId") Long registrationId) {
        try {
            log.info("收到根据挂号单查询病历请求，挂号单ID: {}", registrationId);
            MedicalRecord record = medicalRecordService.getByRegistrationId(registrationId);
            if (record == null) {
                return Result.success("该挂号单尚未创建病历", null);
            }
            MedicalRecordVO vo = VoConverter.toMedicalRecordVO(record);
            return Result.success("查询成功", vo);
        } catch (IllegalArgumentException e) {
            log.warn("查询病历失败: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("查询病历失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 提交病历
     *
     * @param id 病历ID
     * @return 操作结果
     */
    @Operation(summary = "提交病历", description = "将草稿状态的病历提交")
    @PostMapping("/{id}/submit")
    public Result<Void> submit(
            @Parameter(description = "病历ID", required = true, example = "1")
            @PathVariable Long id) {
        try {
            log.info("收到提交病历请求，ID: {}", id);
            medicalRecordService.submit(id);
            return Result.success("提交成功", null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("提交病历失败: {}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("提交病历失败", e);
            return Result.error("提交失败: " + e.getMessage());
        }
    }
}
