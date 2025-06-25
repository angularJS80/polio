package com.cho.polio.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartupRunner implements ApplicationRunner {

    private final UserSchduleComponent userSchduleComponent;
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 애플리케이션 시작 후 실행할 코드
        System.out.println("Application started!");
        userSchduleComponent.testRegist();
        userSchduleComponent.requestUpdateUserName();
    }
}