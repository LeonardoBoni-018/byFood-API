package com.byfood.api.config;

import org.springframework.stereotype.Component;

/** Evita que um único cliente inunde o painel com pedidos falsos. */
@Component
public class OrderRateLimitFilter extends SlidingWindowRateLimitFilter {

    static final int MAX_ORDERS = 10;
    static final long WINDOW_MS = 10 * 60_000L;

    public OrderRateLimitFilter() {
        this(MAX_ORDERS, WINDOW_MS);
    }

    OrderRateLimitFilter(int maxOrders, long windowMs) {
        super("POST", "/public/orders", maxOrders, windowMs,
                "Muitos pedidos em sequência. Aguarde alguns minutos e tente novamente.");
    }
}
