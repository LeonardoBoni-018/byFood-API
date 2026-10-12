package com.byfood.api.repository;

import com.byfood.api.model.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findBySlugAndActiveTrue(String slug);

    Optional<Restaurant> findByOwnerId(Long ownerId);

    boolean existsBySlug(String slug);

    /** Listagem pública com busca por nome/culinária. {@code q} e {@code cuisine} vazios não filtram. */
    @Query("""
            select r from Restaurant r
            where r.active = true
              and (:cuisine = '' or r.cuisine = :cuisine)
              and (:q = '' or lower(r.name) like lower(concat('%', :q, '%')) escape '\\'
                           or lower(r.cuisine) like lower(concat('%', :q, '%')) escape '\\')
            order by r.name asc
            """)
    Page<Restaurant> search(@Param("q") String q, @Param("cuisine") String cuisine, Pageable pageable);

    @Query("select distinct r.cuisine from Restaurant r where r.active = true order by r.cuisine")
    List<String> findActiveCuisines();
}
