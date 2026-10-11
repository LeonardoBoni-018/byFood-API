package com.byfood.api.controller;

import com.byfood.api.dto.OrderRequest;
import com.byfood.api.dto.OrderResponse;
import com.byfood.api.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/public/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        OrderResponse created = service.createOrder(request);
        return ResponseEntity.created(URI.create("/public/orders/" + created.trackingCode())).body(created);
    }

    @GetMapping("/{trackingCode}")
    public ResponseEntity<OrderResponse> getByTrackingCode(@PathVariable UUID trackingCode) {
        return ResponseEntity.ok(service.getOrderByTrackingCode(trackingCode));
    }
}