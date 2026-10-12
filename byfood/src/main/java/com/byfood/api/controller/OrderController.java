package com.byfood.api.controller;

import com.byfood.api.config.AuthUser;
import com.byfood.api.dto.OrderRequest;
import com.byfood.api.dto.OrderResponse;
import com.byfood.api.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/public")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    /** Pedido de visitante ou de cliente logado (o token é opcional) para a loja {@code slug}. */
    @PostMapping("/restaurants/{slug}/orders")
    public ResponseEntity<OrderResponse> create(@PathVariable String slug,
                                                @Valid @RequestBody OrderRequest request,
                                                @AuthenticationPrincipal AuthUser user) {
        OrderResponse created = service.createOrder(slug, request, user);
        return ResponseEntity.created(URI.create("/public/orders/" + created.trackingCode())).body(created);
    }

    @GetMapping("/orders/{trackingCode}")
    public ResponseEntity<OrderResponse> getByTrackingCode(@PathVariable UUID trackingCode) {
        return ResponseEntity.ok(service.getOrderByTrackingCode(trackingCode));
    }
}
