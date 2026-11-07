package com.restaurant.saas.branch.service;

import com.restaurant.saas.branch.dto.BranchRequest;
import com.restaurant.saas.branch.dto.BranchResponse;
import com.restaurant.saas.branch.entity.Branch;
import com.restaurant.saas.branch.repository.BranchRepository;
import com.restaurant.saas.common.exception.ApiException;
import com.restaurant.saas.common.exception.ConflictException;
import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.common.tenant.TenantContext;
import com.restaurant.saas.employee.entity.Employee;
import com.restaurant.saas.employee.repository.EmployeeRepository;
import com.restaurant.saas.identity.entity.User;
import com.restaurant.saas.identity.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BranchService {

    private static final Set<String> VALID_STATUSES = Set.of("ACTIVE", "PAUSED", "INACTIVE");

    private final BranchRepository branchRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public BranchService(
        BranchRepository branchRepository,
        EmployeeRepository employeeRepository,
        UserRepository userRepository) {
        this.branchRepository = branchRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> list() {
        List<Branch> branches = branchRepository.findByTenantIdOrderByNameAsc(TenantContext.requireTenant());
        return resolve(branches);
    }

    @Transactional(readOnly = true)
    public BranchResponse get(Long id) {
        return resolve(List.of(require(id))).get(0);
    }

    @Transactional
    public BranchResponse create(BranchRequest request) {
        Long tenantId = TenantContext.requireTenant();
        if (branchRepository.existsByTenantIdAndCode(tenantId, request.code())) {
            throw new ConflictException("branch_code_taken", "Branch code is already in use");
        }
        Branch branch = new Branch();
        branch.setTenantId(tenantId);
        branch.setRestaurantId(request.restaurantId());
        apply(branch, request, tenantId);
        return resolve(List.of(branchRepository.save(branch))).get(0);
    }

    @Transactional
    public BranchResponse update(Long id, BranchRequest request) {
        Branch branch = require(id);
        apply(branch, request, branch.getTenantId());
        return resolve(List.of(branchRepository.save(branch))).get(0);
    }

    @Transactional
    public void delete(Long id) {
        Branch branch = require(id);
        branch.softDelete();
        branchRepository.save(branch);
    }

    @Transactional
    public BranchResponse updateStatus(Long id, String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!VALID_STATUSES.contains(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_status", "Status must be ACTIVE, PAUSED or INACTIVE");
        }
        Branch branch = require(id);
        if (Boolean.TRUE.equals(branch.getIsDefault()) && !"ACTIVE".equals(normalized)) {
            throw new ConflictException("default_branch_status", "The default branch cannot be paused or deactivated");
        }
        branch.setStatus(normalized);
        return resolve(List.of(branchRepository.save(branch))).get(0);
    }

    private Branch require(Long id) {
        return branchRepository.findByIdAndTenantId(id, TenantContext.requireTenant())
            .orElseThrow(() -> new NotFoundException("Branch", id));
    }

    private List<BranchResponse> resolve(List<Branch> branches) {
        Set<Long> managerIds = branches.stream()
            .map(Branch::getManagerId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (managerIds.isEmpty()) {
            return branches.stream().map(branch -> BranchResponse.from(branch, null)).toList();
        }
        Map<Long, Employee> employees = employeeRepository.findAllById(managerIds).stream()
            .collect(Collectors.toMap(Employee::getId, employee -> employee, (a, b) -> a));
        Map<Long, String> names = employees.values().stream()
            .filter(employee -> employee.getUserId() != null)
            .collect(Collectors.toMap(
                Employee::getUserId,
                employee -> userRepository.findById(employee.getUserId())
                    .map(User::fullName).orElse(employee.getEmployeeCode()),
                (a, b) -> a));
        return branches.stream()
            .map(branch -> {
                String managerName = null;
                if (branch.getManagerId() != null) {
                    Employee employee = employees.get(branch.getManagerId());
                    if (employee != null) {
                        managerName = employee.getUserId() == null
                            ? employee.getEmployeeCode()
                            : names.getOrDefault(employee.getUserId(), employee.getEmployeeCode());
                    }
                }
                return BranchResponse.from(branch, managerName);
            })
            .toList();
    }

    private void apply(Branch branch, BranchRequest request, Long tenantId) {
        branch.setCode(request.code());
        branch.setName(request.name());
        branch.setAddressLine1(request.addressLine1());
        branch.setAddressLine2(request.addressLine2());
        branch.setCity(request.city());
        branch.setState(request.state());
        branch.setPostalCode(request.postalCode());
        if (request.country() != null) {
            branch.setCountry(request.country());
        }
        branch.setPhone(request.phone());
        branch.setEmail(request.email());
        if (request.isDefault() != null) {
            branch.setIsDefault(request.isDefault());
        }
        if (request.status() != null && !request.status().isBlank()) {
            String normalized = request.status().trim().toUpperCase();
            if (VALID_STATUSES.contains(normalized)) {
                branch.setStatus(normalized);
            }
        }
        if (request.timezone() != null && !request.timezone().isBlank()) {
            branch.setTimezone(request.timezone().trim());
        }
        if (request.openingHours() != null) {
            branch.setOpeningHours(request.openingHours().isBlank() ? null : request.openingHours().trim());
        }
        if (request.managerId() != null) {
            Employee employee = employeeRepository.findByIdAndTenantId(request.managerId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Employee", request.managerId()));
            branch.setManagerId(employee.getId());
        } else {
            branch.setManagerId(null);
        }
        if (request.currency() != null && !request.currency().isBlank()) {
            branch.setCurrency(request.currency().trim().toUpperCase());
        } else {
            branch.setCurrency(null);
        }
    }
}
