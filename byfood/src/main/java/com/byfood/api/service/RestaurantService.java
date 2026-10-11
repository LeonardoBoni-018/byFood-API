package com.byfood.api.service;

import com.byfood.api.dto.RestaurantResponse;
import com.byfood.api.exception.NotFoundException;
import com.byfood.api.mapper.RestaurantMapper;
import com.byfood.api.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantService {

    private final RestaurantRepository repository;

    public RestaurantService(RestaurantRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurant() {
        return repository.findFirstByOrderByIdAsc()
                .map(RestaurantMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Restaurante não configurado"));
    }
}