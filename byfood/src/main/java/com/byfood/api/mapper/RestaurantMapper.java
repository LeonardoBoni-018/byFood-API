package com.byfood.api.mapper;

import com.byfood.api.dto.RestaurantResponse;
import com.byfood.api.dto.RestaurantSummaryResponse;
import com.byfood.api.dto.StoreProfileRequest;
import com.byfood.api.model.Restaurant;

public final class RestaurantMapper {

    private RestaurantMapper() {
    }

    public static RestaurantResponse toResponse(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getSlug(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getCuisine(),
                restaurant.getAddress(),
                restaurant.getPhone(),
                restaurant.getWhatsappNumber(),
                restaurant.getOpeningHours(),
                restaurant.getImageUrl(),
                restaurant.isActive()
        );
    }

    public static RestaurantSummaryResponse toSummary(Restaurant restaurant) {
        return new RestaurantSummaryResponse(
                restaurant.getSlug(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getCuisine(),
                restaurant.getOpeningHours(),
                restaurant.getImageUrl()
        );
    }

    /** Copia os campos editáveis; slug, dono e status não podem ser alterados por esta via. */
    public static void apply(StoreProfileRequest request, Restaurant target) {
        target.setName(request.name().trim());
        target.setDescription(blankToNull(request.description()));
        target.setCuisine(request.cuisine().trim());
        target.setAddress(request.address().trim());
        target.setPhone(request.phone().trim());
        target.setWhatsappNumber(request.whatsappNumber().trim());
        target.setOpeningHours(blankToNull(request.openingHours()));
        target.setImageUrl(blankToNull(request.imageUrl()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
