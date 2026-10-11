package com.byfood.api.config;

import org.springframework.stereotype.Component;

@Component
public class LoginRateLimitFilter extends SlidingWindowRateLimitFilter {

    static final int MAX_ATTEMPTS = 5;
    static final long WINDOW_MS = 60_000L;

    public LoginRateLimitFilter() {
        this(MAX_ATTEMPTS, WINDOW_MS);
    }

    LoginRateLimitFilter(int maxAttempts, long windowMs) {
        super("POST", "/auth/login", maxAttempts, windowMs,
                "Muitas tentativas de login. Tente novamente em 1 minuto.");
    }
}
