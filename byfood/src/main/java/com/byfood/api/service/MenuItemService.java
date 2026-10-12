package com.byfood.api.service;

import com.byfood.api.dto.MenuItemRequest;
import com.byfood.api.dto.MenuItemResponse;
import com.byfood.api.exception.NotFoundException;
import com.byfood.api.mapper.MenuItemMapper;
import com.byfood.api.model.MenuItem;
import com.byfood.api.model.Restaurant;
import com.byfood.api.repository.MenuItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantService restaurantService;

    public MenuItemService(MenuItemRepository repository, RestaurantService restaurantService) {
        this.menuItemRepository = repository;
        this.restaurantService = restaurantService;
    }

    /** Cardápio público: apenas itens disponíveis da loja ativa. */
    @Transactional(readOnly = true)
    public Page<MenuItemResponse> getPublicMenu(String slug, Pageable pageable) {
        Restaurant restaurant = restaurantService.requireActiveBySlug(slug);
        return menuItemRepository
                .findByRestaurantIdAndAvailableTrueOrderByCategoryAscNameAsc(restaurant.getId(), pageable)
                .map(MenuItemMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<MenuItemResponse> getAllForOwner(Long ownerId, Pageable pageable) {
        Restaurant restaurant = restaurantService.requireByOwner(ownerId);
        return menuItemRepository.findByRestaurantIdOrderByCategoryAscNameAsc(restaurant.getId(), pageable)
                .map(MenuItemMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public MenuItemResponse getForOwner(Long ownerId, Long id) {
        return MenuItemMapper.toResponse(requireOwned(ownerId, id));
    }

    @Transactional
    public MenuItemResponse create(Long ownerId, MenuItemRequest request) {
        Restaurant restaurant = restaurantService.requireByOwner(ownerId);
        MenuItem item = MenuItemMapper.toEntity(request, restaurant);
        return MenuItemMapper.toResponse(menuItemRepository.save(item));
    }

    @Transactional
    public MenuItemResponse update(Long ownerId, Long id, MenuItemRequest request) {
        MenuItem existing = requireOwned(ownerId, id);
        MenuItemMapper.toEntity(request, existing);
        return MenuItemMapper.toResponse(menuItemRepository.save(existing));
    }

    /** Remoção lógica: o item some do cardápio mas continua nos pedidos antigos. */
    @Transactional
    public void delete(Long ownerId, Long id) {
        MenuItem existing = requireOwned(ownerId, id);
        existing.setAvailable(false);
        menuItemRepository.save(existing);
    }

    /** Busca sempre pelo par (item, loja do dono): item de outra loja é tratado como inexistente. */
    private MenuItem requireOwned(Long ownerId, Long id) {
        Restaurant restaurant = restaurantService.requireByOwner(ownerId);
        return menuItemRepository.findByIdAndRestaurantId(id, restaurant.getId())
                .orElseThrow(() -> new NotFoundException("Item do cardápio não encontrado"));
    }
}
