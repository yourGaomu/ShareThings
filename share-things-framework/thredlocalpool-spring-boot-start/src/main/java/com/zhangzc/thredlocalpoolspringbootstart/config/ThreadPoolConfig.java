package com.zhangzc.thredlocalpoolspringbootstart.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池配置类（基于ThreadPoolExecutor封装的ThreadPoolTaskExecutor）
 * 适配业务场景：IO密集型/CPU密集型可通过参数调整
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

    // 核心线程数（默认CPU核心数）
    @Value("${thread.pool.core-size:${runtime.availableProcessors}}")
    private int corePoolSize;

    // 最大线程数（默认CPU核心数*2）
    @Value("${thread.pool.max-size:${runtime.availableProcessors}*2}")
    private int maxPoolSize;

    // 队列容量（默认1000，IO密集型可增大）
    @Value("${thread.pool.queue-capacity:1000}")
    private int queueCapacity;

    // 空闲线程存活时间（秒）
    @Value("${thread.pool.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    // 线程名称前缀（便于日志排查）
    @Value("${thread.pool.name-prefix:biz-thread-}")
    private String threadNamePrefix;

    /**
     * 核心业务线程池
     * ThreadPoolTaskExecutor是Spring对ThreadPoolExecutor的封装，更易用
     */
    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor bizThreadPoolExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 1. 核心线程数：始终存活的线程数（CPU密集型=CPU核心数，IO密集型=CPU核心数*2/4）
        executor.setCorePoolSize(corePoolSize);
        // 2. 最大线程数：线程池允许的最大线程数（需大于等于核心线程数）
        executor.setMaxPoolSize(maxPoolSize);
        // 3. 队列容量：核心线程满后，任务进入阻塞队列等待（建议使用LinkedBlockingQueue）
        executor.setQueueCapacity(queueCapacity);
        // 4. 空闲线程存活时间：非核心线程空闲超过该时间会被销毁
        executor.setKeepAliveSeconds(keepAliveSeconds);
        // 5. 线程名称前缀：便于日志/监控识别线程归属
        executor.setThreadNamePrefix(threadNamePrefix);

        // 6. 拒绝策略：任务超出线程池承载能力时的处理方式（按需选择）
        // 可选策略：
        // - AbortPolicy（默认）：抛出RejectedExecutionException，快速失败
        // - CallerRunsPolicy：由调用线程（如主线程）执行任务，降低并发压力
        // - DiscardPolicy：直接丢弃任务，无提示
        // - DiscardOldestPolicy：丢弃队列最前面的任务，尝试重新提交当前任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 7. 优雅关闭：当应用关闭时，等待所有任务执行完成后再销毁线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 8. 关闭超时时间：等待任务完成的最大时间（超时则强制销毁）
        executor.setAwaitTerminationSeconds(60);

        // 初始化线程池
        executor.initialize();

        // 注册线程池监控（可选）
        //monitorThreadPool(executor);

        return executor;
    }

    /**
     * 线程池监控：打印核心参数，便于排查问题
     */
    public void monitorThreadPool(ThreadPoolTaskExecutor executor) {
        // 获取底层的ThreadPoolExecutor（Spring封装后需通过getThreadPoolExecutor()获取）
        ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();

        // 启动一个守护线程，定时打印线程池状态（生产环境可替换为监控系统采集）
        new Thread(() -> {
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(30); // 每30秒打印一次
                    log.info("===== 线程池监控 ======");
                    log.info("核心线程数：{}", threadPoolExecutor.getCorePoolSize());
                    log.info("活跃线程数：{}", threadPoolExecutor.getActiveCount());
                    log.info("最大线程数：{}", threadPoolExecutor.getMaximumPoolSize());
                    log.info("队列任务数：{}", threadPoolExecutor.getQueue().size());
                    log.info("已完成任务数：{}", threadPoolExecutor.getCompletedTaskCount());
                    log.info("总任务数：{}", threadPoolExecutor.getTaskCount());
                    log.info("======================");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("线程池监控线程被中断", e);
                    break;
                }
            }
        }, "thread-pool-monitor").setDaemon(true); // 守护线程：应用关闭时自动退出
    }
}