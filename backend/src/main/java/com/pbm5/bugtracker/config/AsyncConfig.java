package com.pbm5.bugtracker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous processing.
 * 
 * Enables @Async annotations and configures thread pool for:
 * - Notification processing
 * - Background tasks
 * - Non-blocking operations
 * 
 * This ensures that bug operations are never blocked by notification
 * processing,
 * providing better system resilience and performance.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Configure the executor for async notification processing.
     * 
     * Thread pool settings:
     * - Core pool size: 5 threads (handles normal load)
     * - Max pool size: 10 threads (handles peak load)
     * - Queue capacity: 100 (buffers during spikes)
     * - Keep alive: 60 seconds (cleanup unused threads)
     */
    @Bean(name = "notificationTaskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("NotificationAsync-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info(
                "AsyncConfig - Notification task executor initialized: corePoolSize=5, maxPoolSize=10, queueCapacity=100");
        return executor;
    }

    /**
     * Handle uncaught exceptions in async methods.
     * 
     * This prevents async notification failures from affecting the main
     * application.
     * Exceptions are logged but don't propagate to the calling thread.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, objects) -> {
            log.error("AsyncConfig - Uncaught exception in async method {}: {}",
                    method.getName(), throwable.getMessage(), throwable);

            // Log additional context about the failed operation
            if (objects != null && objects.length > 0) {
                log.error("AsyncConfig - Method parameters: {}", java.util.Arrays.toString(objects));
            }
        };
    }
}
