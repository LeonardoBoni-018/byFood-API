package com.byfood.api.service;

import com.byfood.api.dto.MenuItemRequest;
import com.byfood.api.dto.MenuItemResponse;
import com.byfood.api.exception.NotFoundException;
import com.byfood.api.mapper.MenuItemMapper;
import com.byfood.api.model.MenuItem;
import com.byfood.api.repository.MenuItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;

    public MenuItemService(MenuItemRepository repository) {
        this.menuItemRepository = repository;
    }

    @Transactional(readOnly = true)
    public Page<MenuItemResponse> getAvailableMenu(Pageable pageable) {
        return menuItemRepository.findByAvailableTrueOrderByCategoryAscNameAsc(pageable)
                .map(MenuItemMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<MenuItemResponse> getAllMenu(Pageable pageable) {
        return menuItemRepository.findAllByOrderByCategoryAscNameAsc(pageable)
                .map(MenuItemMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public MenuItemResponse getMenuItem(Long id) {
        return menuItemRepository.findById(id)
                .map(MenuItemMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Item do cardápio não encontrado"));
    }

    @Transactional
    public MenuItemResponse createMenuItem(MenuItemRequest request) {
        MenuItem item = MenuItemMapper.toEntity(request);
        return MenuItemMapper.toResponse(menuItemRepository.save(item));
    }

    @Transactional
    public MenuItemResponse updateMenuItem(Long id, MenuItemRequest request) {
        MenuItem existing = menuItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item do cardápio não encontrado"));
        MenuItemMapper.toEntity(request, existing);
        return MenuItemMapper.toResponse(menuItemRepository.save(existing));
    }

    @Transactional
    public void deleteMenuItem(Long id) {
        MenuItem existing = menuItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item do cardápio não encontrado"));
        existing.setAvailable(false);
        menuItemRepository.save(existing);
    }
}