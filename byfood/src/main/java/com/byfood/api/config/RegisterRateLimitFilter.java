package com.byfood.api.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/** Limita criação de contas (cliente e loja) por IP para dificultar cadastros em massa. */
@Component
public class RegisterRateLimitFilter extends SlidingWindowRateLimitFilter {

    static final int MAX_REGISTRATIONS = 5;
    static final long WINDOW_MS = 60 * 60_000L;

    public RegisterRateLimitFilter() {
        this(MAX_REGISTRATIONS, WINDOW_MS);
    }

    RegisterRateLimitFilter(int maxRegistrations, long windowMs) {
        super(maxRegistrations, windowMs, "Muitos cadastros a partir deste endereço. Tente novamente mais tarde.");
    }

    @Override
    protected boolean matches(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().startsWith("/auth/register/");
    }
}
