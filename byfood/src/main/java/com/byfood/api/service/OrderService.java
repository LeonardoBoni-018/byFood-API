package com.byfood.api.service;

import com.byfood.api.config.AuthUser;
import com.byfood.api.dto.OrderItemRequest;
import com.byfood.api.dto.OrderRequest;
import com.byfood.api.dto.OrderResponse;
import com.byfood.api.exception.ConflictException;
import com.byfood.api.exception.NotFoundException;
import com.byfood.api.mapper.OrderMapper;
import com.byfood.api.model.MenuItem;
import com.byfood.api.model.Order;
import com.byfood.api.model.OrderItem;
import com.byfood.api.model.OrderStatus;
import com.byfood.api.model.Restaurant;
import com.byfood.api.model.UserRole;
import com.byfood.api.repository.AppUserRepository;
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
    private final AppUserRepository userRepository;
    private final RestaurantService restaurantService;
    private final WhatsAppLinkService whatsAppLinkService;

    public OrderService(OrderRepository orderRepository, MenuItemRepository menuItemRepository,
                        AppUserRepository userRepository, RestaurantService restaurantService,
                        WhatsAppLinkService whatsAppLinkService) {
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.userRepository = userRepository;
        this.restaurantService = restaurantService;
        this.whatsAppLinkService = whatsAppLinkService;
    }

    private OrderResponse toResponse(Order order) {
        Restaurant restaurant = order.getRestaurant();
        String link = whatsAppLinkService.buildOrderLink(order, restaurant.getName(), restaurant.getWhatsappNumber());
        return OrderMapper.toResponse(order, link);
    }

    /**
     * Cria um pedido para a loja {@code slug}. Preços e nomes vêm sempre do cardápio da própria loja
     * (nunca do cliente) e itens de outras lojas são rejeitados. {@code user} é opcional: com um cliente
     * autenticado o pedido entra no histórico dele.
     */
    @Transactional
    public OrderResponse createOrder(String slug, OrderRequest request, AuthUser user) {
        Restaurant restaurant = restaurantService.requireActiveBySlug(slug);

        Order order = Order.builder()
                .restaurant(restaurant)
                .customerName(request.customerName().trim())
                .customerPhone(request.customerPhone().trim())
                .customerAddress(request.customerAddress().trim())
                .status(OrderStatus.RECEIVED)
                .total(BigDecimal.ZERO)
                .build();

        if (user != null && user.role() == UserRole.CUSTOMER) {
            order.setCustomer(userRepository.getReferenceById(user.id()));
        }

        Set<Long> ids = request.items().stream()
                .map(OrderItemRequest::menuItemId)
                .collect(Collectors.toSet());
        Map<Long, MenuItem> menuItems = menuItemRepository.findAllByRestaurantIdAndIdIn(restaurant.getId(), ids).stream()
                .collect(Collectors.toMap(MenuItem::getId, Function.identity()));

        BigDecimal total = BigDecimal.ZERO;
        for (var itemRequest : request.items()) {
            MenuItem menuItem = menuItems.get(itemRequest.menuItemId());
            if (menuItem == null) {
                throw new NotFoundException("Item do cardápio não encontrado nesta loja");
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
    public OrderResponse getOrderByTrackingCode(UUID trackingCode) {
        return orderRepository.findByTrackingCode(trackingCode)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado"));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersForOwner(Long ownerId, OrderStatus status, Pageable pageable) {
        Restaurant restaurant = restaurantService.requireByOwner(ownerId);
        Page<Order> page = status == null
                ? orderRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurant.getId(), pageable)
                : orderRepository.findByRestaurantIdAndStatusOrderByCreatedAtDesc(restaurant.getId(), status, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersForCustomer(Long customerId, Pageable pageable) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
                .map(this::toResponse);
    }

    /** Só altera pedidos da loja do dono; pedido de outra loja é tratado como inexistente. */
    @Transactional
    public OrderResponse updateOrderStatus(Long ownerId, Long orderId, OrderStatus status) {
        Restaurant restaurant = restaurantService.requireByOwner(ownerId);
        Order order = orderRepository.findByIdAndRestaurantId(orderId, restaurant.getId())
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
