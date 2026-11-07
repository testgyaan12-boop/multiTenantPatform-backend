package com.restaurant.saas.superadmin.web;

import com.restaurant.saas.common.web.PageResponse;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.InviteResponse;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.RolePermissionsRequest;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.RoleRequest;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.RoleView;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.UserView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/superadmin")
public class SuperAdminDirectoryController {

    private final SuperAdminPlatformService platformService;

    public SuperAdminDirectoryController(SuperAdminPlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping("/users")
    public PageResponse<UserView> users(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long tenantId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return platformService.listUsers(search, status, tenantId, page, size);
    }

    @PostMapping("/users/{id}/suspend")
    public UserView suspend(@PathVariable Long id) {
        return platformService.updateUserStatus(id, "SUSPENDED");
    }

    @PostMapping("/users/{id}/activate")
    public UserView activate(@PathVariable Long id) {
        return platformService.updateUserStatus(id, "ACTIVE");
    }

    @PostMapping("/users/{id}/resend-invite")
    public InviteResponse resendInvite(@PathVariable Long id) {
        return platformService.resendInvite(id);
    }

    @GetMapping("/roles")
    public List<RoleView> roles() {
        return platformService.listRoles();
    }

    @GetMapping("/permissions")
    public List<Map<String, Object>> permissions() {
        return platformService.listPermissions();
    }

    @PutMapping("/roles/{id}/permissions")
    public RoleView updateRolePermissions(@PathVariable Long id,
                                          @RequestBody RolePermissionsRequest request) {
        return platformService.updateRolePermissions(id, request);
    }

    @PostMapping("/roles")
    public RoleView createRole(@RequestBody RoleRequest request) {
        return platformService.createRole(request);
    }

    @PutMapping("/roles/{id}")
    public RoleView updateRole(@PathVariable Long id, @RequestBody RoleRequest request) {
        return platformService.updateRole(id, request);
    }

    @DeleteMapping("/roles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRole(@PathVariable Long id) {
        platformService.deleteRole(id);
    }

    @GetMapping("/roles/{id}/users")
    public List<UserView> usersInRole(@PathVariable Long id) {
        return platformService.usersInRole(id);
    }

    @PostMapping("/users/{userId}/roles/{roleCode}")
    public RoleView assignUserRole(@PathVariable Long userId, @PathVariable String roleCode) {
        return platformService.assignUserRole(userId, roleCode);
    }

    @DeleteMapping("/users/{userId}/roles/{roleCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeUserRole(@PathVariable Long userId, @PathVariable String roleCode) {
        platformService.removeUserRole(userId, roleCode);
    }
}
