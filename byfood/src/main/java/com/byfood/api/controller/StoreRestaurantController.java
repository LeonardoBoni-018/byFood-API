package com.byfood.api.controller;

import com.byfood.api.config.AuthUser;
import com.byfood.api.dto.RestaurantResponse;
import com.byfood.api.dto.StoreProfileRequest;
import com.byfood.api.service.RestaurantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/store/restaurant")
public class StoreRestaurantController {

    private final RestaurantService service;

    public StoreRestaurantController(RestaurantService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<RestaurantResponse> get(@AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(service.getOwned(user.id()));
    }

    @PutMapping
    public ResponseEntity<RestaurantResponse> update(@AuthenticationPrincipal AuthUser user,
                                                     @Valid @RequestBody StoreProfileRequest request) {
        return ResponseEntity.ok(service.updateOwned(user.id(), request));
    }
}
