package com.byfood.api.config;

import com.byfood.api.TestFixtures;
import com.byfood.api.TestcontainersConfiguration;
import com.byfood.api.service.AuthService;
import com.byfood.api.service.RestaurantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "app.cors.allowed-origins=https://byfood.vercel.app,https://byfood-*.vercel.app")
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private RestaurantService restaurantService;

    @Test
    void shouldAllowPreflightFromConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/public/restaurants/bistro-byfood/orders")
                        .header("Origin", "https://byfood.vercel.app")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://byfood.vercel.app"));
    }

    @Test
    void shouldAllowVercelPreviewOrigins() throws Exception {
        mockMvc.perform(get("/public/restaurants").header("Origin", "https://byfood-git-feature-x.vercel.app"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://byfood-git-feature-x.vercel.app"));
    }

    @Test
    void shouldRejectUnknownOrigin() throws Exception {
        mockMvc.perform(options("/public/restaurants/bistro-byfood/orders")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldSendSecurityHeaders() throws Exception {
        mockMvc.perform(get("/public/restaurants/bistro-byfood"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void shouldDenyUnmappedPaths() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectTamperedToken() throws Exception {
        mockMvc.perform(get("/store/orders").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectOldAdminPaths() throws Exception {
        mockMvc.perform(get("/admin/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldSeparateStoreAndCustomerAreas() throws Exception {
        var store = TestFixtures.store(authService, restaurantService, "Loja Roles");
        var customer = TestFixtures.customer(authService);

        mockMvc.perform(get("/store/orders").header("Authorization", "Bearer " + store.token()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/me/orders").header("Authorization", "Bearer " + customer.token()))
                .andExpect(status().isOk());

        // papel errado: autenticado, porém sem permissão
        mockMvc.perform(get("/store/orders").header("Authorization", "Bearer " + customer.token()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/store/menu").header("Authorization", "Bearer " + customer.token()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/me/orders").header("Authorization", "Bearer " + store.token()))
                .andExpect(status().isForbidden());

        // sem token
        mockMvc.perform(get("/store/orders")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/me")).andExpect(status().isUnauthorized());
    }
}
