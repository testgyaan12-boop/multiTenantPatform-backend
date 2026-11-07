package com.restaurant.saas.diningtable.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiningTableRequest(
    @NotBlank @Size(max = 20) String tableNumber,
    @Size(max = 80) String name,
    @Min(1) Integer capacity,
    Long sectionId
) {
}
