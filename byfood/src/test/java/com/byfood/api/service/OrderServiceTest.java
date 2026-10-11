package com.byfood.api.service;

import com.byfood.api.TestcontainersConfiguration;
import com.byfood.api.dto.MenuItemRequest;
import com.byfood.api.dto.OrderItemRequest;
import com.byfood.api.dto.OrderRequest;
import com.byfood.api.dto.OrderResponse;
import com.byfood.api.exception.ConflictException;
import com.byfood.api.exception.NotFoundException;
import com.byfood.api.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
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

    private Long createMenuItem(String name, BigDecimal price) {
        return menuItemService.createMenuItem(new MenuItemRequest(
                name, "desc", price, "Main", null, true)).id();
    }

    private OrderResponse createOrder(Long itemId, int quantity) {
        return orderService.createOrder(new OrderRequest(
                "John Doe", "+5511999998888", "Rua A, 10",
                List.of(new OrderItemRequest(itemId, quantity))));
    }

    @Test
    void shouldCreateOrderWithComputedTotal() {
        Long itemId = createMenuItem("Burger", new BigDecimal("25.00"));

        OrderResponse created = createOrder(itemId, 2);

        assertThat(created.id()).isNotNull();
        assertThat(created.trackingCode()).isNotNull();
        assertThat(created.status()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(created.total()).isEqualByComparingTo("50.00");
        assertThat(created.items()).hasSize(1);
        assertThat(created.items().get(0).itemName()).isEqualTo("Burger");
    }

    @Test
    void shouldComputeTotalWithMultipleItems() {
        Long burger = createMenuItem("Burger", new BigDecimal("25.00"));
        Long soda = createMenuItem("Soda", new BigDecimal("6.50"));

        OrderResponse created = orderService.createOrder(new OrderRequest(
                "John Doe", "+5511999998888", "Rua A, 10",
                List.of(new OrderItemRequest(burger, 2), new OrderItemRequest(soda, 3))));

        assertThat(created.total()).isEqualByComparingTo("69.50");
        assertThat(created.items()).hasSize(2);
    }

    @Test
    void shouldReadCreatedOrder() {
        Long itemId = createMenuItem("Pizza", new BigDecimal("39.90"));

        OrderResponse created = orderService.createOrder(new OrderRequest(
                "Jane Doe", "+5511999997777", "Rua B, 20",
                List.of(new OrderItemRequest(itemId, 1))));

        OrderResponse found = orderService.getOrder(created.id());

        assertThat(found.customerName()).isEqualTo("Jane Doe");
        assertThat(found.total()).isEqualByComparingTo("39.90");
    }

    @Test
    void shouldFindOrderByTrackingCode() {
        Long itemId = createMenuItem("Pizza", new BigDecimal("39.90"));
        OrderResponse created = createOrder(itemId, 1);

        OrderResponse found = orderService.getOrderByTrackingCode(created.trackingCode());

        assertThat(found.id()).isEqualTo(created.id());
    }

    @Test
    void shouldThrowNotFoundForUnknownTrackingCode() {
        assertThatThrownBy(() -> orderService.getOrderByTrackingCode(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldThrowNotFoundForUnknownOrder() {
        assertThatThrownBy(() -> orderService.getOrder(9999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldThrowNotFoundForUnknownMenuItem() {
        OrderRequest request = new OrderRequest(
                "John Doe", "+5511999998888", "Rua A, 10",
                List.of(new OrderItemRequest(9999L, 1)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldRejectUnavailableMenuItem() {
        Long itemId = createMenuItem("Old item", new BigDecimal("10.00"));
        menuItemService.deleteMenuItem(itemId);

        assertThatThrownBy(() -> createOrder(itemId, 1))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("não está disponível");
    }

    @Test
    void shouldUpdateOrderStatus() {
        Long itemId = createMenuItem("Soda", new BigDecimal("6.00"));
        OrderResponse created = createOrder(itemId, 1);

        OrderResponse updated = orderService.updateOrderStatus(created.id(), OrderStatus.PREPARING);

        assertThat(updated.status()).isEqualTo(OrderStatus.PREPARING);
    }

    @Test
    void shouldNotChangeStatusOfFinishedOrder() {
        Long itemId = createMenuItem("Soda", new BigDecimal("6.00"));
        OrderResponse created = createOrder(itemId, 1);
        orderService.updateOrderStatus(created.id(), OrderStatus.CANCELLED);

        assertThatThrownBy(() -> orderService.updateOrderStatus(created.id(), OrderStatus.PREPARING))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldAllowRepeatingCurrentStatusOfFinishedOrder() {
        Long itemId = createMenuItem("Soda", new BigDecimal("6.00"));
        OrderResponse created = createOrder(itemId, 1);
        orderService.updateOrderStatus(created.id(), OrderStatus.DELIVERED);

        OrderResponse again = orderService.updateOrderStatus(created.id(), OrderStatus.DELIVERED);

        assertThat(again.status()).isEqualTo(OrderStatus.DELIVERED);
    }
}
