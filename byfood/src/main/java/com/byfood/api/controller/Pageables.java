package com.byfood.api.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

final class Pageables {

    static final int MAX_PAGE_SIZE = 100;

    private Pageables() {
    }

    /**
     * Sanitiza a paginação vinda do cliente:
     * <ul>
     *   <li>descarta a ordenação (?sort=...): cada consulta define a sua própria, e aceitar propriedades
     *       arbitrárias permitiria ordenar por campos internos de relacionamentos;</li>
     *   <li>limita o tamanho da página a {@value #MAX_PAGE_SIZE}. A propriedade
     *       {@code spring.data.web.pageable.max-page-size} não é aplicada neste projeto (o teto real era 2000),
     *       então o limite é imposto aqui.</li>
     * </ul>
     */
    static Pageable unsorted(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
    }
}
