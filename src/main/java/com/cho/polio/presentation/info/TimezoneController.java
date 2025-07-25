package com.cho.polio.presentation.info;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TimezoneController {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/api/timezones")
    public Map<String, String> getTimezones() {
        Map<String, String> result = new HashMap<>();

        // 1. JVM(System) 타임존
        ZoneId systemZone = ZoneId.systemDefault();
        result.put("systemTimeZone", systemZone.toString());

        // 2. MySQL 서버 타임존 조회 (JPA 연결 세션 기준)
        String dbTimeZone = "unknown";
        try {
            // native query 실행
            dbTimeZone = (String) entityManager.createNativeQuery("SELECT @@global.time_zone").getSingleResult();
            if ("SYSTEM".equalsIgnoreCase(dbTimeZone)) {
                dbTimeZone = (String) entityManager.createNativeQuery("SELECT @@system_time_zone").getSingleResult();
            }
        } catch (Exception e) {
            dbTimeZone = "error: " + e.getMessage();
        }
        result.put("dbTimeZone", dbTimeZone);

        // 3. API 타임존 (JVM 기준으로 출력)
        result.put("apiTimeZone", systemZone.toString());

        // 4. API가 현재 이용하는 시간(now) - UTC 기준 Instant
        Instant utcNow = Instant.now();
        result.put("apiNowUtc", utcNow.toString());

        // 5. API가 현재 이용하는 시간(now) - 시스템 기본 타임존 기준 ZonedDateTime
        ZonedDateTime systemNow = ZonedDateTime.now(systemZone);
        result.put("apiNowSystemZone", systemNow.toString());

        return result;
    }
}
