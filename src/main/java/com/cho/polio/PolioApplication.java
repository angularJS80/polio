package com.cho.polio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PolioApplication {

    public static void main(String[] args) {
        SpringApplication.run(PolioApplication.class, args);
    }

}
