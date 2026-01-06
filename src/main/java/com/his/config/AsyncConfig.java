package com.his.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步任务配置类
 *
 * <p>为审计日志保存等异步操作配置线程池</p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>启用异步</b>：使用@EnableAsync注解启用Spring异步支持</li>
 *   <li><b>线程池配置</b>：为审计日志保存配置专用线程池</li>
 *   <li><b>优雅停机</b>：应用关闭时等待任务完成</li>
 * </ul>
 *
 * <h3>线程池配置</h3>
 * <ul>
 *   <li><b>线程池名称</b>：auditLogExecutor</li>
 *   <li><b>核心线程数</b>：2（即使在空闲时刻也保持活跃的线程数）</li>
 *   <li><b>最大线程数</b>：5（线程池允许的最大线程数）</li>
 *   <li><b>队列容量</b>：100（等待执行的任务队列最大长度）</li>
 *   <li><b>线程名称前缀</b>：audit-log-（便于日志查看和问题排查）</li>
 *   <li><b>拒绝策略</b>：CallerRunsPolicy（队列满时由调用线程执行）</li>
 *   <li><b>优雅停机</b>：等待任务完成后再关闭（最长等待60秒）</li>
 * </ul>
 *
 * <h3>性能考虑</h3>
 * <ul>
 *   <li><b>核心线程数=2</b>：审计日志保存是IO密集型操作,不需要太多线程</li>
 *   <li><b>最大线程数=5</b>：应对突发流量,避免无限创建线程</li>
 *   <li><b>队列容量=100</b>：缓冲队列,避免直接拒绝任务</li>
 *   <li><b>CallerRunsPolicy</b>：队列满时降级为同步执行,保证任务不丢失</li>
 * </ul>
 *
 * <h3>监控建议</h3>
 * <ul>
 *   <li>监控线程池活跃线程数和队列长度</li>
 *   <li>如果队列经常满,考虑增加核心线程数或优化保存逻辑</li>
 *   <li>使用Spring Actuator查看线程池指标</li>
 * </ul>
 *
 * @author HIS 开发团队
 * @version 1.0
 * @since 1.0
 * @see org.springframework.scheduling.annotation.Async
 * @see org.springframework.scheduling.annotation.EnableAsync
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 审计日志异步保存线程池
     *
     * <p>用于异步保存审计日志到数据库,避免阻塞业务线程</p>
     *
     * <h3>线程池参数说明</h3>
     * <ul>
     *   <li><b>核心线程数</b>：2 - 即使空闲也保持2个线程活跃</li>
     *   <li><b>最大线程数</b>：5 - 最多创建5个线程处理任务</li>
     *   <li><b>队列容量</b>：100 - 最多缓存100个待执行任务</li>
     *   <li><b>线程名称前缀</b>：audit-log- - 便于日志识别</li>
     *   <li><b>拒绝策略</b>：CallerRunsPolicy - 队列满时由调用线程执行</li>
     *   <li><b>优雅停机</b>：true - 应用关闭时等待任务完成</li>
     *   <li><b>停机等待时间</b>：60秒 - 最长等待60秒</li>
     * </ul>
     *
     * <h3>拒绝策略说明</h3>
     * <p>CallerRunsPolicy：当队列满时,不抛出异常,而是由调用线程（通常是业务线程）执行任务</p>
     * <ul>
     *   <li><b>优点</b>：保证任务不丢失,同时提供背压机制</li>
     *   <li><b>缺点</b>：会阻塞业务线程,降低吞吐量</li>
     *   <li><b>适用场景</b>：审计日志保存失败不应该影响业务,但也不应该丢失</li>
     * </ul>
     *
     * @return 配置好的线程池执行器
     * @since 1.0
     */
    @Bean(name = "auditLogExecutor")
    public Executor auditLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：即使在空闲时刻也保持活跃的线程数
        executor.setCorePoolSize(2);

        // 最大线程数：线程池允许的最大线程数
        executor.setMaxPoolSize(5);

        // 队列容量：等待执行的任务队列最大长度
        executor.setQueueCapacity(100);

        // 线程名称前缀：便于日志查看和问题排查
        executor.setThreadNamePrefix("audit-log-");

        // 拒绝策略：队列满时由调用线程执行（保证任务不丢失）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 优雅停机：应用关闭时等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 停机等待时间：最长等待60秒
        executor.setAwaitTerminationSeconds(60);

        // 初始化线程池
        executor.initialize();

        return executor;
    }
}
