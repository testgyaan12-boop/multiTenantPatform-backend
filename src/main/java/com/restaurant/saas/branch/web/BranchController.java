package com.restaurant.saas.branch.web;

import com.restaurant.saas.branch.dto.BranchRequest;
import com.restaurant.saas.branch.dto.BranchResponse;
import com.restaurant.saas.branch.service.BranchService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('branch.read')")
    public List<BranchResponse> list() {
        return branchService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('branch.read')")
    public BranchResponse get(@PathVariable Long id) {
        return branchService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('branch.manage')")
    public ResponseEntity<BranchResponse> create(@Valid @RequestBody BranchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(branchService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('branch.manage')")
    public BranchResponse update(@PathVariable Long id, @Valid @RequestBody BranchRequest request) {
        return branchService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('branch.manage')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        branchService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('branch.manage')")
    public BranchResponse updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        return branchService.updateStatus(id, request.status());
    }

    public record StatusRequest(String status) {
    }
}
