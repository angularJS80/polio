package com.cho.polio;

import com.cho.polio.application.service.UserSchduleComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SpringBootTest

class PolioApplicationTests {

    @Autowired
    private UserSchduleComponent userSchduleComponent;

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    void testConcurrentGetRequestsWithRestTemplate() throws InterruptedException {
        int requestCount = 1000;
        String url = "http://localhost:8080/user/regist?mode=double-service";

        ExecutorService executor = Executors.newFixedThreadPool(20); // 병렬 처리용 스레드 풀

        List<CompletableFuture<Void>> futures = IntStream.range(0, requestCount)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        String response = restTemplate.getForObject(url, String.class);
                        System.out.printf("Response[%d]: %s%n", i, response);
                    } catch (Exception e) {
                        System.err.printf("Error[%d]: %s%n", i, e.getMessage());
                    }
                }, executor))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
    }


    @Test
    void updateChangeName() throws InterruptedException {
        userSchduleComponent.requestUpdateUserName();
    }


    @Test
    void contextLoads() {

    }

}
