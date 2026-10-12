package com.byfood.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRateLimitFilterTest {

    private final OrderRateLimitFilter filter = new OrderRateLimitFilter(2, 60_000L);

    private int perform(String method, String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr("10.0.0.5");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {
        });
        return response.getStatus();
    }

    @Test
    void shouldBlockOrderFloodFromSameClient() throws Exception {
        String uri = "/public/restaurants/pizzaria-do-ze/orders";
        assertThat(perform("POST", uri)).isEqualTo(200);
        assertThat(perform("POST", uri)).isEqualTo(200);
        assertThat(perform("POST", uri)).isEqualTo(429);
    }

    @Test
    void shouldNotLimitOrderTrackingOrMenuReads() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(perform("GET", "/public/orders/abc")).isEqualTo(200);
            assertThat(perform("GET", "/public/restaurants/pizzaria-do-ze/menu")).isEqualTo(200);
        }
    }
}
