package com.restaurant.saas.diningtable.dto;

import com.restaurant.saas.diningtable.entity.DiningTable;

public record DiningTableResponse(
    Long id,
    Long branchId,
    Long sectionId,
    String tableNumber,
    String name,
    Integer capacity,
    String status,
    String qrToken
) {

    public static DiningTableResponse from(DiningTable table) {
        return new DiningTableResponse(
            table.getId(),
            table.getBranchId(),
            table.getSectionId(),
            table.getTableNumber(),
            table.getName(),
            table.getCapacity(),
            table.getStatus(),
            table.getQrToken()
        );
    }
}
