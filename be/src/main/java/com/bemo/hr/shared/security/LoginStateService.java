package com.bemo.hr.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
public class LoginStateService {
    static final int MAX_LOGIN_ATTEMPTS = 5;
    static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private final AppUserRepository appUserRepository;

    public LoginStateService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String appId, String username, Instant now) {
        log.debug("recordSuccess called with appId={}, username={}", appId, username);
        appUserRepository.findByAppIdAndUsernameIgnoreCase(appId, username).ifPresent(user -> {
            user.recordSuccessfulLogin(now);
            appUserRepository.save(user);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String appId, String username, Instant now) {
        log.debug("recordFailure called with appId={}, username={}", appId, username);
        appUserRepository.findByAppIdAndUsernameIgnoreCase(appId, username).ifPresent(user -> {
            user.recordFailedLogin(now, MAX_LOGIN_ATTEMPTS, LOCKOUT_DURATION);
            appUserRepository.save(user);
        });
    }
}
