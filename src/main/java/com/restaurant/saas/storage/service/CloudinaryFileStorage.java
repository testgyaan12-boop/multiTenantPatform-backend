package com.restaurant.saas.storage.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.restaurant.saas.storage.entity.CloudinaryAccount;
import com.restaurant.saas.common.config.StorageProperties;
import com.restaurant.saas.storage.crypto.CredentialCryptoService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

@Component
public class CloudinaryFileStorage implements FileStorage {

    private final CredentialCryptoService crypto;
    private final String defaultFolder;

    public CloudinaryFileStorage(CredentialCryptoService crypto, StorageProperties props) {
        this.crypto = crypto;
        this.defaultFolder = props.cloudinary("folder") != null ? props.cloudinary("folder") : "resto-saas";
    }

    @Override
    public String provider() {
        return "CLOUDINARY";
    }

    public FileStorage.UploadResult uploadViaAccount(CloudinaryAccount account, InputStream input, String originalName,
                                         String contentType, long sizeBytes, String subDir, Long tenantId)
        throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("empty_file");
        }
        if (originalName == null || originalName.isBlank()) {
            originalName = "file";
        }
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        String apiSecret = crypto.decrypt(account.getApiSecretEnc());

        Map<String, String> config = ObjectUtils.asMap(
            "cloud_name", account.getCloudName(),
            "api_key", account.getApiKey(),
            "api_secret", apiSecret,
            "secure", true
        );
        Cloudinary cloudinary = new Cloudinary(config);

        String folder = account.getFolderPrefix() != null && !account.getFolderPrefix().isBlank()
            ? account.getFolderPrefix()
            : defaultFolder + "/" + subDir;
        String publicIdBase = folder + "/" + (tenantId == null ? "platform" : tenantId) + "/"
            + UUID.randomUUID();

        Map<String, Object> options = ObjectUtils.asMap(
            "public_id", publicIdBase,
            "resource_type", contentType.startsWith("image/") ? "image" : "raw"
        );

        byte[] bytes = input.readAllBytes();
        Map uploadResult = cloudinary.uploader().upload(bytes, options);

        String publicId = String.valueOf(uploadResult.get("public_id"));
        String secureUrl = String.valueOf(uploadResult.get("secure_url"));

        return new FileStorage.UploadResult(secureUrl, publicId, account.getId().toString());
    }

    @Override
    public FileStorage.UploadResult upload(InputStream input, String originalName, String contentType, long sizeBytes,
                                           String subDir, Long tenantId) throws IOException {
        throw new UnsupportedOperationException("Use uploadViaAccount with a selected CloudinaryAccount");
    }
}
