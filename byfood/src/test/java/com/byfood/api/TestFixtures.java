package com.byfood.api;

import com.byfood.api.dto.LoginResponse;
import com.byfood.api.dto.MenuItemRequest;
import com.byfood.api.dto.RegisterCustomerRequest;
import com.byfood.api.dto.RegisterStoreRequest;
import com.byfood.api.dto.StoreProfileRequest;
import com.byfood.api.service.AuthService;
import com.byfood.api.service.MenuItemService;
import com.byfood.api.service.RestaurantService;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

/** Cria lojas e clientes únicos por teste, já que todos compartilham o mesmo banco. */
public final class TestFixtures {

    public static final String PASSWORD = "senha-forte-1";

    private static final AtomicInteger SEQ = new AtomicInteger();

    public record Store(Long ownerId, String slug, String token, String email) {
    }

    public record Customer(Long userId, String token, String email) {
    }

    private TestFixtures() {
    }

    public static StoreProfileRequest profile(String name) {
        return new StoreProfileRequest(name, "Descrição", "Pizza", "Rua A, 1",
                "+5511999990000", "+5511988880000", "11h às 23h", null);
    }

    public static Store store(AuthService auth, RestaurantService restaurants, String name) {
        String email = "dono" + SEQ.incrementAndGet() + "@teste.com";
        LoginResponse login = auth.registerStore(new RegisterStoreRequest("Dono " + name, email, PASSWORD, profile(name)));
        Long ownerId = login.user().id();
        return new Store(ownerId, restaurants.getOwned(ownerId).slug(), login.token(), email);
    }

    public static Customer customer(AuthService auth) {
        String email = "cliente" + SEQ.incrementAndGet() + "@teste.com";
        LoginResponse login = auth.registerCustomer(new RegisterCustomerRequest("Cliente", email, "+5511977776666", PASSWORD));
        return new Customer(login.user().id(), login.token(), email);
    }

    public static Long menuItem(MenuItemService menu, Long ownerId, String name, String price) {
        return menu.create(ownerId, new MenuItemRequest(name, "desc", new BigDecimal(price), "Main", null, true)).id();
    }
}
