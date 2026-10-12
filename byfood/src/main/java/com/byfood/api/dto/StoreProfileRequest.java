package com.byfood.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dados públicos da loja, usados no cadastro e na edição pelo dono. */
public record StoreProfileRequest(
        @NotBlank(message = "nome da loja é obrigatório")
        @Size(max = 120, message = "nome da loja deve ter no máximo 120 caracteres")
        String name,

        @Size(max = 500, message = "descrição deve ter no máximo 500 caracteres")
        String description,

        @NotBlank(message = "tipo de culinária é obrigatório")
        @Size(max = 60, message = "culinária deve ter no máximo 60 caracteres")
        String cuisine,

        @NotBlank(message = "endereço é obrigatório")
        @Size(max = 255, message = "endereço deve ter no máximo 255 caracteres")
        String address,

        @NotBlank(message = "telefone é obrigatório")
        @Pattern(regexp = "^[0-9+()\\-\\s]{8,30}$", message = "telefone inválido")
        String phone,

        @NotBlank(message = "WhatsApp é obrigatório")
        @Pattern(regexp = "^[0-9+()\\-\\s]{8,30}$", message = "WhatsApp inválido")
        String whatsappNumber,

        @Size(max = 255, message = "horário deve ter no máximo 255 caracteres")
        String openingHours,

        @Size(max = 500, message = "URL da imagem deve ter no máximo 500 caracteres")
        @Pattern(regexp = "^(https?://\\S+)?$", message = "URL da imagem deve começar com http:// ou https://")
        String imageUrl
) {
}
