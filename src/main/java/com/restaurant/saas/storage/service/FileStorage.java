package com.restaurant.saas.storage.service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Pluggable file storage. Each implementation is one provider (LOCAL or CLOUDINARY).
 */
public interface FileStorage {

    String provider();

    UploadResult upload(InputStream input, String originalName, String contentType, long sizeBytes,
                        String subDir, Long tenantId) throws IOException;

    record UploadResult(String url, String storageKey, String cloudinaryAccountId) {
    }
}
