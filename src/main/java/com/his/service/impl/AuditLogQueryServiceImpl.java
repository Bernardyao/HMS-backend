package com.his.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.his.entity.AuditLogEntity;
import com.his.repository.AuditLogRepository;
import com.his.service.AuditLogQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 审计日志查询服务实现类
 *
 * <p>提供审计日志的综合查询功能,支持多条件组合查询和分页</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>动态查询</b>：使用JPA Specification实现动态条件拼接</li>
 *   <li><b>分页支持</b>：所有查询都支持分页和排序</li>
 *   <li><b>性能优化</b>：利用数据库索引,避免全表扫描</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see com.his.service.AuditLogQueryService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogQueryServiceImpl implements AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;

    /**
     * 综合查询审计日志（支持分页）
     *
     * <p>使用JPA Specification动态构建查询条件</p>
     *
     * <p><b>实现说明：</b></p>
     * <ul>
     *   <li>所有条件都是可选的,只拼接非空条件</li>
     *   <li>使用Criteria API动态构建WHERE子句</li>
     *   <li>充分利用数据库索引(idx_audit_module, idx_audit_operator等)</li>
     * </ul>
     *
     * @param module 模块名称(可选)
     * @param action 操作描述(可选)
     * @param operatorUsername 操作人用户名(可选)
     * @param auditType 审计类型(可选)
     * @param startTime 查询开始时间(可选)
     * @param endTime 查询结束时间(可选)
     * @param pageable 分页和排序参数
     * @return 分页查询结果
     */
    @Override
    public Page<AuditLogEntity> searchAuditLogs(
            String module, String action, String operatorUsername, String auditType,
            LocalDateTime startTime, LocalDateTime endTime,
            Pageable pageable) {

        log.debug("综合查询审计日志: module={}, action={}, operator={}, type={}, startTime={}, endTime={}",
                module, action, operatorUsername, auditType, startTime, endTime);

        // 构建动态查询条件
        Specification<AuditLogEntity> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 模块名称条件
            if (module != null && !module.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("module"), module));
            }

            // 操作描述条件
            if (action != null && !action.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("action"), action));
            }

            // 操作人用户名条件
            if (operatorUsername != null && !operatorUsername.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("operatorUsername"), operatorUsername));
            }

            // 审计类型条件
            if (auditType != null && !auditType.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("auditType"), auditType));
            }

            // 时间范围条件
            if (startTime != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createTime"), startTime));
            }
            if (endTime != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createTime"), endTime));
            }

            // 组合所有条件（AND关系）
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // 执行分页查询
        Page<AuditLogEntity> result = auditLogRepository.findAll(spec, pageable);

        log.debug("查询到 {} 条审计日志", result.getTotalElements());
        return result;
    }

    /**
     * 根据TraceId查询审计日志
     *
     * <p>直接调用Repository方法,利用idx_audit_trace_id索引</p>
     *
     * @param traceId 链路追踪ID
     * @return 该TraceId对应的所有审计日志(按创建时间倒序)
     */
    @Override
    public List<AuditLogEntity> getAuditLogsByTraceId(String traceId) {
        log.debug("根据TraceId查询审计日志: traceId={}", traceId);

        List<AuditLogEntity> logs = auditLogRepository.findByTraceIdOrderByCreateTimeDesc(traceId);

        log.debug("查询到 {} 条审计日志", logs.size());
        return logs;
    }

    /**
     * 查询操作人的审计日志
     *
     * <p>直接调用Repository方法,利用idx_audit_operator索引</p>
     *
     * @param operatorId 操作人ID
     * @return 该操作人的所有审计日志(按创建时间倒序)
     */
    @Override
    public List<AuditLogEntity> getAuditLogsByOperator(Long operatorId) {
        log.debug("查询操作人的审计日志: operatorId={}", operatorId);

        List<AuditLogEntity> logs = auditLogRepository.findByOperatorIdOrderByCreateTimeDesc(operatorId);

        log.debug("查询到 {} 条审计日志", logs.size());
        return logs;
    }
}
