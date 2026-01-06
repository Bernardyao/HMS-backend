package com.his.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.his.common.Result;
import com.his.converter.VoConverter;
import com.his.entity.Prescription;
import com.his.service.PrescriptionService;
import com.his.vo.PrescriptionVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 公共接口-处方查询控制器（统一版本）
 *
 * <p>为所有工作站提供统一的处方查询功能，替代原有的医生和药师处方查询接口</p>
 *
 * <h3>🔄 API重构说明</h3>
 * <p>本控制器整合了以下原有的查询端点：</p>
 * <ul>
 *   <li>~~<code>GET /api/doctor/prescriptions/{id}</code>~~ → <code>GET /api/common/prescriptions/{id}</code></li>
 *   <li>~~<code>GET /api/doctor/prescriptions/by-record/{recordId}</code>~~ → <code>GET /api/common/prescriptions/by-record/{recordId}</code></li>
 *   <li>~~<code>GET /api/pharmacist/prescriptions/{id}</code>~~ → <code>GET /api/common/prescriptions/{id}</code></li>
 * </ul>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>处方详情查询</b>：根据处方ID查询详细信息（所有角色）</li>
 *   <li><b>按病历查询</b>：查询指定病历的所有处方（所有角色）</li>
 * </ul>
 *
 * <h3>📋 保留的操作类API（未迁移）</h3>
 * <p>处方操作接口保留在各工作站控制器中，保持业务职责清晰：</p>
 * <ul>
 *   <li>医生操作：<code>POST /api/doctor/prescriptions</code>（创建处方）</li>
 *   <li>医生操作：<code>POST /api/doctor/prescriptions/{id}/review</code>（审核处方）</li>
 *   <li>药师操作：<code>GET /api/pharmacist/prescriptions/pending</code>（待发药列表）</li>
 *   <li>药师操作：<code>POST /api/pharmacist/prescriptions/{id}/dispense</code>（发药）</li>
 *   <li>药师操作：<code>POST /api/pharmacist/prescriptions/{id}/return</code>（退药）</li>
 * </ul>
 *
 * <h3>角色权限</h3>
 * <p>本控制器所有接口需要已认证用户（isAuthenticated()）</p>
 *
 * @author HIS 开发团队
 * @version 2.0
 * @since 2.0
 * @see com.his.service.PrescriptionService
 * @see com.his.controller.PrescriptionController
 * @see com.his.controller.PharmacistPrescriptionController
 */
@Tag(name = "公共接口-处方查询", description = "统一的处方查询接口（替代原有医生和药师查询接口）")
@Slf4j
@RestController
@RequestMapping("/api/common/prescriptions")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CommonPrescriptionController {

    private final PrescriptionService prescriptionService;

    /**
     * 根据ID查询处方详情
     * <p>
     * 查询处方的完整信息，包括处方明细、状态、审核信息、发药信息等。
     * </p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>医生：查看已开具的处方详情</li>
     *   <li>药师：查看待发药或已发药的处方详情</li>
     *   <li>收费员：查询处方金额进行收费</li>
     *   <li>患者：查询自己的处方记录（如果支持患者端）</li>
     * </ul>
     *
     * <p><b>响应示例：</b></p>
     * <pre>{@code
     * {
     *   "code": 200,
     *   "message": "查询成功",
     *   "data": {
     *     "mainId": 1,
     *     "prescriptionNo": "RX202501010001",
     *     "patientId": 10,
     *     "patientName": "张三",
     *     "doctorId": 5,
     *     "doctorName": "李医生",
     *     "totalAmount": 156.80,
     *     "itemCount": 3,
     *     "status": "PAID",
     *     "items": [...]
     *   }
     * }
     * }</pre>
     *
     * @param id 处方ID
     * @return 处方详细信息
     */
    @Operation(
        summary = "查询处方详情",
        description = """
            根据处方ID查询处方详细信息，包含处方明细、状态、审核信息、发药信息等。

            **🔄 API迁移说明：**
            - 原接口：~~`GET /api/doctor/prescriptions/{id}`~~ 和 ~~`GET /api/pharmacist/prescriptions/{id}`~~
            - 新接口：`GET /api/common/prescriptions/{id}` （统一查询接口，所有角色可用）

            **使用场景：**
            - 医生查看已开具的处方详情
            - 药师查看待发药或已发药的处方详情
            - 收费员查询处方金额进行收费
            - 患者查询自己的处方记录

            **处方状态说明：**
            - DRAFT: 草稿（未提交）
            - PENDING: 待审核
            - APPROVED: 已审核（待收费）
            - PAID: 已收费（待发药）
            - DISPENSED: 已发药
            - RETURNED: 已退药
            - CANCELLED: 已作废

            **请求示例：**
            ```bash
            # 查询处方详情（所有角色通用）
            GET /api/common/prescriptions/123
            ```
            """
    )
    @GetMapping("/{id}")
    public Result<PrescriptionVO> getById(
        @Parameter(description = "处方ID", required = true, example = "1")
        @PathVariable("id") Long id) {

        log.info("【通用】查询处方详情 - ID: {}", id);

        Prescription prescription = prescriptionService.getById(id);
        PrescriptionVO vo = VoConverter.toPrescriptionVO(prescription);

        return Result.success("查询成功", vo);
    }

    /**
     * 根据病历ID查询处方列表
     * <p>
     * 查询指定病历的所有处方，通常用于查看就诊历史。
     * </p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>医生：查看本次就诊开具的所有处方</li>
     *   <li>药师：查看患者的用药历史</li>
     *   <li>收费员：查看本次就诊的所有待收费处方</li>
     * </ul>
     *
     * <p><b>响应示例：</b></p>
     * <pre>{@code
     * {
     *   "code": 200,
     *   "message": "查询成功",
     *   "data": [
     *     {
     *       "mainId": 1,
     *       "prescriptionNo": "RX202501010001",
     *       "totalAmount": 156.80,
     *       "status": "PAID",
     *       "createdAt": "2025-01-01T10:30:00"
     *     },
     *     {
     *       "mainId": 2,
     *       "prescriptionNo": "RX202501010002",
     *       "totalAmount": 89.50,
     *       "status": "APPROVED",
     *       "createdAt": "2025-01-01T11:15:00"
     *     }
     *   ]
     * }
     * }</pre>
     *
     * @param recordId 病历ID
     * @return 该病历的所有处方列表
     */
    @Operation(
        summary = "查询病历的处方列表",
        description = """
            根据病历ID查询该病历的所有处方，按创建时间倒序排列。

            **🔄 API迁移说明：**
            - 原接口：~~`GET /api/doctor/prescriptions/by-record/{recordId}`~~
            - 新接口：`GET /api/common/prescriptions/by-record/{recordId}` （统一查询接口，所有角色可用）

            **使用场景：**
            - 医生查看本次就诊开具的所有处方
            - 药师查看患者的用药历史
            - 收费员查看本次就诊的所有待收费处方

            **注意事项：**
            - 返回的是该病历的所有处方（包括草稿、已审核、已收费、已发药等所有状态）
            - 按创建时间倒序排列，最新的处方在前

            **请求示例：**
            ```bash
            # 查询病历的所有处方（所有角色通用）
            GET /api/common/prescriptions/by-record/456
            ```
            """
    )
    @GetMapping("/by-record/{recordId}")
    public Result<List<PrescriptionVO>> getByRecordId(
        @Parameter(description = "病历ID", required = true, example = "1")
        @PathVariable("recordId") Long recordId) {

        log.info("【通用】查询病历的处方列表 - 病历ID: {}", recordId);

        List<Prescription> prescriptions = prescriptionService.getByRecordId(recordId);
        List<PrescriptionVO> voList = prescriptions.stream()
            .map(VoConverter::toPrescriptionVO)
            .collect(Collectors.toList());

        return Result.success(
            String.format("查询成功，共 %d 张处方", voList.size()),
            voList
        );
    }
}
