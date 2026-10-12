package com.byfood.api.controller;

import com.byfood.api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    /** Cada chamada usa um IP diferente para não esbarrar nos rate limits de login/cadastro. */
    private MockHttpServletRequestBuilder json(String path, String body) {
        String ip = "10.2.0." + SEQ.incrementAndGet();
        return post(path).with(request -> {
            request.setRemoteAddr(ip);
            return request;
        }).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String customerBody(String email, String password) {
        return """
                {"name":"Maria","email":"%s","phone":"+5511999990000","password":"%s"}
                """.formatted(email, password);
    }

    private String storeBody(String email, String storeName) {
        return """
                {"ownerName":"Joao","email":"%s","password":"senha-forte-1",
                 "store":{"name":"%s","description":"Boa comida","cuisine":"Pizza","address":"Rua A, 1",
                          "phone":"+5511999990000","whatsappNumber":"+5511988880000","openingHours":"11h-23h"}}
                """.formatted(email, storeName);
    }

    @Test
    void shouldLoginLegacyOwnerWithValidCredentials() throws Exception {
        mockMvc.perform(json("/auth/login", """
                        {"username":"admin","password":"admin123"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("STORE"));
    }

    @Test
    void shouldRejectWrongPasswordAndUnknownUser() throws Exception {
        mockMvc.perform(json("/auth/login", """
                        {"username":"admin","password":"wrong"}
                        """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(json("/auth/login", """
                        {"username":"ghost","password":"admin123"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRegisterCustomerAndLoginCaseInsensitively() throws Exception {
        mockMvc.perform(json("/auth/register/customer", customerBody("Maria.Auth@Teste.com", "senha-forte-1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.login").value("maria.auth@teste.com"));

        mockMvc.perform(json("/auth/login", """
                        {"username":"MARIA.AUTH@teste.com","password":"senha-forte-1"}
                        """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        mockMvc.perform(json("/auth/register/customer", customerBody("dup@teste.com", "senha-forte-1")))
                .andExpect(status().isCreated());
        mockMvc.perform(json("/auth/register/customer", customerBody("DUP@teste.com", "senha-forte-1")))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectWeakPasswordAndInvalidEmail() throws Exception {
        mockMvc.perform(json("/auth/register/customer", customerBody("fraca@teste.com", "123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
        mockMvc.perform(json("/auth/register/customer", customerBody("nao-e-email", "senha-forte-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void shouldRegisterStoreWithGeneratedSlug() throws Exception {
        mockMvc.perform(json("/auth/register/store", storeBody("loja.auth@teste.com", "Doceria da Vó")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("STORE"));
    }

    @Test
    void shouldValidateNestedStoreFields() throws Exception {
        mockMvc.perform(json("/auth/register/store", """
                        {"ownerName":"Joao","email":"semloja@teste.com","password":"senha-forte-1",
                         "store":{"name":"","cuisine":"","address":"","phone":"x","whatsappNumber":""}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['store.name']").exists())
                .andExpect(jsonPath("$.errors['store.phone']").exists());
    }

    @Test
    void shouldNotAcceptRoleFromClient() throws Exception {
        mockMvc.perform(json("/auth/register/customer", """
                        {"name":"Hacker","email":"hacker@teste.com","password":"senha-forte-1","role":"STORE"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
    }
}
