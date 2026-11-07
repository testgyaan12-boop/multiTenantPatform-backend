package com.restaurant.saas.storage.service;

import com.restaurant.saas.storage.entity.StoredFile;
import com.restaurant.saas.storage.repository.StoredFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StorageService {

    private final StoredFileRepository fileRepository;
    private final String publicBaseUrl;

    public StorageService(
        StoredFileRepository fileRepository,
        @Value("${app.storage.public-base-url}") String publicBaseUrl) {
        this.fileRepository = fileRepository;
        this.publicBaseUrl = publicBaseUrl;
    }

    public record PresignResult(String uploadUrl, String publicUrl, String storageKey) {
    }

    public PresignResult presignUpload(Long tenantId, String fileName, String contentType) {
        String safeName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = "tenants/" + (tenantId == null ? "platform" : tenantId)
            + "/" + UUID.randomUUID() + "-" + safeName;

        StoredFile file = new StoredFile();
        file.setTenantId(tenantId);
        file.setStorageKey(key);
        file.setOriginalName(fileName);
        file.setContentType(contentType);
        file.setSizeBytes(0L);
        file.setPublicUrl(publicBaseUrl + "/" + key);
        fileRepository.save(file);

        return new PresignResult(
            publicBaseUrl + "/" + key,
            file.getPublicUrl(),
            key
        );
    }
}
