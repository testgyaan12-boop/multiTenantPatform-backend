package com.restaurant.saas.common.config;

import com.restaurant.saas.storage.crypto.CredentialCryptoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Bean
    public CredentialCryptoService credentialCryptoService(StorageProperties properties) {
        return new CredentialCryptoService(properties.cloudinary("encrypt-key"));
    }
}
