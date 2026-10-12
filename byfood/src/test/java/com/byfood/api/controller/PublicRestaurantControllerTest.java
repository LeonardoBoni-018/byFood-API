package com.byfood.api.controller;

import com.byfood.api.TestFixtures;
import com.byfood.api.TestcontainersConfiguration;
import com.byfood.api.service.AuthService;
import com.byfood.api.service.MenuItemService;
import com.byfood.api.service.RestaurantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PublicRestaurantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private MenuItemService menuItemService;

    @Test
    void shouldServeLegacyStoreByItsSlug() throws Exception {
        mockMvc.perform(get("/public/restaurants/bistro-byfood"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My Restaurant"))
                .andExpect(header().string("Cache-Control", containsString("max-age=30")));
    }

    @Test
    void shouldReturnNotFoundForUnknownSlug() throws Exception {
        mockMvc.perform(get("/public/restaurants/nao-existe")).andExpect(status().isNotFound());
        mockMvc.perform(get("/public/restaurants/nao-existe/menu")).andExpect(status().isNotFound());
    }

    @Test
    void shouldListAndSearchStoresWithoutLeakingContacts() throws Exception {
        TestFixtures.store(authService, restaurantService, "Padaria Estrela");

        mockMvc.perform(get("/public/restaurants?q=estrela"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Padaria Estrela"))
                .andExpect(jsonPath("$.content[0].whatsappNumber").doesNotExist())
                .andExpect(jsonPath("$.content[0].phone").doesNotExist());
    }

    @Test
    void shouldFilterByCuisineAndListCuisines() throws Exception {
        TestFixtures.store(authService, restaurantService, "Cantina Filtro");

        mockMvc.perform(get("/public/restaurants/cuisines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("Pizza")));
        mockMvc.perform(get("/public/restaurants?cuisine=Inexistente"))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void shouldCapPageSizeAndIgnoreClientSort() throws Exception {
        mockMvc.perform(get("/public/restaurants?size=5000&sort=owner.passwordHash,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(100));
    }

    @Test
    void shouldShowOnlyOwnAvailableItemsInPublicMenu() throws Exception {
        var a = TestFixtures.store(authService, restaurantService, "Cardapio A");
        var b = TestFixtures.store(authService, restaurantService, "Cardapio B");
        TestFixtures.menuItem(menuItemService, a.ownerId(), "Só da A", "10.00");
        Long hidden = TestFixtures.menuItem(menuItemService, a.ownerId(), "Escondido", "10.00");
        menuItemService.delete(a.ownerId(), hidden);

        mockMvc.perform(get("/public/restaurants/" + a.slug() + "/menu"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Só da A"));
        mockMvc.perform(get("/public/restaurants/" + b.slug() + "/menu"))
                .andExpect(jsonPath("$.content.length()").value(0));
    }
}
