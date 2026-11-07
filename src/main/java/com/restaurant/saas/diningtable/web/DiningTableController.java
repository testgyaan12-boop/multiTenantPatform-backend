package com.restaurant.saas.diningtable.web;

import com.restaurant.saas.diningtable.dto.DiningTableRequest;
import com.restaurant.saas.diningtable.dto.DiningTableResponse;
import com.restaurant.saas.diningtable.service.DiningTableService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tables")
public class DiningTableController {

    private final DiningTableService tableService;

    public DiningTableController(DiningTableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('table.read')")
    public List<DiningTableResponse> list(@RequestParam Long branchId) {
        return tableService.list(branchId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('table.manage')")
    public ResponseEntity<DiningTableResponse> create(
        @RequestParam Long branchId,
        @Valid @RequestBody DiningTableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tableService.create(branchId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('table.manage')")
    public DiningTableResponse update(@PathVariable Long id, @Valid @RequestBody DiningTableRequest request) {
        return tableService.update(id, request);
    }

    @PostMapping("/{id}/rotate-qr")
    @PreAuthorize("hasAuthority('table.manage')")
    public DiningTableResponse rotateQr(@PathVariable Long id) {
        return tableService.rotateQr(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('table.manage')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tableService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
