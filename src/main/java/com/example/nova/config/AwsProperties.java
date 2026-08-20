package com.example.nova.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.aws")
public class AwsProperties {

    /**
     * Base64 encoded AES-256 key used to encrypt AwsCredential.secretKey at
     * rest (see service.AwsCredentialEncryptionService). Generate one with:
     * openssl rand -base64 32
     */
    private String encryptionKey;
}
