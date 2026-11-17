package com.c2c.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置：为邮件等慢 IO 异步任务提供独立线程池，
 * 避免共用公共 ForkJoinPool 被慢邮件阻塞；队列满时由调用线程兜底执行（CallerRunsPolicy），不丢任务。
 *
 * @author wzh
 */
@Configuration
public class AsyncExecutorConfig {

    /** 邮件发送专用线程池 */
    @Bean("mailExecutor")
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mail-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
