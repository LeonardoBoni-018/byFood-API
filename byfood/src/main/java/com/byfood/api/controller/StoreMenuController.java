package com.byfood.api.controller;

import com.byfood.api.config.AuthUser;
import com.byfood.api.dto.MenuItemRequest;
import com.byfood.api.dto.MenuItemResponse;
import com.byfood.api.service.MenuItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/** Cardápio da loja do usuário autenticado. A loja nunca vem da URL: sai do token, evitando acesso a outras lojas. */
@RestController
@RequestMapping("/store/menu")
public class StoreMenuController {

    private final MenuItemService service;

    public StoreMenuController(MenuItemService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<MenuItemResponse>> getAll(@AuthenticationPrincipal AuthUser user,
                                                         @PageableDefault(size = 100) Pageable pageable) {
        return ResponseEntity.ok(service.getAllForOwner(user.id(), Pageables.unsorted(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuItemResponse> getById(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ResponseEntity.ok(service.getForOwner(user.id(), id));
    }

    @PostMapping
    public ResponseEntity<MenuItemResponse> create(@AuthenticationPrincipal AuthUser user,
                                                   @Valid @RequestBody MenuItemRequest request) {
        MenuItemResponse created = service.create(user.id(), request);
        return ResponseEntity.created(URI.create("/store/menu/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuItemResponse> update(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                                   @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(service.update(user.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        service.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}
