package com.byfood.api.dto;

public record RestaurantResponse(
        Long id,
        String slug,
        String name,
        String description,
        String cuisine,
        String address,
        String phone,
        String whatsappNumber,
        String openingHours,
        String imageUrl,
        boolean active
) {
}
