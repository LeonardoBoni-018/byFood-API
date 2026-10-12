package com.byfood.api.repository;

import com.byfood.api.TestFixtures;
import com.byfood.api.TestcontainersConfiguration;
import com.byfood.api.model.MenuItem;
import com.byfood.api.model.Restaurant;
import com.byfood.api.service.AuthService;
import com.byfood.api.service.RestaurantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MenuItemRepositoryTest {

    @Autowired
    private MenuItemRepository repository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private RestaurantService restaurantService;

    @Test
    void shouldSaveAndReadMenuItemScopedByRestaurant() {
        var store = TestFixtures.store(authService, restaurantService, "Repo Store");
        Restaurant restaurant = restaurantRepository.findByOwnerId(store.ownerId()).orElseThrow();

        MenuItem saved = repository.save(MenuItem.builder()
                .restaurant(restaurant).name("Burger").description("Beef burger")
                .price(new BigDecimal("19.90")).category("Main").available(true).build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(repository.findByIdAndRestaurantId(saved.getId(), restaurant.getId())).isPresent();
        assertThat(repository.findByIdAndRestaurantId(saved.getId(), restaurant.getId() + 9999)).isEmpty();
        assertThat(repository.findByRestaurantIdAndAvailableTrueOrderByCategoryAscNameAsc(
                restaurant.getId(), PageRequest.of(0, 10))).extracting(MenuItem::getName).containsExactly("Burger");
    }

    @Test
    void shouldIgnoreItemIdsFromOtherRestaurants() {
        var a = TestFixtures.store(authService, restaurantService, "Repo A");
        var b = TestFixtures.store(authService, restaurantService, "Repo B");
        Restaurant restaurantA = restaurantRepository.findByOwnerId(a.ownerId()).orElseThrow();
        Restaurant restaurantB = restaurantRepository.findByOwnerId(b.ownerId()).orElseThrow();
        MenuItem item = repository.save(MenuItem.builder().restaurant(restaurantA).name("X")
                .price(BigDecimal.ONE).category("Main").build());

        assertThat(repository.findAllByRestaurantIdAndIdIn(restaurantB.getId(), List.of(item.getId()))).isEmpty();
        assertThat(repository.findAllByRestaurantIdAndIdIn(restaurantA.getId(), List.of(item.getId()))).hasSize(1);
    }
}
