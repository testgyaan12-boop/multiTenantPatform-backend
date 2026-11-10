package com.restaurant.saas.restaurant.web;

import com.restaurant.saas.common.exception.ApiException;
import com.restaurant.saas.common.tenant.TenantContext;
import com.restaurant.saas.storage.service.FileStorage;
import com.restaurant.saas.storage.service.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/restaurant")
public class RestaurantLogoController {

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/png", "image/jpeg", "image/webp", "image/gif", "image/svg+xml");
    private static final Set<String> EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "gif", "svg");
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final StorageService storageService;

    public RestaurantLogoController(StorageService storageService) {
        this.storageService = storageService;
    }

    public record LogoUploadResponse(String url) {
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('restaurant.update')")
    public LogoUploadResponse uploadLogo(@RequestParam("file") MultipartFile file) throws IOException {
        return new LogoUploadResponse(store(file, "logos", "Logo"));
    }

    @PostMapping(value = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('restaurant.update')")
    public LogoUploadResponse uploadMedia(
        @RequestParam("file") MultipartFile file,
        @RequestParam("type") String type) throws IOException {
        String dir = switch (type == null ? "" : type.toLowerCase(Locale.ROOT)) {
            case "cover" -> "covers";
            case "gallery" -> "gallery";
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_media_type",
                "type must be cover or gallery");
        };
        return new LogoUploadResponse(store(file, dir, "Image"));
    }

    private String store(MultipartFile file, String subDir, String label) throws IOException {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "empty_file", "No file was uploaded");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_file_type",
                label + " must be a PNG, JPEG, WEBP, GIF or SVG image");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "file_too_large", label + " must be 5MB or smaller");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
            ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
            : contentType.substring(contentType.lastIndexOf('/') + 1);
        if (!EXTENSIONS.contains(ext)) {
            ext = "png";
        }

        Long tenantId = TenantContext.requireTenant();
        FileStorage.UploadResult result = storageService.upload(
            file.getInputStream(), original, contentType, file.getSize(), subDir, tenantId);
        return result.url();
    }
}
