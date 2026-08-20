package com.example.nova.service;

import com.example.nova.dto.AwsCredentialResponse;
import com.example.nova.entity.AwsCredential;
import com.example.nova.exception.AwsCredentialNotFoundException;
import com.example.nova.repository.AwsCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AwsCredentialService {

    private final AwsCredentialRepository awsCredentialRepository;
    private final AwsCredentialEncryptionService encryptionService;

    @Transactional(readOnly = true)
    public AwsCredentialResponse getCredentials() {
        AwsCredential credential = awsCredentialRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new AwsCredentialNotFoundException("AWS credentials are not configured yet"));

        return AwsCredentialResponse.builder()
                .id(credential.getId())
                .accessKey(credential.getAccessKey())
                .secretKey(credential.getSecretKey())
//                .secretKey(encryptionService.decrypt(credential.getSecretKey()))
                .region(credential.getRegion())
                .bucketName(credential.getBucketName())
                .url(credential.getUrl())
                .createdAt(credential.getCreatedAt())
                .updatedAt(credential.getUpdatedAt())
                .build();
    }
}
