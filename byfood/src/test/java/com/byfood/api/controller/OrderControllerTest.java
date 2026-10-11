package com.byfood.api.controller;

import com.byfood.api.TestcontainersConfiguration;
import com.byfood.api.dto.MenuItemRequest;
import com.byfood.api.service.MenuItemService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    private static final AtomicInteger CLIENT = new AtomicInteger(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MenuItemService menuItemService;

    private Long createMenuItem() {
        return menuItemService.createMenuItem(new MenuItemRequest(
                "Burger", "desc", new BigDecimal("25.00"), "Main", null, true)).id();
    }

    /** Cada requisição usa um IP distinto para não esbarrar no rate limit de pedidos. */
    private MockHttpServletRequestBuilder postOrder(String body) {
        String ip = "10.1.0." + CLIENT.getAndIncrement();
        return post("/public/orders")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private String orderBody(Long itemId, int quantity, String phone) {
        return """
                {
                  "customerName": "John Doe",
                  "customerPhone": "%s",
                  "customerAddress": "Rua A, 10",
                  "items": [{"menuItemId": %d, "quantity": %d}]
                }
                """.formatted(phone, itemId, quantity);
    }

    @Test
    void shouldCreateOrder() throws Exception {
        Long itemId = createMenuItem();

        mockMvc.perform(postOrder(orderBody(itemId, 2, "+5511999998888")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/public/orders/")))
                .andExpect(jsonPath("$.trackingCode").isNotEmpty())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.total").value(50.00))
                .andExpect(jsonPath("$.whatsappLink").value(startsWith("https://wa.me/5511988888888?text=")));
    }

    @Test
    void shouldTrackOrderByTrackingCode() throws Exception {
        Long itemId = createMenuItem();

        String response = mockMvc.perform(postOrder(orderBody(itemId, 1, "+5511999998888")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String trackingCode = JsonPath.read(response, "$.trackingCode");

        mockMvc.perform(get("/public/orders/" + trackingCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingCode").value(trackingCode))
                .andExpect(jsonPath("$.customerName").value("John Doe"));
    }

    @Test
    void shouldNotExposeOrdersBySequentialId() throws Exception {
        mockMvc.perform(get("/public/orders/1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundForUnknownTrackingCode() throws Exception {
        mockMvc.perform(get("/public/orders/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectExcessiveQuantity() throws Exception {
        Long itemId = createMenuItem();

        mockMvc.perform(postOrder(orderBody(itemId, 51, "+5511999998888")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['items[0].quantity']").exists());
    }

    @Test
    void shouldRejectInvalidPhone() throws Exception {
        Long itemId = createMenuItem();

        mockMvc.perform(postOrder(orderBody(itemId, 1, "<script>")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.customerPhone").value("telefone inválido"));
    }
}
