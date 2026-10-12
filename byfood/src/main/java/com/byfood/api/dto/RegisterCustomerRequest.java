package com.byfood.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterCustomerRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 120, message = "nome deve ter no máximo 120 caracteres")
        String name,

        @NotBlank(message = "e-mail é obrigatório")
        @Email(message = "e-mail inválido")
        @Size(max = 120, message = "e-mail deve ter no máximo 120 caracteres")
        String email,

        @Pattern(regexp = "^([0-9+()\\-\\s]{8,30})?$", message = "telefone inválido")
        String phone,

        // BCrypt considera apenas os primeiros 72 bytes
        @NotBlank(message = "senha é obrigatória")
        @Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres")
        String password
) {
}
