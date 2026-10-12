package com.byfood.api.service;

import com.byfood.api.dto.RestaurantResponse;
import com.byfood.api.dto.RestaurantSummaryResponse;
import com.byfood.api.dto.StoreProfileRequest;
import com.byfood.api.exception.NotFoundException;
import com.byfood.api.mapper.RestaurantMapper;
import com.byfood.api.model.AppUser;
import com.byfood.api.model.Restaurant;
import com.byfood.api.repository.RestaurantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RestaurantService {

    private static final int MAX_SLUG_LENGTH = 60;
    private static final int MAX_QUERY_LENGTH = 60;

    private final RestaurantRepository repository;

    public RestaurantService(RestaurantRepository repository) {
        this.repository = repository;
    }

    // ------------------------------------------------------------------ público

    @Transactional(readOnly = true)
    public Page<RestaurantSummaryResponse> search(String q, String cuisine, Pageable pageable) {
        return repository.search(escapeLike(q), cuisine == null ? "" : cuisine.trim(), pageable)
                .map(RestaurantMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public List<String> cuisines() {
        return repository.findActiveCuisines();
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getPublic(String slug) {
        return RestaurantMapper.toResponse(requireActiveBySlug(slug));
    }

    /** Loja visível ao público; lojas desativadas respondem como inexistentes. */
    @Transactional(readOnly = true)
    public Restaurant requireActiveBySlug(String slug) {
        return repository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new NotFoundException("Loja não encontrada"));
    }

    // -------------------------------------------------------------------- dono

    @Transactional(readOnly = true)
    public Restaurant requireByOwner(Long ownerId) {
        return repository.findByOwnerId(ownerId)
                .orElseThrow(() -> new NotFoundException("Loja não encontrada para este usuário"));
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getOwned(Long ownerId) {
        return RestaurantMapper.toResponse(requireByOwner(ownerId));
    }

    @Transactional
    public RestaurantResponse updateOwned(Long ownerId, StoreProfileRequest request) {
        Restaurant restaurant = requireByOwner(ownerId);
        RestaurantMapper.apply(request, restaurant);
        return RestaurantMapper.toResponse(repository.save(restaurant));
    }

    @Transactional
    public Restaurant createForOwner(AppUser owner, StoreProfileRequest request) {
        Restaurant restaurant = Restaurant.builder().owner(owner).slug(uniqueSlug(request.name())).build();
        RestaurantMapper.apply(request, restaurant);
        return repository.save(restaurant);
    }

    // ------------------------------------------------------------------ helpers

    private String uniqueSlug(String name) {
        String base = slugify(name);
        if (!repository.existsBySlug(base)) {
            return base;
        }
        for (int i = 2; i <= 20; i++) {
            String candidate = base + "-" + i;
            if (!repository.existsBySlug(candidate)) {
                return candidate;
            }
        }
        return base + "-" + Integer.toHexString(ThreadLocalRandom.current().nextInt(0x10000, 0xFFFFF));
    }

    static String slugify(String name) {
        String slug = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.isEmpty()) {
            return "loja";
        }
        if (slug.length() > MAX_SLUG_LENGTH) {
            slug = slug.substring(0, MAX_SLUG_LENGTH).replaceAll("-+$", "");
        }
        return slug;
    }

    /** Escapa curingas do LIKE para que a busca do usuário seja sempre literal. */
    static String escapeLike(String q) {
        if (q == null) {
            return "";
        }
        String trimmed = q.trim();
        if (trimmed.length() > MAX_QUERY_LENGTH) {
            trimmed = trimmed.substring(0, MAX_QUERY_LENGTH);
        }
        return trimmed.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
