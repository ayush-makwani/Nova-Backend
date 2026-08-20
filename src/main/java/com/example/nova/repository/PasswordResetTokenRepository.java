package com.example.nova.repository;

import com.example.nova.entity.PasswordResetToken;
import com.example.nova.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    /** Invalidates any still-usable tokens from earlier forgot-password requests, so only the newest link works. */
    @Modifying
    @Transactional
    @Query("update PasswordResetToken t set t.used = true where t.user = :user and t.used = false")
    int invalidateAllUnusedForUser(User user);
}
