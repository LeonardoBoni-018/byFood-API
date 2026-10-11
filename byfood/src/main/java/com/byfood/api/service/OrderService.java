package com.byfood.api.service;

import com.byfood.api.dto.OrderItemRequest;
import com.byfood.api.dto.OrderRequest;
import com.byfood.api.dto.OrderResponse;
import com.byfood.api.dto.RestaurantResponse;
import com.byfood.api.exception.ConflictException;
import com.byfood.api.exception.NotFoundException;
import com.byfood.api.mapper.OrderMapper;
import com.byfood.api.model.MenuItem;
import com.byfood.api.model.Order;
import com.byfood.api.model.OrderItem;
import com.byfood.api.model.OrderStatus;
import com.byfood.api.repository.MenuItemRepository;
import com.byfood.api.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Set<OrderStatus> FINAL_STATUSES = EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED);

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantService restaurantService;
    private final WhatsAppLinkService whatsAppLinkService;

    public OrderService(OrderRepository orderRepository, MenuItemRepository menuItemRepository,
                        RestaurantService restaurantService, WhatsAppLinkService whatsAppLinkService) {
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.restaurantService = restaurantService;
        this.whatsAppLinkService = whatsAppLinkService;
    }

    private OrderResponse toResponse(Order order, RestaurantResponse restaurant) {
        String link = whatsAppLinkService.buildOrderLink(order, restaurant.name(), restaurant.whatsappNumber());
        return OrderMapper.toResponse(order, link);
    }

    private OrderResponse toResponse(Order order) {
        return toResponse(order, restaurantService.getRestaurant());
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        Order order = Order.builder()
                .customerName(request.customerName().trim())
                .customerPhone(request.customerPhone().trim())
                .customerAddress(request.customerAddress().trim())
                .status(OrderStatus.RECEIVED)
                .total(BigDecimal.ZERO)
                .build();

        Set<Long> ids = request.items().stream()
                .map(OrderItemRequest::menuItemId)
                .collect(Collectors.toSet());
        Map<Long, MenuItem> menuItems = menuItemRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(MenuItem::getId, Function.identity()));

        BigDecimal total = BigDecimal.ZERO;
        for (var itemRequest : request.items()) {
            MenuItem menuItem = menuItems.get(itemRequest.menuItemId());
            if (menuItem == null) {
                throw new NotFoundException("Item do cardápio não encontrado");
            }
            if (!menuItem.isAvailable()) {
                throw new NotFoundException("Item do cardápio não está disponível: " + menuItem.getName());
            }

            BigDecimal lineTotal = menuItem.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity()));
            total = total.add(lineTotal);

            order.addItem(OrderItem.builder()
                    .itemName(menuItem.getName())
                    .unitPrice(menuItem.getPrice())
                    .quantity(itemRequest.quantity())
                    .build());
        }

        order.setTotal(total);
        return toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        return orderRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado"));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByTrackingCode(UUID trackingCode) {
        return orderRepository.findByTrackingCode(trackingCode)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado"));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        RestaurantResponse restaurant = restaurantService.getRestaurant();
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(order -> toResponse(order, restaurant));
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado"));

        if (order.getStatus() != status) {
            if (FINAL_STATUSES.contains(order.getStatus())) {
                throw new ConflictException("Pedido já finalizado não pode mudar de status");
            }
            order.setStatus(status);
            order = orderRepository.save(order);
        }
        return toResponse(order);
    }
}
