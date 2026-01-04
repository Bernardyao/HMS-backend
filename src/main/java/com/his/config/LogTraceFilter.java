package com.his.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 日志追踪过滤器
 *
 * <p>为每个HTTP请求生成唯一的traceId（追踪ID），并放入MDC（Mapped Diagnostic Context）中，
 * 便于在日志中进行分布式请求链路追踪</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>生成追踪ID</b>：为每个请求生成唯一的traceId（UUID，32位十六进制字符串）</li>
 *   <li><b>链路透传</b>：支持从上游服务获取traceId（微服务场景）</li>
 *   <li><b>MDC注入</b>：将traceId放入SLF4J的MDC中，所有日志自动包含traceId</li>
 *   <li><b>响应头返回</b>：将traceId放入响应头，方便前端和调用方排查问题</li>
 *   <li><b>资源清理</b>：请求结束后自动清理MDC，防止内存泄漏和线程污染</li>
 * </ul>
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li>请求到达：使用@Order(HIGHEST_PRECEDENCE)确保最先执行</li>
 *   <li>检查请求头：如果包含traceId（上游服务传入），直接使用</li>
 *   <li>生成新ID：如果请求头没有traceId，生成新的UUID（去除连字符）</li>
 *   <li>放入MDC：调用MDC.put("traceId", traceId)</li>
 *   <li>放入响应头：response.setHeader("traceId", traceId)</li>
 *   <li>继续处理：调用filterChain.doFilter传递请求</li>
 *   <li>清理MDC：在finally块中调用MDC.remove("traceId")</li>
 * </ol>
 *
 * <h3>日志配置示例</h3>
 * <p>在logback-spring.xml中配置日志格式包含traceId：</p>
 * <pre>
 * &lt;pattern&gt;%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [traceId:%X{traceId}] %logger{36} - %msg%n&lt;/pattern&gt;
 * </pre>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li><b>单体应用</b>：追踪同一个请求在不同组件、方法之间的调用链</li>
 *   <li><b>微服务架构</b>：通过HTTP请求头透传traceId，追踪跨服务的调用链</li>
 *   <li><b>问题排查</b>：根据traceId快速定位一个完整请求的所有日志</li>
 *   <li><b>性能分析</b>：计算traceId的首尾日志时间差，得到请求总耗时</li>
 * </ul>
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li><b>最高优先级</b>：@Order(Ordered.HIGHEST_PRECEDENCE)确保最先执行</li>
 *   <li><b>继承OncePerRequestFilter</b>：确保每个请求只执行一次（避免转发场景重复执行）</li>
 *   <li><b>资源清理</b>：使用finally块确保MDC一定被清理，避免线程池复用时的数据污染</li>
 *   <li><b>UUID格式</b>：去除连字符（-），生成32位纯十六进制字符串，紧凑且易于解析</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see org.slf4j.MDC
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 保证最先执行
public class LogTraceFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";

    /**
     * 执行日志追踪过滤
     *
     * <p>为每个请求生成或获取traceId，放入MDC和响应头，请求结束后清理MDC</p>
     *
     * <p><b>处理流程：</b></p>
     * <ol>
     *   <li><b>获取traceId</b>：
     *     <ul>
     *       <li>优先从请求头获取（支持上游服务透传）</li>
     *       <li>如果请求头为空，生成新的UUID（32位十六进制字符串）</li>
     *     </ul>
     *   </li>
     *   <li><b>放入MDC</b>：调用MDC.put("traceId", traceId)，后续所有日志自动包含此ID</li>
     *   <li><b>放入响应头</b>：调用response.setHeader("traceId", traceId)，方便前端和调用方获取</li>
     *   <li><b>继续过滤链</b>：调用filterChain.doFilter(request, response)传递请求</li>
     *   <li><b>清理MDC</b>：在finally块中调用MDC.remove("traceId")，防止线程污染</li>
     * </ol>
     *
     * <p><b>MDC清理的重要性：</b></p>
     * <ul>
     *   <li>Web容器使用线程池处理请求，线程会被复用</li>
     *   <li>如果不清理MDC，下一个请求可能看到上一个请求的traceId</li>
     *   <li>使用finally块确保无论请求成功或失败，MDC都会被清理</li>
     * </ul>
     *
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet异常
     * @throws IOException IO异常
     * @since 1.0
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 1. 尝试从请求头获取 traceId (用于微服务透传)，如果没有则生成
            String traceId = request.getHeader(TRACE_ID);
            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString().replace("-", "");
            }

            // 2. 放入 MDC
            MDC.put(TRACE_ID, traceId);
            
            // 3. 同时放入响应头，方便前端/调用方排查
            response.setHeader(TRACE_ID, traceId);

            filterChain.doFilter(request, response);
        } finally {
            // 4. 清理 MDC，防止内存泄漏和线程污染
            MDC.remove(TRACE_ID);
        }
    }
}
