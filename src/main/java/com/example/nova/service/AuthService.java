package com.example.nova.service;

import com.example.nova.config.SecurityProperties;
import com.example.nova.dto.*;
import com.example.nova.entity.Company;
import com.example.nova.entity.RefreshToken;
import com.example.nova.entity.Role;
import com.example.nova.entity.User;
import com.example.nova.exception.*;
import com.example.nova.repository.CompanyRepository;
import com.example.nova.repository.UserRepository;
import com.example.nova.security.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final MfaService mfaService;
    private final SecurityProperties securityProperties;
    private final CompanionService companionService;

    /** "Individual" account type: one user, one free companion, no company workspace. */
    @Transactional
    public MessageResponse signupIndividual(IndividualSignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        User user = User.builder()
                .username(generateUniqueUsername(request.getEmail()))
                .email(request.getEmail())
                .fullName(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(Role.ROLE_USER))
                .enabled(true)
                .accountNonLocked(true)
                .mfaEnabled(false)
                .build();
        userRepository.save(user);

        companionService.provisionInitialCompanion(user);

        log.info("New individual account registered: {}", user.getUsername());
        return new MessageResponse("Account created successfully. You can now log in.");
    }

    /**
     * "Company / Team" account type: creates the company workspace and its first
     * admin user. No companion is auto-provisioned here - the admin buys and
     * assigns companions afterward via POST /api/companions.
     */
    @Transactional
    public MessageResponse signupCompany(CompanySignupRequest request) {
        String domain = request.getCompanyDomain().trim().toLowerCase();
        if (companyRepository.existsByDomainIgnoreCase(domain)) {
            throw new CompanyDomainAlreadyExistsException("A company is already registered with domain '" + domain + "'");
        }
        if (userRepository.existsByEmail(request.getAdminEmail())) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        Company company = companyRepository.save(Company.builder()
                .name(request.getCompanyName().trim())
                .domain(domain)
                .build());

        User admin = User.builder()
                .username(generateUniqueUsername(request.getAdminEmail()))
                .email(request.getAdminEmail())
                .fullName(request.getAdminName())
                .password(passwordEncoder.encode(request.getAdminPassword()))
                .roles(Set.of(Role.ROLE_ADMIN))
                .company(company)
                .enabled(true)
                .accountNonLocked(true)
                .mfaEnabled(false)
                .build();
        userRepository.save(admin);

        log.info("New company workspace registered: '{}' ({}), admin '{}'", company.getName(), domain, admin.getUsername());
        return new MessageResponse("Company workspace created successfully. You can now log in.");
    }

    /**
     * The signup screens collect a name + email but no separate username, so one
     * is derived from the email's local part and de-duplicated on collision.
     */
    private String generateUniqueUsername(String email) {
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

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        checkAccountLock(user);

        if (user.getPassword() == null) {
            // SSO-provisioned account (authProvider = SAML): there is no local
            // password to check. Fail with the same generic message as any
            // other bad credentials, WITHOUT registering a failed attempt -
            // an attacker probing this username with password guesses should
            // not be able to lock the account out of its legitimate SSO login.
            throw new BadCredentialsException("Invalid username or password");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException ex) {
            registerFailedAttempt(user);
            throw ex;
        }

        // successful password check -> reset lockout counters
        resetFailedAttempts(user);

        if (user.isMfaEnabled()) {
            String challengeToken = jwtService.generateMfaChallengeToken(user.getUsername());
            return AuthResponse.builder()
                    .mfaRequired(true)
                    .challengeToken(challengeToken)
                    .build();
        }

        return issueTokens(user, httpRequest);
    }

    @Transactional
    public AuthResponse verifyMfaAndIssueTokens(MfaVerifyRequest request, HttpServletRequest httpRequest) {
        String username;
        try {
            username = jwtService.validateAndExtractMfaChallenge(request.getChallengeToken());
        } catch (JwtException e) {
            throw new InvalidMfaCodeException("MFA challenge is invalid or expired, please log in again");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidMfaCodeException("MFA challenge is invalid or expired, please log in again"));

        if (!user.isMfaEnabled() || user.getMfaSecret() == null) {
            throw new InvalidMfaCodeException("MFA is not enabled for this account");
        }

        if (!mfaService.verifyCode(user.getMfaSecret(), request.getCode())) {
            throw new InvalidMfaCodeException("Invalid or expired MFA code");
        }

        return issueTokens(user, httpRequest);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        RefreshToken current = refreshTokenService.verify(request.getRefreshToken());
        RefreshToken rotated = refreshTokenService.rotate(current, httpRequest);
        User user = rotated.getUser();

        String accessToken = jwtService.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rotated.getToken())
                .expiresInSeconds(jwtService.getAccessTokenExpirationMs() / 1000)
                .mfaRequired(false)
                .build();
    }

    @Transactional
    public MessageResponse logout(RefreshTokenRequest request) {
        // Revoke just the presented refresh token; access tokens simply expire
        // naturally (short TTL) which is the standard stateless-JWT trade-off.
        RefreshToken token = refreshTokenService.verify(request.getRefreshToken());
        token.setRevoked(true);
        return new MessageResponse("Logged out successfully");
    }

    @Transactional
    public MessageResponse logoutAllDevices(User user) {
        refreshTokenService.revokeAllForUser(user);
        return new MessageResponse("Logged out from all devices");
    }

    // ------------------------------------------------------------------
    // MFA enrolment
    // ------------------------------------------------------------------

    @Transactional
    public MfaSetupResponse setupMfa(User user) {
        String secret = mfaService.generateSecret();
        user.setMfaSecret(secret);
        user.setMfaEnabled(false); // not enabled until confirmed via /mfa/enable
        userRepository.save(user);
        return mfaService.buildSetupResponse(user, secret);
    }

    @Transactional
    public MessageResponse enableMfa(User user, MfaEnableRequest request) {
        if (user.getMfaSecret() == null) {
            throw new InvalidMfaCodeException("MFA setup has not been initiated");
        }
        if (!mfaService.verifyCode(user.getMfaSecret(), request.getCode())) {
            throw new InvalidMfaCodeException("Invalid MFA code");
        }
        user.setMfaEnabled(true);
        userRepository.save(user);
        return new MessageResponse("Multi-factor authentication enabled");
    }

    @Transactional
    public MessageResponse disableMfa(User user) {
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
        return new MessageResponse("Multi-factor authentication disabled");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private AuthResponse issueTokens(User user, HttpServletRequest httpRequest) {
        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, httpRequest);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresInSeconds(jwtService.getAccessTokenExpirationMs() / 1000)
                .mfaRequired(false)
                .build();
    }

    private void checkAccountLock(User user) {
        if (!user.isAccountNonLocked()) {
            if (user.getLockTime() != null) {
                long lockDurationMinutes = securityProperties.getAccountLock().getLockDurationMinutes();
                Instant unlockAt = user.getLockTime().plus(lockDurationMinutes, ChronoUnit.MINUTES);
                if (Instant.now().isAfter(unlockAt)) {
                    // lock has expired -> unlock automatically
                    user.setAccountNonLocked(true);
                    user.setFailedAttempts(0);
                    user.setLockTime(null);
                    userRepository.save(user);
                    return;
                }
            }
            throw new AccountLockedException("Account is locked due to multiple failed login attempts. Try again later.");
        }
    }

    private void registerFailedAttempt(User user) {
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

    private void resetFailedAttempts(User user) {
        if (user.getFailedAttempts() != 0) {
            user.setFailedAttempts(0);
            userRepository.save(user);
        }
    }
}
