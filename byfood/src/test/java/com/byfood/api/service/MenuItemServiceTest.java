package com.byfood.api.service;

import com.byfood.api.TestFixtures;
import com.byfood.api.TestcontainersConfiguration;
import com.byfood.api.dto.MenuItemRequest;
import com.byfood.api.dto.MenuItemResponse;
import com.byfood.api.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MenuItemServiceTest {

    @Autowired
    private MenuItemService service;

    @Autowired
    private AuthService authService;

    @Autowired
    private RestaurantService restaurantService;

    private MenuItemRequest sampleRequest() {
        return new MenuItemRequest("Pizza Margherita", "Tomato, mozzarella and basil",
                new BigDecimal("39.90"), "Pizza", null, true);
    }

    @Test
    void shouldCreateAndReadMenuItem() {
        var store = TestFixtures.store(authService, restaurantService, "Loja Menu");

        MenuItemResponse created = service.create(store.ownerId(), sampleRequest());

        assertThat(created.id()).isNotNull();
        assertThat(service.getForOwner(store.ownerId(), created.id()).price()).isEqualByComparingTo("39.90");
    }

    @Test
    void shouldUpdateMenuItem() {
        var store = TestFixtures.store(authService, restaurantService, "Loja Update");
        MenuItemResponse created = service.create(store.ownerId(), sampleRequest());

        MenuItemResponse updated = service.update(store.ownerId(), created.id(), new MenuItemRequest(
                "Pizza Margherita XL", "desc", new BigDecimal("49.90"), "Pizza", null, false));

        assertThat(updated.name()).isEqualTo("Pizza Margherita XL");
        assertThat(updated.price()).isEqualByComparingTo("49.90");
        assertThat(updated.available()).isFalse();
    }

    @Test
    void shouldSoftDeleteAndHideFromPublicMenu() {
        var store = TestFixtures.store(authService, restaurantService, "Loja Delete");
        MenuItemResponse created = service.create(store.ownerId(), sampleRequest());

        service.delete(store.ownerId(), created.id());

        assertThat(service.getForOwner(store.ownerId(), created.id()).available()).isFalse();
        assertThat(service.getPublicMenu(store.slug(), Pageable.unpaged())).isEmpty();
    }

    @Test
    void shouldThrowNotFoundForUnknownId() {
        var store = TestFixtures.store(authService, restaurantService, "Loja 404");

        assertThatThrownBy(() -> service.getForOwner(store.ownerId(), 999_999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldNotTouchAnotherStoresMenu() {
        var owner = TestFixtures.store(authService, restaurantService, "Dona do Prato");
        var intruder = TestFixtures.store(authService, restaurantService, "Intruso");
        Long itemId = service.create(owner.ownerId(), sampleRequest()).id();

        assertThatThrownBy(() -> service.getForOwner(intruder.ownerId(), itemId)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.update(intruder.ownerId(), itemId, sampleRequest())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete(intruder.ownerId(), itemId)).isInstanceOf(NotFoundException.class);
        assertThat(service.getForOwner(owner.ownerId(), itemId).available()).isTrue();
    }

    @Test
    void shouldListOnlyOwnItems() {
        var a = TestFixtures.store(authService, restaurantService, "Loja A Lista");
        var b = TestFixtures.store(authService, restaurantService, "Loja B Lista");
        service.create(a.ownerId(), sampleRequest());

        assertThat(service.getAllForOwner(a.ownerId(), Pageable.unpaged())).hasSize(1);
        assertThat(service.getAllForOwner(b.ownerId(), Pageable.unpaged())).isEmpty();
    }
}
