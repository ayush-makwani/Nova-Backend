package com.example.nova.controller;

import com.example.nova.dto.AwsCredentialResponse;
import com.example.nova.service.AwsCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/aws-credentials")
@RequiredArgsConstructor
public class AwsCredentialController {

    private final AwsCredentialService awsCredentialService;

    /** Admins only - the app's S3 configuration, including the decrypted secret key. */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<AwsCredentialResponse> getCredentials() {
        return ResponseEntity.ok(awsCredentialService.getCredentials());
    }
}
