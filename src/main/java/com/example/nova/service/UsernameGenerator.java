package com.example.nova.service;

import com.example.nova.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Several account-creation flows (signup, team-user invites) collect a name +
 * email but no separate username, so one is derived from the email's local
 * part and de-duplicated on collision.
 */
@Component
@RequiredArgsConstructor
public class UsernameGenerator {

    private final UserRepository userRepository;

    public String generateUniqueUsername(String email) {
        String localPart = email.substring(0, email.indexOf('@'))
                .toLowerCase()
                .replaceAll("[^a-z0-9._-]", "");
        if (localPart.isBlank()) {
            localPart = "user";
        }
        if (localPart.length() > 40) {
            localPart = localPart.substring(0, 40);
        }

        String candidate = localPart;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = localPart + suffix++;
        }
        return candidate;
    }
}
