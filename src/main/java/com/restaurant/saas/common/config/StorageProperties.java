package com.restaurant.saas.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String provider = "cloudinary";
    private String endpoint;
    private String region;
    private String bucket;
    private String accessKey;
    private String secretKey;
    private String publicBaseUrl;
    private String localDir;
    private Map<String, String> cloudinary;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String getLocalDir() {
        return localDir;
    }

    public void setLocalDir(String localDir) {
        this.localDir = localDir;
    }

    public Map<String, String> getCloudinary() {
        return cloudinary;
    }

    public void setCloudinary(Map<String, String> cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String cloudinary(String key) {
        if (cloudinary == null) {
            return null;
        }
        return cloudinary.get(key);
    }
}
