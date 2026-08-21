package com.example.nova.service;

import com.example.nova.entity.AwsCredential;
import com.example.nova.exception.DocumentDeletionFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.time.Duration;

/**
 * Talks to the app's configured S3 bucket (see AwsCredential) to delete
 * project documents. A fresh client is built per call rather than cached -
 * credentials are a single admin-configurable DB row that can be rotated at
 * any time, and deletions are infrequent enough that this isn't a hot path.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3DocumentService {

    private final AwsCredentialEncryptionService encryptionService;

    public void deleteDocument(AwsCredential credential, String documentKey) {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                credential.getAccessKey(), encryptionService.decrypt(credential.getSecretKey()));

        try (S3Client s3Client = S3Client.builder()
                .region(Region.of(credential.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofSeconds(10))
                        .build())
                .build()) {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(credential.getBucketName())
                    .key(documentKey)
                    .build());
            log.info("Deleted document '{}' from S3 bucket '{}'", documentKey, credential.getBucketName());
        } catch (SdkException e) {
            throw new DocumentDeletionFailedException("Failed to delete document '" + documentKey + "' from S3", e);
        }
    }
}
