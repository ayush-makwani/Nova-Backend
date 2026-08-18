package com.example.nova.repository;

import com.example.nova.entity.RefreshToken;
import com.example.nova.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Transactional
    @Query("update RefreshToken r set r.revoked = true where r.user = :user")
    int revokeAllByUser(User user);

    @Modifying
    @Transactional
    void deleteAllByUser(User user);
}
