package com.byfood.api.controller;

import com.byfood.api.TestFixtures;
import com.byfood.api.TestcontainersConfiguration;
import com.byfood.api.service.AuthService;
import com.byfood.api.service.MenuItemService;
import com.byfood.api.service.RestaurantService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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

    @Autowired
    private AuthService authService;

    @Autowired
    private RestaurantService restaurantService;

    /** Cada requisição usa um IP distinto para não esbarrar no rate limit de pedidos. */
    private MockHttpServletRequestBuilder postOrder(String slug, String body) {
        String ip = "10.1.0." + CLIENT.getAndIncrement();
        return post("/public/restaurants/" + slug + "/orders")
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

    private record Setup(TestFixtures.Store store, Long itemId) {
    }

    private Setup setup() {
        var store = TestFixtures.store(authService, restaurantService, "Loja " + UUID.randomUUID());
        return new Setup(store, TestFixtures.menuItem(menuItemService, store.ownerId(), "Burger", "25.00"));
    }

    @Test
    void shouldCreateOrderAsGuest() throws Exception {
        var s = setup();

        mockMvc.perform(postOrder(s.store().slug(), orderBody(s.itemId(), 2, "+5511999998888")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/public/orders/")))
                .andExpect(jsonPath("$.trackingCode").isNotEmpty())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.total").value(50.00))
                .andExpect(jsonPath("$.restaurantSlug").value(s.store().slug()))
                .andExpect(jsonPath("$.whatsappLink").value(startsWith("https://wa.me/5511988880000?text=")));
    }

    @Test
    void shouldAppearInCustomerHistoryWhenLoggedIn() throws Exception {
        var s = setup();
        var customer = TestFixtures.customer(authService);

        mockMvc.perform(postOrder(s.store().slug(), orderBody(s.itemId(), 1, "+5511999998888"))
                        .header("Authorization", "Bearer " + customer.token()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/me/orders").header("Authorization", "Bearer " + customer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].restaurantSlug").value(s.store().slug()));
    }

    @Test
    void shouldTrackOrderByTrackingCode() throws Exception {
        var s = setup();

        String response = mockMvc.perform(postOrder(s.store().slug(), orderBody(s.itemId(), 1, "+5511999998888")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String trackingCode = JsonPath.read(response, "$.trackingCode");

        mockMvc.perform(get("/public/orders/" + trackingCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingCode").value(trackingCode))
                .andExpect(jsonPath("$.customerName").value("John Doe"));
    }

    @Test
    void shouldRejectItemFromAnotherStore() throws Exception {
        var s = setup();
        var other = TestFixtures.store(authService, restaurantService, "Outra " + UUID.randomUUID());

        mockMvc.perform(postOrder(other.slug(), orderBody(s.itemId(), 1, "+5511999998888")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundForUnknownStore() throws Exception {
        var s = setup();

        mockMvc.perform(postOrder("nao-existe", orderBody(s.itemId(), 1, "+5511999998888")))
                .andExpect(status().isNotFound());
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
        var s = setup();

        mockMvc.perform(postOrder(s.store().slug(), orderBody(s.itemId(), 51, "+5511999998888")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['items[0].quantity']").exists());
    }

    @Test
    void shouldRejectInvalidPhone() throws Exception {
        var s = setup();

        mockMvc.perform(postOrder(s.store().slug(), orderBody(s.itemId(), 1, "<script>")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.customerPhone").value("telefone inválido"));
    }

    @Test
    void shouldIgnoreInvalidTokenOnPublicOrder() throws Exception {
        var s = setup();

        mockMvc.perform(postOrder(s.store().slug(), orderBody(s.itemId(), 1, "+5511999998888"))
                        .header("Authorization", "Bearer lixo.lixo.lixo"))
                .andExpect(status().isCreated());
    }
}
