package com.byfood.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MenuItemRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 150)
        String name,

        @Size(max = 500)
        String description,

        @NotNull(message = "preço é obrigatório")
        @DecimalMin(value = "0.01", message = "preço deve ser maior que zero")
        @DecimalMax(value = "99999.99", message = "preço deve ser menor que 100.000")
        BigDecimal price,

        @NotBlank(message = "categoria é obrigatória")
        @Size(max = 80)
        String category,

        @Size(max = 500, message = "URL da imagem deve ter no máximo 500 caracteres")
        @Pattern(regexp = "^(https?://\\S+)?$", message = "URL da imagem deve começar com http:// ou https://")
        String imageUrl,

        boolean available
) {
}
