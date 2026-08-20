package com.example.nova.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class AwsCredentialResponse {
    private Long id;
    private String accessKey;
    private String secretKey;
    private String region;
    private String bucketName;
    private String url;
    private Instant createdAt;
    private Instant updatedAt;
}
