package com.restaurant.saas.common.web;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(List<T> content, PageMeta page) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages())
        );
    }

    public record PageMeta(int page, int size, long totalElements, int totalPages) {
    }
}
