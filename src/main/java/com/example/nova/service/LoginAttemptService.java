package com.example.nova.service;

import com.example.nova.config.SecurityProperties;
import com.example.nova.entity.User;
import com.example.nova.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Failed-login bookkeeping has to live in its own transaction. AuthService.login()
 * always throws immediately after a failed attempt, and Spring's default
 * rollback policy reverts the *entire* enclosing transaction on an unchecked
 * exception - so a save() made right before that throw would otherwise be
 * silently undone, and the account would never actually reach the lockout
 * threshold no matter how many bad passwords were submitted. REQUIRES_NEW
 * commits this update independently, before login() goes on to throw.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UserRepository userRepository;
    private final SecurityProperties securityProperties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailedAttempt(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();

        int maxAttempts = securityProperties.getAccountLock().getMaxFailedAttempts();
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        if (attempts >= maxAttempts) {
            user.setAccountNonLocked(false);
            user.setLockTime(Instant.now());
            log.warn("Account '{}' locked after {} failed login attempts", user.getUsername(), attempts);
        }
        userRepository.save(user);
    }
}
