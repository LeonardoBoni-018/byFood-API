package com.byfood.api.controller;

import com.byfood.api.config.AuthUser;
import com.byfood.api.dto.OrderResponse;
import com.byfood.api.dto.UserResponse;
import com.byfood.api.service.AuthService;
import com.byfood.api.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Área do cliente autenticado: perfil e histórico de pedidos. */
@RestController
@RequestMapping("/me")
public class MeController {

    private final AuthService authService;
    private final OrderService orderService;

    public MeController(AuthService authService, OrderService orderService) {
        this.authService = authService;
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<UserResponse> profile(@AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(authService.getProfile(user.id()));
    }

    @GetMapping("/orders")
    public ResponseEntity<Page<OrderResponse>> orders(@AuthenticationPrincipal AuthUser user,
                                                      @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrdersForCustomer(user.id(), Pageables.unsorted(pageable)));
    }
}
