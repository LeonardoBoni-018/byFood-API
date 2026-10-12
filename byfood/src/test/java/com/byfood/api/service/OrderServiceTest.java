package com.byfood.api.service;

import com.byfood.api.TestFixtures;
import com.byfood.api.TestcontainersConfiguration;
import com.byfood.api.config.AuthUser;
import com.byfood.api.dto.OrderItemRequest;
import com.byfood.api.dto.OrderRequest;
import com.byfood.api.dto.OrderResponse;
import com.byfood.api.exception.ConflictException;
import com.byfood.api.exception.NotFoundException;
import com.byfood.api.model.OrderStatus;
import com.byfood.api.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private MenuItemService menuItemService;

    @Autowired
    private AuthService authService;

    @Autowired
    private RestaurantService restaurantService;

    private TestFixtures.Store newStore() {
        return TestFixtures.store(authService, restaurantService, "Loja " + UUID.randomUUID());
    }

    private Long item(TestFixtures.Store store, String name, String price) {
        return TestFixtures.menuItem(menuItemService, store.ownerId(), name, price);
    }

    private OrderResponse order(TestFixtures.Store store, Long itemId, int quantity) {
        return orderService.createOrder(store.slug(), new OrderRequest("John Doe", "+5511999998888", "Rua A, 10",
                List.of(new OrderItemRequest(itemId, quantity))), null);
    }

    @Test
    void shouldCreateOrderWithComputedTotalAndStoreData() {
        var store = newStore();
        Long itemId = item(store, "Burger", "25.00");

        OrderResponse created = order(store, itemId, 2);

        assertThat(created.trackingCode()).isNotNull();
        assertThat(created.status()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(created.total()).isEqualByComparingTo("50.00");
        assertThat(created.restaurantSlug()).isEqualTo(store.slug());
        assertThat(created.whatsappLink()).startsWith("https://wa.me/5511988880000?text=");
    }

    @Test
    void shouldComputeTotalWithMultipleItems() {
        var store = newStore();
        Long burger = item(store, "Burger", "25.00");
        Long soda = item(store, "Soda", "6.50");

        OrderResponse created = orderService.createOrder(store.slug(), new OrderRequest("John", "+5511999998888", "Rua A, 10",
                List.of(new OrderItemRequest(burger, 2), new OrderItemRequest(soda, 3))), null);

        assertThat(created.total()).isEqualByComparingTo("69.50");
        assertThat(created.items()).hasSize(2);
    }

    @Test
    void shouldRejectItemFromAnotherStore() {
        var storeA = newStore();
        var storeB = newStore();
        Long itemOfB = item(storeB, "Prato de B", "10.00");

        assertThatThrownBy(() -> order(storeA, itemOfB, 1))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("nesta loja");
    }

    @Test
    void shouldRejectOrderForUnknownStore() {
        assertThatThrownBy(() -> orderService.createOrder("loja-inexistente", new OrderRequest("J", "+5511999998888", "R",
                List.of(new OrderItemRequest(1L, 1))), null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldRejectUnavailableMenuItem() {
        var store = newStore();
        Long itemId = item(store, "Old item", "10.00");
        menuItemService.delete(store.ownerId(), itemId);

        assertThatThrownBy(() -> order(store, itemId, 1))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("não está disponível");
    }

    @Test
    void shouldFindOrderByTrackingCode() {
        var store = newStore();
        OrderResponse created = order(store, item(store, "Pizza", "39.90"), 1);

        assertThat(orderService.getOrderByTrackingCode(created.trackingCode()).id()).isEqualTo(created.id());
        assertThatThrownBy(() -> orderService.getOrderByTrackingCode(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldLinkOrderToLoggedCustomerAndListOnlyTheirs() {
        var store = newStore();
        Long itemId = item(store, "Soda", "6.00");
        var customer = TestFixtures.customer(authService);
        var other = TestFixtures.customer(authService);
        var principal = new AuthUser(customer.userId(), customer.email(), UserRole.CUSTOMER);

        orderService.createOrder(store.slug(), new OrderRequest("Cliente", "+5511999998888", "Rua A, 10",
                List.of(new OrderItemRequest(itemId, 1))), principal);
        order(store, itemId, 1); // pedido de visitante

        assertThat(orderService.getOrdersForCustomer(customer.userId(), PageRequest.of(0, 10))).hasSize(1);
        assertThat(orderService.getOrdersForCustomer(other.userId(), PageRequest.of(0, 10))).isEmpty();
    }

    @Test
    void shouldIgnoreStoreTokenWhenLinkingCustomer() {
        var store = newStore();
        Long itemId = item(store, "Soda", "6.00");
        var asStore = new AuthUser(store.ownerId(), store.email(), UserRole.STORE);

        orderService.createOrder(store.slug(), new OrderRequest("Dono", "+5511999998888", "Rua A, 10",
                List.of(new OrderItemRequest(itemId, 1))), asStore);

        assertThat(orderService.getOrdersForCustomer(store.ownerId(), PageRequest.of(0, 10))).isEmpty();
    }

    @Test
    void shouldListOrdersOnlyFromOwnStoreAndFilterByStatus() {
        var a = newStore();
        var b = newStore();
        OrderResponse orderA = order(a, item(a, "A", "5.00"), 1);
        order(b, item(b, "B", "5.00"), 1);
        orderService.updateOrderStatus(a.ownerId(), orderA.id(), OrderStatus.PREPARING);

        assertThat(orderService.getOrdersForOwner(a.ownerId(), null, PageRequest.of(0, 10))).hasSize(1);
        assertThat(orderService.getOrdersForOwner(a.ownerId(), OrderStatus.PREPARING, PageRequest.of(0, 10))).hasSize(1);
        assertThat(orderService.getOrdersForOwner(a.ownerId(), OrderStatus.RECEIVED, PageRequest.of(0, 10))).isEmpty();
    }

    @Test
    void shouldNotUpdateStatusOfAnotherStoresOrder() {
        var a = newStore();
        var intruder = newStore();
        OrderResponse orderA = order(a, item(a, "A", "5.00"), 1);

        assertThatThrownBy(() -> orderService.updateOrderStatus(intruder.ownerId(), orderA.id(), OrderStatus.CANCELLED))
                .isInstanceOf(NotFoundException.class);
        assertThat(orderService.getOrderByTrackingCode(orderA.trackingCode()).status()).isEqualTo(OrderStatus.RECEIVED);
    }

    @Test
    void shouldNotChangeStatusOfFinishedOrder() {
        var store = newStore();
        OrderResponse created = order(store, item(store, "Soda", "6.00"), 1);
        orderService.updateOrderStatus(store.ownerId(), created.id(), OrderStatus.CANCELLED);

        assertThatThrownBy(() -> orderService.updateOrderStatus(store.ownerId(), created.id(), OrderStatus.PREPARING))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldAllowRepeatingCurrentStatusOfFinishedOrder() {
        var store = newStore();
        OrderResponse created = order(store, item(store, "Soda", "6.00"), 1);
        orderService.updateOrderStatus(store.ownerId(), created.id(), OrderStatus.DELIVERED);

        assertThat(orderService.updateOrderStatus(store.ownerId(), created.id(), OrderStatus.DELIVERED).status())
                .isEqualTo(OrderStatus.DELIVERED);
    }
}
