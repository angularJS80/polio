package com.cho.polio.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        // 인증된 사용자 ID를 반환하거나, 없으면 "system" 같은 기본값 반환
        return Optional.of("system");
    }
}
