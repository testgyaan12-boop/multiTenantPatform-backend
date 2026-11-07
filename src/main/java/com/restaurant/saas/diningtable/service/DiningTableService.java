package com.restaurant.saas.diningtable.service;

import com.restaurant.saas.common.exception.ConflictException;
import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.common.tenant.TenantContext;
import com.restaurant.saas.diningtable.dto.DiningTableRequest;
import com.restaurant.saas.diningtable.dto.DiningTableResponse;
import com.restaurant.saas.diningtable.entity.DiningTable;
import com.restaurant.saas.diningtable.repository.DiningTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DiningTableService {

    private final DiningTableRepository tableRepository;

    public DiningTableService(DiningTableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    @Transactional(readOnly = true)
    public List<DiningTableResponse> list(Long branchId) {
        return tableRepository.findByTenantIdAndBranchIdOrderByTableNumberAsc(
                TenantContext.requireTenant(), branchId)
            .stream().map(DiningTableResponse::from).toList();
    }

    @Transactional
    public DiningTableResponse create(Long branchId, DiningTableRequest request) {
        Long tenantId = TenantContext.requireTenant();
        if (tableRepository.existsByBranchIdAndTableNumber(branchId, request.tableNumber())) {
            throw new ConflictException("table_number_taken", "Table number is already in use");
        }
        DiningTable table = new DiningTable();
        table.setTenantId(tenantId);
        table.setBranchId(branchId);
        apply(table, request);
        return DiningTableResponse.from(tableRepository.save(table));
    }

    @Transactional
    public DiningTableResponse update(Long id, DiningTableRequest request) {
        DiningTable table = require(id);
        apply(table, request);
        return DiningTableResponse.from(tableRepository.save(table));
    }

    @Transactional
    public DiningTableResponse rotateQr(Long id) {
        DiningTable table = require(id);
        table.setQrToken(UUID.randomUUID().toString());
        table.setQrRotatedAt(Instant.now());
        return DiningTableResponse.from(tableRepository.save(table));
    }

    @Transactional
    public void delete(Long id) {
        DiningTable table = require(id);
        table.softDelete();
        tableRepository.save(table);
    }

    private DiningTable require(Long id) {
        return tableRepository.findByIdAndTenantId(id, TenantContext.requireTenant())
            .orElseThrow(() -> new NotFoundException("Dining table", id));
    }

    private static void apply(DiningTable table, DiningTableRequest request) {
        table.setTableNumber(request.tableNumber());
        table.setName(request.name());
        if (request.capacity() != null) {
            table.setCapacity(request.capacity());
        }
        table.setSectionId(request.sectionId());
    }
}
