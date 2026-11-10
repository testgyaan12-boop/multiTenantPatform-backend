package com.restaurant.saas.storage.web;

import com.restaurant.saas.audit.service.AuditService;
import com.restaurant.saas.authorization.security.AuthenticatedPrincipal;
import com.restaurant.saas.storage.service.CloudinaryAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/superadmin/cloudinary-accounts")
public class CloudinaryAccountController {

    private final CloudinaryAccountService accountService;
    private final AuditService auditService;

    public CloudinaryAccountController(CloudinaryAccountService accountService, AuditService auditService) {
        this.accountService = accountService;
        this.auditService = auditService;
    }

    @GetMapping
    public List<CloudinaryAccountService.AccountView> list() {
        return accountService.listAccounts();
    }

    @GetMapping("/{id}")
    public CloudinaryAccountService.AccountView get(@PathVariable Long id) {
        return accountService.getAccount(id);
    }

    @PostMapping
    public ResponseEntity<CloudinaryAccountService.AccountView> create(@RequestBody CloudinaryAccountService.AccountRequest request) {
        var before = "none";
        CloudinaryAccountService.AccountView view = accountService.createAccount(request);
        audit("CLOUDINARY_ACCOUNT_CREATED", String.valueOf(view.id()), before, view.cloudName());
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @PutMapping("/{id}")
    public CloudinaryAccountService.AccountView update(@PathVariable Long id,
                                                       @RequestBody CloudinaryAccountService.AccountRequest request) {
        CloudinaryAccountService.AccountView before = accountService.getAccount(id);
        CloudinaryAccountService.AccountView view = accountService.updateAccount(id, request);
        audit("CLOUDINARY_ACCOUNT_UPDATED", String.valueOf(view.id()), before.cloudName(), view.cloudName());
        return view;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accountService.deleteAccount(id);
        audit("CLOUDINARY_ACCOUNT_DELETED", String.valueOf(id), "deleted", "none");
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test")
    public CloudinaryAccountService.AccountView test(@PathVariable Long id) {
        return accountService.testAccount(id);
    }

    @PostMapping("/{id}/refresh-usage")
    public CloudinaryAccountService.AccountView refreshUsage(@PathVariable Long id) {
        return accountService.refreshUsage(id);
    }

    @GetMapping("/summary")
    public CloudinaryAccountService.Summary summary() {
        return accountService.summary();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorder(@RequestBody List<Map<String, Object>> items) {
        accountService.reorder(items);
        return ResponseEntity.noContent().build();
    }

    private void audit(String action, String entityId, String before, String after) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long actorId = null;
        String actorEmail = "system";
        if (authentication != null && authentication.isAuthenticated()
            && authentication.getPrincipal() instanceof AuthenticatedPrincipal p) {
            actorId = p.userId();
            actorEmail = p.email();
        }
        auditService.record(null, actorId, actorEmail, action, "CLOUDINARY_ACCOUNT", entityId, before, after);
    }
}
