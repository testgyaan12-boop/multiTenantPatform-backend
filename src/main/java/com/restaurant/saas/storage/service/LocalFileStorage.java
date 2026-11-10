package com.restaurant.saas.storage.service;

import com.restaurant.saas.storage.entity.StoredFile;
import com.restaurant.saas.storage.repository.StoredFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class LocalFileStorage implements FileStorage {

    private final StoredFileRepository fileRepository;
    private final Path basePath;
    private final String publicBaseUrl;

    public LocalFileStorage(StoredFileRepository fileRepository,
                            @Value("${app.storage.tidb-fs.base-path:${user.home}/tidbcloudfs}") String basePath,
                            @Value("${app.storage.public-base-url:http://localhost:8080/api/v1/storage}") String publicBaseUrl) {
        this.fileRepository = fileRepository;
        this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.endsWith("/") ? publicBaseUrl : publicBaseUrl + "/";
    }

    @Override
    public String provider() {
        return "LOCAL";
    }

    @Override
    public UploadResult upload(InputStream input, String originalName, String contentType, long sizeBytes,
                               String subDir, Long tenantId) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("empty_file");
        }
        if (originalName == null || originalName.isBlank()) {
            originalName = "file";
        }
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        Path tenantDir = basePath.resolve(subDir).resolve(String.valueOf(tenantId == null ? "platform" : tenantId));
        Files.createDirectories(tenantDir);

        String ext = originalName.contains(".")
            ? originalName.substring(originalName.lastIndexOf('.') + 1)
            : "";
        String key = subDir + "/" + (tenantId == null ? "platform" : tenantId)
            + "/" + UUID.randomUUID() + (ext.isBlank() ? "" : "." + ext);
        Path target = tenantDir.resolve(key.substring(key.lastIndexOf('/') + 1));

        try (InputStream in = input) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        StoredFile stored = new StoredFile();
        stored.setTenantId(tenantId);
        stored.setStorageKey(key);
        stored.setOriginalName(originalName);
        stored.setContentType(contentType);
        stored.setSizeBytes(sizeBytes);
        stored.setPublicUrl(publicBaseUrl + key);
        stored.setProvider(provider());
        stored.setUploadedBy(currentUserId());
        fileRepository.save(stored);

        return new FileStorage.UploadResult(stored.getPublicUrl(), key, null);
    }

    public Path resolvePath(String key) {
        return basePath.resolve(key).normalize();
    }

    private Long currentUserId() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
            || !(authentication.getPrincipal() instanceof com.restaurant.saas.authorization.security.AuthenticatedPrincipal p)) {
            return null;
        }
        return p.userId();
    }
}
