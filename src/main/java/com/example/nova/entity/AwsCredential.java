package com.example.nova.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** App-wide S3 configuration (singleton row). */
@Entity
@Table(name = "aws_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AwsCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "access_key", nullable = false, length = 128)
    private String accessKey;

    // AES-GCM encrypted at rest (see service.AwsCredentialEncryptionService) - never stored in plaintext.
    @Column(name = "secret_key", nullable = false, length = 512)
    private String secretKey;

    @Column(nullable = false, length = 50)
    private String region;

    @Column(name = "bucket_name", nullable = false, length = 100)
    private String bucketName;

    @Column(length = 255)
    private String url;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
