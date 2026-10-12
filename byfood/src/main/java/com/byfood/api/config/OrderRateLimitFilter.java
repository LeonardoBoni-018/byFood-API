package com.byfood.api.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/** Evita que um único cliente inunde as lojas com pedidos falsos: POST /public/restaurants/{slug}/orders. */
@Component
public class OrderRateLimitFilter extends SlidingWindowRateLimitFilter {

    static final int MAX_ORDERS = 10;
    static final long WINDOW_MS = 10 * 60_000L;

    public OrderRateLimitFilter() {
        this(MAX_ORDERS, WINDOW_MS);
    }

    OrderRateLimitFilter(int maxOrders, long windowMs) {
        super(maxOrders, windowMs, "Muitos pedidos em sequência. Aguarde alguns minutos e tente novamente.");
    }

    @Override
    protected boolean matches(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "POST".equalsIgnoreCase(request.getMethod())
                && uri.startsWith("/public/restaurants/")
                && uri.endsWith("/orders");
    }
}
