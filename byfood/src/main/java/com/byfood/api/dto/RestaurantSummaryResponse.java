package com.byfood.api.dto;

/** Versão enxuta para a vitrine de lojas: sem telefone/WhatsApp, que só aparecem na página da loja. */
public record RestaurantSummaryResponse(
        String slug,
        String name,
        String description,
        String cuisine,
        String openingHours,
        String imageUrl
) {
}
