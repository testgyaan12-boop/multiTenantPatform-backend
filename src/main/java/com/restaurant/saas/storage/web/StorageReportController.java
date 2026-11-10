package com.restaurant.saas.storage.web;

import com.restaurant.saas.audit.service.AuditService;
import com.restaurant.saas.authorization.security.AuthenticatedPrincipal;
import com.restaurant.saas.storage.service.StorageReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/superadmin/storage")
public class StorageReportController {

    private final StorageReportService storageReportService;
    private final AuditService auditService;

    public StorageReportController(StorageReportService storageReportService, AuditService auditService) {
        this.storageReportService = storageReportService;
        this.auditService = auditService;
    }

    @GetMapping("/overview")
    public StorageReportService.Overview overview(@RequestParam(required = false) Long tenantId) {
        return storageReportService.overview(tenantId);
    }

    @GetMapping("/growth")
    public List<StorageReportService.GrowthPoint> growth(
        @RequestParam(required = false) Long tenantId,
        @RequestParam(required = false) Integer days,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return storageReportService.growth(tenantId, days, from, to);
    }

    @GetMapping("/file-types")
    public List<StorageReportService.FileTypeStat> fileTypes(@RequestParam(required = false) Long tenantId) {
        return storageReportService.fileTypes(tenantId);
    }

    @GetMapping("/recent")
    public List<StorageReportService.FileView> recent(@RequestParam(required = false) Long tenantId,
                                                      @RequestParam(defaultValue = "8") int limit) {
        return storageReportService.recent(tenantId, limit);
    }

    @GetMapping("/files")
    public StorageReportService.PagedFiles files(
        @RequestParam(required = false) Long tenantId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String type) {
        return storageReportService.files(tenantId, page, size, search, type);
    }

    @PostMapping("/upload")
    public ResponseEntity<StorageReportService.FileView> upload(
        @RequestParam("file") MultipartFile file,
        @RequestParam(required = false) Long tenantId) throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        StorageReportService.FileView view = storageReportService.upload(
            file.getInputStream(), file.getOriginalFilename(), file.getContentType(), file.getSize(), tenantId);
        audit(tenantId, view.name());
        return ResponseEntity.ok(view);
    }

    private void audit(Long tenantId, String name) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long actorId = null;
        String actorEmail = "system";
        if (authentication != null && authentication.isAuthenticated()
            && authentication.getPrincipal() instanceof AuthenticatedPrincipal p) {
            actorId = p.userId();
            actorEmail = p.email();
        }
        auditService.record(tenantId, actorId, actorEmail, "STORAGE_FILE_UPLOADED",
            "STORED_FILE", name, "none", name);
    }
}
