package com.example.nova.repository;

import com.example.nova.entity.AwsCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AwsCredentialRepository extends JpaRepository<AwsCredential, Long> {
    /** Singleton config row - there's only ever meant to be one. */
    Optional<AwsCredential> findFirstByOrderByIdAsc();
}
