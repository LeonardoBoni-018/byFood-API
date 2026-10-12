package com.byfood.api.repository;

import com.byfood.api.model.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    Page<MenuItem> findByRestaurantIdAndAvailableTrueOrderByCategoryAscNameAsc(Long restaurantId, Pageable pageable);

    Page<MenuItem> findByRestaurantIdOrderByCategoryAscNameAsc(Long restaurantId, Pageable pageable);

    Optional<MenuItem> findByIdAndRestaurantId(Long id, Long restaurantId);

    /** Só devolve itens da loja informada: impede misturar pratos de lojas diferentes em um pedido. */
    List<MenuItem> findAllByRestaurantIdAndIdIn(Long restaurantId, Collection<Long> ids);
}
