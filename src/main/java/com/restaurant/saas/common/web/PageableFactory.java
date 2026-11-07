package com.restaurant.saas.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableFactory {

    private static final int MAX_SIZE = 100;

    private PageableFactory() {
    }

    public static Pageable of(int page, int size, String sort, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        String property = (sort == null || sort.isBlank()) ? "createdAt" : sort;
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(dir, property));
    }
}
