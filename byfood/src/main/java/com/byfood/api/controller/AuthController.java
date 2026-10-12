package com.byfood.api.controller;

import com.byfood.api.dto.LoginRequest;
import com.byfood.api.dto.LoginResponse;
import com.byfood.api.dto.RegisterCustomerRequest;
import com.byfood.api.dto.RegisterStoreRequest;
import com.byfood.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(service.login(request));
    }

    @PostMapping("/register/customer")
    public ResponseEntity<LoginResponse> registerCustomer(@Valid @RequestBody RegisterCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registerCustomer(request));
    }

    @PostMapping("/register/store")
    public ResponseEntity<LoginResponse> registerStore(@Valid @RequestBody RegisterStoreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registerStore(request));
    }
}
