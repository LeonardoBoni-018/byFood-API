package com.byfood.api.service;

import com.byfood.api.config.JwtService;
import com.byfood.api.dto.LoginRequest;
import com.byfood.api.dto.LoginResponse;
import com.byfood.api.dto.RegisterCustomerRequest;
import com.byfood.api.dto.RegisterStoreRequest;
import com.byfood.api.dto.UserResponse;
import com.byfood.api.exception.ConflictException;
import com.byfood.api.exception.NotFoundException;
import com.byfood.api.model.AppUser;
import com.byfood.api.model.UserRole;
import com.byfood.api.repository.AppUserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestaurantService restaurantService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService,
                       AppUserRepository userRepository, PasswordEncoder passwordEncoder,
                       RestaurantService restaurantService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.restaurantService = restaurantService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String login = AppUserDetailsService.normalize(request.username());
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(login, request.password()));
        AppUser user = userRepository.findByLogin(login)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        return toLoginResponse(user);
    }

    @Transactional
    public LoginResponse registerCustomer(RegisterCustomerRequest request) {
        AppUser user = createUser(request.email(), request.password(), request.name(), request.phone(), UserRole.CUSTOMER);
        return toLoginResponse(user);
    }

    @Transactional
    public LoginResponse registerStore(RegisterStoreRequest request) {
        AppUser owner = createUser(request.email(), request.password(), request.ownerName(),
                request.store().phone(), UserRole.STORE);
        restaurantService.createForOwner(owner, request.store());
        return toLoginResponse(owner);
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return userRepository.findById(userId)
                .map(AuthService::toUserResponse)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }

    private AppUser createUser(String email, String rawPassword, String name, String phone, UserRole role) {
        String login = AppUserDetailsService.normalize(email);
        if (userRepository.existsByLogin(login)) {
            throw new ConflictException("Este e-mail já está cadastrado");
        }
        AppUser user = AppUser.builder()
                .login(login)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .name(name.trim())
                .phone(phone == null || phone.isBlank() ? null : phone.trim())
                .role(role)
                .build();
        return userRepository.save(user);
    }

    private LoginResponse toLoginResponse(AppUser user) {
        String token = jwtService.generateToken(user);
        Instant expiresAt = Instant.now().plusMillis(jwtService.getExpirationMs());
        return new LoginResponse(token, expiresAt, toUserResponse(user));
    }

    private static UserResponse toUserResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getName(), user.getLogin(), user.getPhone(), user.getRole());
    }
}
