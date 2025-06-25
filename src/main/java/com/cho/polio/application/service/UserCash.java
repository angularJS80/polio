package com.cho.polio.application.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserCash {

    // 이름 점유 캐시 (동시성 고려 필요)
    private final Set<String> nameInProgress = ConcurrentHashMap.newKeySet();

    private final Map<String, String> nextNameInProgress = new ConcurrentHashMap<>();


    public Set<String> getNameInProgress() {
        return nameInProgress;
    }

    public  Map<String, String> getNextNameInProgress() {
        return nextNameInProgress;
    }

}
