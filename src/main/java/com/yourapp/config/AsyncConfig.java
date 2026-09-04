package com.yourapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * AsyncConfig enables Spring's asynchronous processing.
 * 
 * WHY THIS IS CRITICAL FOR WHATSAPP:
 * Meta Cloud API requires an HTTP 200 OK within 3 seconds.
 * By marking our message processing service with @Async("webhookExecutor"),
 * the HTTP Controller immediately returns HTTP 200 OK to Meta in < 50ms,
 * while this background thread pool handles database queries and nutrition logic.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "webhookExecutor")
    public Executor webhookExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("wa-webhook-");
        executor.initialize();
        return executor;
    }
}
