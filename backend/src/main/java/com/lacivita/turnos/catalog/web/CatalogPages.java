package com.lacivita.turnos.catalog.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Paginación de los listados del catálogo. */
final class CatalogPages {

    static final String DEFAULT_SIZE = "50";
    private static final int MAX_SIZE = 100;

    private CatalogPages() {}

    static Pageable of(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE), sort);
    }
}
