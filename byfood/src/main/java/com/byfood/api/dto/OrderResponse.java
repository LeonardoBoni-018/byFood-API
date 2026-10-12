package com.byfood.api.dto;

import com.byfood.api.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        Long id,
        UUID trackingCode,
        String restaurantName,
        String restaurantSlug,
        String customerName,
        String customerPhone,
        String customerAddress,
        OrderStatus status,
        BigDecimal total,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        String whatsappLink
) {
}
