package com.byfood.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull(message = "menuItemId é obrigatório")
        Long menuItemId,

        @NotNull(message = "quantidade é obrigatória")
        @Min(value = 1, message = "quantidade deve ser no mínimo 1")
        @Max(value = 50, message = "quantidade deve ser no máximo 50")
        Integer quantity
) {
}
