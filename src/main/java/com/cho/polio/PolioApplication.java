package com.cho.polio;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@EnableScheduling
public class PolioApplication {
    @Value("${spring.jackson.time-zone:UTC}")
    private String jacksonTimeZone;

    public static void main(String[] args) {
        SpringApplication.run(PolioApplication.class, args);
    }

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(jacksonTimeZone));
        System.out.println("Set JVM Default TimeZone to: " + jacksonTimeZone);
    }

}
