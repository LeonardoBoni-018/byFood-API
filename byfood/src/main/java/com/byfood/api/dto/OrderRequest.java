package com.byfood.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderRequest(
        @NotBlank(message = "nome do cliente é obrigatório")
        @Size(max = 120)
        String customerName,

        @NotBlank(message = "telefone do cliente é obrigatório")
        @Size(max = 30)
        @Pattern(regexp = "^[0-9+()\\-\\s]{8,30}$", message = "telefone inválido")
        String customerPhone,

        @NotBlank(message = "endereço do cliente é obrigatório")
        @Size(max = 255)
        String customerAddress,

        @NotEmpty(message = "os itens não podem estar vazios")
        @Size(max = 30, message = "o pedido pode ter no máximo 30 itens diferentes")
        List<@Valid OrderItemRequest> items
) {
}
