package com.cho.polio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorConfig {

    @Bean(name = "httpSend", destroyMethod = "shutdown")
    public ExecutorService httpSendTaskExecutor() {
        // 고정 쓰레드풀 생성 (스레드 수 10)
        return Executors.newFixedThreadPool(2);
    }

    @Bean(name = "httpReceive", destroyMethod = "shutdown")
    public ExecutorService httpReceiveTaskExecutor() {
        // 고정 쓰레드풀 생성 (스레드 수 10)
        return Executors.newFixedThreadPool(2);
    }

    @Bean(name = "transaction", destroyMethod = "shutdown")
    public ExecutorService transactionTaskExecutor() {
        // 고정 쓰레드풀 생성 (스레드 수 10)
        return Executors.newFixedThreadPool(2);
    }

    @Bean(name = "task", destroyMethod = "shutdown")
    public ExecutorService taskExecutor() {
        // 고정 쓰레드풀 생성 (스레드 수 10)
        return Executors.newFixedThreadPool(2);
    }
}