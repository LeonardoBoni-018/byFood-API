package com.byfood.api.controller;

import com.byfood.api.TestFixtures;
import com.byfood.api.TestcontainersConfiguration;
import com.byfood.api.dto.OrderItemRequest;
import com.byfood.api.dto.OrderRequest;
import com.byfood.api.dto.OrderResponse;
import com.byfood.api.service.AuthService;
import com.byfood.api.service.MenuItemService;
import com.byfood.api.service.OrderService;
import com.byfood.api.service.RestaurantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Garante, via HTTP, que uma loja não enxerga nem altera dados de outra (IDOR). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class StoreAccessControlTest {

    private static final String ITEM_JSON = """
            {"name":"Prato novo","description":"d","price":12.50,"category":"Main","available":true}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private MenuItemService menuItemService;

    @Autowired
    private OrderService orderService;

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void ownerManagesOwnMenuThroughHttp() throws Exception {
        var store = TestFixtures.store(authService, restaurantService, "Menu HTTP");

        mockMvc.perform(post("/store/menu").header("Authorization", bearer(store.token()))
                        .contentType(MediaType.APPLICATION_JSON).content(ITEM_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Prato novo"));

        mockMvc.perform(get("/store/menu").header("Authorization", bearer(store.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/public/restaurants/" + store.slug() + "/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void otherStoreCannotReadUpdateOrDeleteMenuItem() throws Exception {
        var owner = TestFixtures.store(authService, restaurantService, "Dono Menu");
        var intruder = TestFixtures.store(authService, restaurantService, "Intruso Menu");
        Long itemId = TestFixtures.menuItem(menuItemService, owner.ownerId(), "Segredo", "9.90");
        String auth = bearer(intruder.token());

        mockMvc.perform(get("/store/menu/" + itemId).header("Authorization", auth)).andExpect(status().isNotFound());
        mockMvc.perform(put("/store/menu/" + itemId).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(ITEM_JSON))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/store/menu/" + itemId).header("Authorization", auth)).andExpect(status().isNotFound());

        mockMvc.perform(get("/store/menu").header("Authorization", auth))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void otherStoreCannotChangeOrderStatus() throws Exception {
        var owner = TestFixtures.store(authService, restaurantService, "Dono Pedido");
        var intruder = TestFixtures.store(authService, restaurantService, "Intruso Pedido");
        Long itemId = TestFixtures.menuItem(menuItemService, owner.ownerId(), "Prato", "10.00");
        OrderResponse order = orderService.createOrder(owner.slug(), new OrderRequest("Cli", "+5511999998888", "Rua",
                List.of(new OrderItemRequest(itemId, 1))), null);

        mockMvc.perform(put("/store/orders/" + order.id() + "/status").header("Authorization", bearer(intruder.token()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/store/orders/" + order.id() + "/status").header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PREPARING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PREPARING"));

        mockMvc.perform(get("/store/orders").header("Authorization", bearer(intruder.token())))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void ownerUpdatesStoreProfileButNotSlug() throws Exception {
        var store = TestFixtures.store(authService, restaurantService, "Perfil Loja");

        mockMvc.perform(put("/store/restaurant").header("Authorization", bearer(store.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Perfil Renomeado","slug":"invadir","active":false,"description":"x","cuisine":"Pizza",
                                 "address":"Rua","phone":"+5511999990000","whatsappNumber":"+5511988880000"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Perfil Renomeado"))
                .andExpect(jsonPath("$.slug").value(store.slug()))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void sortParameterCannotReachInternalFields() throws Exception {
        var store = TestFixtures.store(authService, restaurantService, "Sort Loja");

        mockMvc.perform(get("/store/orders?sort=restaurant.owner.passwordHash,asc")
                        .header("Authorization", bearer(store.token())))
                .andExpect(status().isOk());
    }
}
