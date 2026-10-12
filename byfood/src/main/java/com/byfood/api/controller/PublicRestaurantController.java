package com.byfood.api.controller;

import com.byfood.api.dto.MenuItemResponse;
import com.byfood.api.dto.RestaurantResponse;
import com.byfood.api.dto.RestaurantSummaryResponse;
import com.byfood.api.service.MenuItemService;
import com.byfood.api.service.RestaurantService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/public/restaurants")
public class PublicRestaurantController {

    // Dados públicos mudam pouco: um cache curto no navegador/CDN alivia a API (Render free hiberna)
    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic();

    private final RestaurantService restaurantService;
    private final MenuItemService menuItemService;

    public PublicRestaurantController(RestaurantService restaurantService, MenuItemService menuItemService) {
        this.restaurantService = restaurantService;
        this.menuItemService = menuItemService;
    }

    @GetMapping
    public ResponseEntity<Page<RestaurantSummaryResponse>> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String cuisine,
            @PageableDefault(size = 12) Pageable pageable) {
        return ResponseEntity.ok().cacheControl(CACHE)
                .body(restaurantService.search(q, cuisine, Pageables.unsorted(pageable)));
    }

    @GetMapping("/cuisines")
    public ResponseEntity<List<String>> cuisines() {
        return ResponseEntity.ok().cacheControl(CACHE).body(restaurantService.cuisines());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<RestaurantResponse> get(@PathVariable String slug) {
        return ResponseEntity.ok().cacheControl(CACHE).body(restaurantService.getPublic(slug));
    }

    @GetMapping("/{slug}/menu")
    public ResponseEntity<Page<MenuItemResponse>> menu(@PathVariable String slug,
                                                       @PageableDefault(size = 100) Pageable pageable) {
        return ResponseEntity.ok().cacheControl(CACHE)
                .body(menuItemService.getPublicMenu(slug, Pageables.unsorted(pageable)));
    }
}
