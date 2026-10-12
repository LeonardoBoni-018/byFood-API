package com.byfood.api.config;

import com.byfood.api.model.UserRole;

/** Identidade autenticada extraída do JWT e disponível via {@code @AuthenticationPrincipal}. */
public record AuthUser(Long id, String login, UserRole role) {
}
