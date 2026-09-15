package com.jzo2o.aigc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class AigcAsyncConfiguration {

    @Bean(name = "aigcExecutor")
    public ThreadPoolTaskExecutor aigcExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("aigc-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    @Bean(name = "aigcScheduler", destroyMethod = "shutdown")
    public ScheduledExecutorService aigcScheduler() {
        AtomicInteger sequence = new AtomicInteger();
        return Executors.newScheduledThreadPool(2, task -> {
            Thread thread = new Thread(task, "aigc-timeout-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }
}
