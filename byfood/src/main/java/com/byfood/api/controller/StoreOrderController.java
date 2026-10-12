package com.byfood.api.controller;

import com.byfood.api.config.AuthUser;
import com.byfood.api.dto.OrderResponse;
import com.byfood.api.dto.OrderStatusUpdateRequest;
import com.byfood.api.model.OrderStatus;
import com.byfood.api.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/store/orders")
public class StoreOrderController {

    private final OrderService service;

    public StoreOrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAll(@AuthenticationPrincipal AuthUser user,
                                                      @RequestParam(required = false) OrderStatus status,
                                                      @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.getOrdersForOwner(user.id(), status, Pageables.unsorted(pageable)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                                      @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(service.updateOrderStatus(user.id(), id, request.status()));
    }
}
