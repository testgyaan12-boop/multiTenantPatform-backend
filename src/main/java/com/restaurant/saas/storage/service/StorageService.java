package com.restaurant.saas.storage.service;

import com.restaurant.saas.authorization.security.AuthenticatedPrincipal;
import com.restaurant.saas.common.config.StorageProperties;
import com.restaurant.saas.common.exception.ApiException;
import com.restaurant.saas.storage.entity.StoredFile;
import com.restaurant.saas.storage.repository.StoredFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

@Service
public class StorageService {

    private final StorageProperties properties;
    private final CloudinaryAccountService cloudinaryAccountService;
    private final CloudinaryFileStorage cloudinaryFileStorage;
    private final LocalFileStorage localFileStorage;
    private final StoredFileRepository fileRepository;

    public StorageService(StorageProperties properties,
                          CloudinaryAccountService cloudinaryAccountService,
                          CloudinaryFileStorage cloudinaryFileStorage,
                          LocalFileStorage localFileStorage,
                          StoredFileRepository fileRepository,
                          @Value("${app.storage.public-base-url:http://localhost:8080/api/v1/storage}") String publicBaseUrl) {
        this.properties = properties;
        this.cloudinaryAccountService = cloudinaryAccountService;
        this.cloudinaryFileStorage = cloudinaryFileStorage;
        this.localFileStorage = localFileStorage;
        this.fileRepository = fileRepository;
        this.publicBaseUrl = publicBaseUrl.endsWith("/") ? publicBaseUrl : publicBaseUrl + "/";
    }

    private final String publicBaseUrl;

    public FileStorage.UploadResult upload(InputStream input, String originalName, String contentType, long sizeBytes,
                                           String subDir, Long tenantId) throws IOException {
        String provider = properties.getProvider() == null ? "cloudinary" : properties.getProvider();

        if ("cloudinary".equalsIgnoreCase(provider)) {
            var account = cloudinaryAccountService.selectAccount();
            if (account == null) {
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "storage_capacity_exhausted",
                    "All Cloudinary accounts are full or misconfigured. Add a new account in Storage settings.");
            }
            try {
                var result = cloudinaryFileStorage.uploadViaAccount(account, input, originalName,
                    contentType, sizeBytes, subDir, tenantId);
                cloudinaryAccountService.recordUpload(account.getId(), sizeBytes);
                persistStoredFile(originalName, contentType, sizeBytes, tenantId, result, account.getId());
                return result;
            } catch (ApiException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "cloudinary_upload_failed",
                    "Cloudinary upload failed: " + ex.getMessage());
            }
        }

        var result = localFileStorage.upload(input, originalName, contentType, sizeBytes, subDir, tenantId);
        persistStoredFile(originalName, contentType, sizeBytes, tenantId, result, null);
        return result;
    }

    public Path resolvePath(String key) {
        if (!"local".equalsIgnoreCase(properties.getProvider())) {
            throw new IllegalStateException("file is stored on cloudinary; use public_url directly");
        }
        return localFileStorage.resolvePath(key);
    }

    private void persistStoredFile(String originalName, String contentType, long sizeBytes, Long tenantId,
                                   FileStorage.UploadResult result, Long accountId) {
        StoredFile stored = new StoredFile();
        stored.setTenantId(tenantId);
        stored.setStorageKey(result.storageKey());
        stored.setOriginalName(originalName);
        stored.setContentType(contentType);
        stored.setSizeBytes(sizeBytes);
        stored.setProvider(result.cloudinaryAccountId() != null ? "CLOUDINARY" : "LOCAL");
        stored.setCloudinaryAccountId(accountId);
        stored.setPublicUrl(result.url());
        stored.setUploadedBy(currentUserId());
        fileRepository.save(stored);
    }

    private Long currentUserId() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
            || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal p)) {
            return null;
        }
        return p.userId();
    }
}
