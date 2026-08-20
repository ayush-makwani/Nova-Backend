package com.example.nova.service;

import com.example.nova.dto.AddTeamUserRequest;
import com.example.nova.dto.TeamUserResponse;
import com.example.nova.entity.Companion;
import com.example.nova.entity.Role;
import com.example.nova.entity.User;
import com.example.nova.exception.CompanionNotFoundException;
import com.example.nova.exception.TeamUserNotFoundException;
import com.example.nova.exception.UserAlreadyExistsException;
import com.example.nova.repository.CompanionRepository;
import com.example.nova.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamUserService {

    private final UserRepository userRepository;
    private final CompanionRepository companionRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsernameGenerator usernameGenerator;
    private final EmailService emailService;

    /**
     * Adds a new user to the calling admin's own company workspace. The admin
     * sets the temp password directly (per the "Team Users" screen); it's
     * emailed to the new user along with a login link. Companion assignment
     * isn't part of this form - it happens separately, one companion per user.
     */
    @Transactional
    public TeamUserResponse addTeamUser(User admin, AddTeamUserRequest request) {
        requireCompanyAdmin(admin);
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        User teamUser = User.builder()
                .username(usernameGenerator.generateUniqueUsername(request.getEmail()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getTempPassword()))
                .roles(Set.of(Role.ROLE_USER))
                .company(admin.getCompany())
                .enabled(true)
                .accountNonLocked(true)
                .mfaEnabled(false)
                .build();
        userRepository.save(teamUser);

        emailService.sendTeamUserWelcomeEmail(teamUser.getEmail(), teamUser.getFullName(), request.getTempPassword());

        log.info("Admin '{}' added team user '{}' to company '{}'",
                admin.getUsername(), teamUser.getUsername(), admin.getCompany().getName());

        // Brand-new user, never assigned a companion yet.
        return toResponse(teamUser, admin, null);
    }

    /** Every member of the calling admin's company, with whichever companion (if any) each is paired with. */
    @Transactional(readOnly = true)
    public List<TeamUserResponse> listTeamUsers(User admin) {
        requireCompanyAdmin(admin);
        return userRepository.findAllByCompanyOrderByCreatedAtAsc(admin.getCompany()).stream()
                .map(teamUser -> toResponse(teamUser, admin, companionRepository.findByAssignedUser(teamUser).orElse(null)))
                .collect(Collectors.toList());
    }

    /**
     * Pairs a company-owned, currently-unassigned companion with a team member
     * (the "Team Users" dropdown). Per the screen's own copy: each user holds
     * at most one companion, so (a) if this user already held a different one,
     * it's freed back to the pool, and (b) if the target companion was already
     * held by someone else, overwriting its assignedUser is exactly what
     * "unassigns it from the previous holder" means - a companion can only
     * ever point at one user at a time.
     */
    @Transactional
    public TeamUserResponse assignCompanion(User admin, Long teamUserId, Long companionId) {
        requireCompanyAdmin(admin);

        User teamUser = userRepository.findByIdAndCompany(teamUserId, admin.getCompany())
                .orElseThrow(() -> new TeamUserNotFoundException("User not found"));

        Companion companion = companionRepository.findByIdAndUser_Company(companionId, admin.getCompany())
                .orElseThrow(() -> new CompanionNotFoundException("Companion not found"));

        companionRepository.findByAssignedUser(teamUser)
                .filter(current -> !current.getId().equals(companion.getId()))
                .ifPresent(current -> {
                    current.setAssignedUser(null);
                    companionRepository.save(current);
                });

        companion.setAssignedUser(teamUser);
        companionRepository.save(companion);

        log.info("Admin '{}' assigned companion '{}' to user '{}'",
                admin.getUsername(), companion.getName(), teamUser.getUsername());

        return toResponse(teamUser, admin, companion);
    }

    private void requireCompanyAdmin(User admin) {
        if (admin.getCompany() == null) {
            // ROLE_ADMIN is only ever granted via company signup, so this
            // shouldn't be reachable - defensive guard, not a real user path.
            throw new AccessDeniedException("Only company admins can manage team users");
        }
    }

    private TeamUserResponse toResponse(User teamUser, User admin, Companion assignedCompanion) {
        return TeamUserResponse.builder()
                .id(teamUser.getId())
                .username(teamUser.getUsername())
                .fullName(teamUser.getFullName())
                .email(teamUser.getEmail())
                .roles(teamUser.getRoles())
                .currentUser(teamUser.getId().equals(admin.getId()))
                .companionId(assignedCompanion != null ? assignedCompanion.getId() : null)
                .companionName(assignedCompanion != null ? assignedCompanion.getName() : null)
                .companionEmail(assignedCompanion != null ? assignedCompanion.getEmail() : null)
                .createdAt(teamUser.getCreatedAt())
                .build();
    }
}
