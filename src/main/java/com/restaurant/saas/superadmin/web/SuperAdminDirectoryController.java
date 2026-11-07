package com.restaurant.saas.superadmin.web;

import com.restaurant.saas.common.web.PageResponse;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.InviteResponse;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.RolePermissionsRequest;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.RoleView;
import com.restaurant.saas.superadmin.service.SuperAdminPlatformService.UserView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
}
