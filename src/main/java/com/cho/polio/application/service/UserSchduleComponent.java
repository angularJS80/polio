package com.cho.polio.application.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class UserSchduleComponent {
    private final UserService userService;
    private final UserReadService userReadService;

    private final RestTemplate restTemplate ;

    private final ExecutorService executorService; // 혹은 ThreadPoolTaskExecutor


    public UserSchduleComponent(
            UserService userService,
            UserReadService userReadService,
            RestTemplate restTemplate,
            @Qualifier("task") ExecutorService executorService) {

        this.userService = userService;
        this.userReadService = userReadService;
        this.restTemplate = restTemplate;
        this.executorService = executorService;
    }

    public void requestUpdateUserName(){
        userService.findAll().forEach(user -> {
            executorService.submit(() -> {
                userService.changeNextName(user);
            });
        });
    }

    public void testRegist() {

        int requestCount = 500;
        String url = "http://localhost:8080/user/regist?mode=double-service";

        ExecutorService executor = Executors.newFixedThreadPool(20); // 병렬 처리용 스레드 풀

        List<CompletableFuture<Void>> futures = IntStream.range(0, requestCount)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        String response = restTemplate.getForObject(url, String.class);
                    } catch (Exception e) {
                        System.err.printf("Error[%d]: %s%n", i, e.getMessage());
                    }
                }, executor))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

    }
}
