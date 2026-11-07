package com.restaurant.saas.restaurant.web;

import com.restaurant.saas.common.exception.ApiException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurant")
public class RestaurantLogoController {

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/png", "image/jpeg", "image/webp", "image/gif", "image/svg+xml");
    private static final Set<String> EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "gif", "svg");
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final Path uploadDir;

    public RestaurantLogoController(@Value("${app.storage.local-dir:uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(uploadDir.resolve("logos"));
        Files.createDirectories(uploadDir.resolve("covers"));
        Files.createDirectories(uploadDir.resolve("gallery"));
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
        String fileName = UUID.randomUUID() + "." + ext;
        Path target = uploadDir.resolve(subDir).resolve(fileName);
        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/uploads/")
            .path(subDir)
            .path("/")
            .path(fileName)
            .toUriString();
    }
}
