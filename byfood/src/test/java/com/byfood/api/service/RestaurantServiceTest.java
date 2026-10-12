package com.byfood.api.service;

import com.byfood.api.TestFixtures;
import com.byfood.api.TestcontainersConfiguration;
import com.byfood.api.dto.RestaurantResponse;
import com.byfood.api.dto.StoreProfileRequest;
import com.byfood.api.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RestaurantServiceTest {

    @Autowired
    private RestaurantService service;

    @Autowired
    private AuthService authService;

    @Test
    void shouldKeepLegacyRestaurantAsFirstStore() {
        RestaurantResponse legacy = service.getPublic("bistro-byfood");

        assertThat(legacy.name()).isEqualTo("My Restaurant");
        assertThat(legacy.active()).isTrue();
    }

    @Test
    void shouldGenerateUniqueSlugsFromStoreName() {
        var first = TestFixtures.store(authService, service, "Pizzaria do Zé");
        var second = TestFixtures.store(authService, service, "Pizzaria do Zé");

        assertThat(first.slug()).isEqualTo("pizzaria-do-ze");
        assertThat(second.slug()).startsWith("pizzaria-do-ze-").isNotEqualTo(first.slug());
    }

    @Test
    void shouldUpdateOnlyOwnStoreAndKeepSlug() {
        var store = TestFixtures.store(authService, service, "Cantina Nova");

        RestaurantResponse updated = service.updateOwned(store.ownerId(), new StoreProfileRequest(
                "Cantina Renomeada", "Nova descrição", "Italiana", "Rua B, 2",
                "+5511911112222", "+5511933334444", "9h às 18h", "https://img.example.com/x.jpg"));

        assertThat(updated.name()).isEqualTo("Cantina Renomeada");
        assertThat(updated.cuisine()).isEqualTo("Italiana");
        assertThat(updated.slug()).isEqualTo(store.slug());
    }

    @Test
    void shouldHideInactiveStoreFromPublic() {
        assertThatThrownBy(() -> service.getPublic("loja-que-nao-existe"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldSearchByNameCuisineAndTreatWildcardsLiterally() {
        TestFixtures.store(authService, service, "Sushi Kaze");

        assertThat(service.search("kaze", "", PageRequest.of(0, 10)).getContent())
                .extracting(r -> r.name()).contains("Sushi Kaze");
        assertThat(service.search("%", "", PageRequest.of(0, 10)).getContent()).isEmpty();
        assertThat(service.cuisines()).contains("Pizza", "Variada");
    }

    @Test
    void shouldSlugifyAccentsAndSymbols() {
        assertThat(RestaurantService.slugify("  Açaí & Cia!! ")).isEqualTo("acai-cia");
        assertThat(RestaurantService.slugify("???")).isEqualTo("loja");
    }
}
