package com.byfood.api.dto;

import com.byfood.api.model.UserRole;

public record UserResponse(
        Long id,
        String name,
        String login,
        String phone,
        UserRole role
) {
}
