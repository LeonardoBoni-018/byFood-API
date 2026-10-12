package com.byfood.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterStoreRequest(
        @NotBlank(message = "nome do responsável é obrigatório")
        @Size(max = 120, message = "nome deve ter no máximo 120 caracteres")
        String ownerName,

        @NotBlank(message = "e-mail é obrigatório")
        @Email(message = "e-mail inválido")
        @Size(max = 120, message = "e-mail deve ter no máximo 120 caracteres")
        String email,

        @NotBlank(message = "senha é obrigatória")
        @Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres")
        String password,

        @NotNull(message = "dados da loja são obrigatórios")
        @Valid
        StoreProfileRequest store
) {
}
