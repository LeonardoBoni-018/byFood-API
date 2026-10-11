package com.byfood.api.mapper;

import com.byfood.api.dto.OrderItemResponse;
import com.byfood.api.dto.OrderResponse;
import com.byfood.api.model.Order;
import com.byfood.api.model.OrderItem;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(Order order, String whatsappLink) {
        return new OrderResponse(
                order.getId(),
                order.getTrackingCode(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getCustomerAddress(),
                order.getStatus(),
                order.getTotal(),
                order.getItems().stream()
                        .map(OrderMapper::toItemResponse)
                        .toList(),
                order.getCreatedAt(),
                whatsappLink
        );
    }

    private static OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getItemName(),
                item.getUnitPrice(),
                item.getQuantity()
        );
    }
}