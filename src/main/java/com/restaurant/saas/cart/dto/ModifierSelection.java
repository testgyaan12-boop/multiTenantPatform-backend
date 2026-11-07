package com.restaurant.saas.cart.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

public record ModifierSelection(Long modifierId, String name, BigDecimal priceDelta) {

    private static final TypeReference<List<ModifierSelection>> LIST_TYPE = new TypeReference<>() {
    };

    public static String write(ObjectMapper mapper, List<ModifierSelection> selections) {
        try {
            return mapper.writeValueAsString(selections);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize modifier selections", e);
        }
    }

    public static List<ModifierSelection> read(ObjectMapper mapper, String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(json, LIST_TYPE);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
