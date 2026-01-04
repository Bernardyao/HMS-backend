package com.his.controller;

import com.his.common.Result;
import com.his.entity.AuditLogEntity;
import com.his.service.AuditLogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志查询控制器
 *
 * <p>提供审计日志的查询API,仅管理员可访问</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>综合查询</b>：支持多条件组合查询（模块、操作、操作人、类型、时间范围）</li>
 *   <li><b>TraceId查询</b>：根据TraceId查询整个请求链路的审计日志</li>
 *   <li><b>操作人查询</b>：查询某个用户的所有审计日志</li>
 *   <li><b>分页支持</b>：所有查询都支持分页和排序</li>
 * </ul>
 *
 * <h3>权限控制</h3>
 * <ul>
 *   <li><b>所有接口</b>：仅限管理员(ADMIN)访问</li>
 *   <li><b>权限验证</b>：使用@PreAuthorize注解实现方法级权限控制</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 1. 综合查询：查询最近7天的所有审计日志
 * GET /api/audit-logs/search?startTime=2025-01-01T00:00:00&endTime=2025-01-08T00:00:00&page=0&size=20
 *
 * // 2. 查询特定模块的敏感操作
 * GET /api/audit-logs/search?module=认证管理&auditType=SENSITIVE_OPERATION
 *
 * // 3. 根据TraceId查询
 * GET /api/audit-logs/trace/abc123def456...
 *
 * // 4. 查询某个用户的操作记录
 * GET /api/audit-logs/operator/123
 * </pre>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.AuditLogQueryService
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "审计日志管理", description = "审计日志查询接口（仅管理员）")
@PreAuthorize("hasRole('ADMIN')")  // 所有接口都需要管理员权限
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    /**
     * 综合查询审计日志（支持分页）
     *
     * <p>支持多条件组合查询,所有参数都是可选的</p>
     *
     * <p><b>查询参数：</b></p>
     * <ul>
     *   <li><b>module</b>：按模块查询（如"认证管理"、"挂号管理"）</li>
     *   <li><b>action</b>：按操作查询（如"用户登录"、"患者挂号"）</li>
     *   <li><b>operatorUsername</b>：按操作人用户名查询</li>
     *   <li><b>auditType</b>：按审计类型查询（SENSITIVE_OPERATION、BUSINESS、DATA_ACCESS）</li>
     *   <li><b>startTime</b>：查询开始时间（格式：yyyy-MM-ddTHH:mm:ss）</li>
     *   <li><b>endTime</b>：查询结束时间（格式：yyyy-MM-ddTHH:mm:ss）</li>
     *   <li><b>page</b>：页码（从0开始）</li>
     *   <li><b>size</b>：每页大小（默认20）</li>
     *   <li><b>sort</b>：排序字段（如：createTime,desc）</li>
     * </ul>
     *
     * <p><b>使用示例：</b></p>
     * <pre>
     * // 查询最近7天的所有审计日志
     * GET /api/audit-logs/search?startTime=2025-01-01T00:00:00&endTime=2025-01-08T00:00:00&page=0&size=20&sort=createTime,desc
     *
     * // 查询特定模块的敏感操作
     * GET /api/audit-logs/search?module=认证管理&auditType=SENSITIVE_OPERATION&page=0&size=20
     *
     * // 查询某个用户的操作记录
     * GET /api/audit-logs/search?operatorUsername=admin&page=0&size=20
     * </pre>
     *
     * @param module 模块名称(可选)
     * @param action 操作描述(可选)
     * @param operatorUsername 操作人用户名(可选)
     * @param auditType 审计类型(可选)
     * @param startTime 查询开始时间(可选)
     * @param endTime 查询结束时间(可选)
     * @param pageable 分页和排序参数
     * @return 分页查询结果
     * @since 1.0
     */
    @GetMapping("/search")
    @Operation(summary = "综合查询审计日志", description = "支持多条件组合查询（模块、操作、操作人、类型、时间范围），仅管理员可访问")
    public Result<Page<AuditLogEntity>> searchAuditLogs(
            @Parameter(description = "模块名称（如：认证管理、挂号管理）")
            @RequestParam(required = false) String module,

            @Parameter(description = "操作描述（如：用户登录、患者挂号）")
            @RequestParam(required = false) String action,

            @Parameter(description = "操作人用户名")
            @RequestParam(required = false) String operatorUsername,

            @Parameter(description = "审计类型（SENSITIVE_OPERATION、BUSINESS、DATA_ACCESS）")
            @RequestParam(required = false) String auditType,

            @Parameter(description = "查询开始时间（格式：yyyy-MM-ddTHH:mm:ss）")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,

            @Parameter(description = "查询结束时间（格式：yyyy-MM-ddTHH:mm:ss）")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,

            @Parameter(description = "分页参数（page: 页码从0开始，size: 每页大小，sort: 排序字段和方向）")
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AuditLogEntity> logs = auditLogQueryService.searchAuditLogs(
                module, action, operatorUsername, auditType, startTime, endTime, pageable);

        return Result.success(logs);
    }

    /**
     * 根据TraceId查询审计日志
     *
     * <p>用于查询整个请求链路的所有审计日志</p>
     * <p>TraceId由MDC自动生成,在同一个请求链路中保持一致</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>问题排查：查看某个请求的完整执行过程</li>
     *   <li>性能分析：分析某个请求的各阶段耗时</li>
     *   <li>审计追溯：追溯某个操作的所有相关日志</li>
     * </ul>
     *
     * <p><b>使用示例：</b></p>
     * <pre>
     * GET /api/audit-logs/trace/abc123def4567890abcdefghij1234567890
     * </pre>
     *
     * @param traceId 链路追踪ID(32位十六进制字符串)
     * @return 该TraceId对应的所有审计日志(按创建时间倒序)
     * @since 1.0
     */
    @GetMapping("/trace/{traceId}")
    @Operation(summary = "根据TraceId查询审计日志", description = "查询整个请求链路的所有审计日志，用于问题排查和性能分析")
    public Result<List<AuditLogEntity>> getAuditLogsByTraceId(
            @Parameter(description = "链路追踪ID（32位十六进制字符串）")
            @PathVariable String traceId) {

        List<AuditLogEntity> logs = auditLogQueryService.getAuditLogsByTraceId(traceId);
        return Result.success(logs);
    }

    /**
     * 查询操作人的审计日志
     *
     * <p>查询某个用户的所有审计日志,包括敏感操作和业务操作</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>用户行为审计：审计某个用户的所有操作</li>
     *   <li>安全调查：调查可疑用户的行为</li>
     *   <li>工作量统计：统计用户的操作次数</li>
     * </ul>
     *
     * <p><b>使用示例：</b></p>
     * <pre>
     * GET /api/audit-logs/operator/123
     * </pre>
     *
     * @param operatorId 操作人ID(sys_user.id)
     * @return 该操作人的所有审计日志(按创建时间倒序)
     * @since 1.0
     */
    @GetMapping("/operator/{operatorId}")
    @Operation(summary = "查询操作人的审计日志", description = "查询某个用户的所有审计日志，用于用户行为审计和安全调查")
    public Result<List<AuditLogEntity>> getAuditLogsByOperator(
            @Parameter(description = "操作人ID（sys_user.id）")
            @PathVariable Long operatorId) {

        List<AuditLogEntity> logs = auditLogQueryService.getAuditLogsByOperator(operatorId);
        return Result.success(logs);
    }
}
