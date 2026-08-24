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
    private final UsernameGenerator usernameGenerator;
    private final LoginAttemptService loginAttemptService;

    /** "Individual" account type: one user, one free companion, no company workspace. */
    @Transactional
    public MessageResponse signupIndividual(IndividualSignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        User user = User.builder()
                .username(usernameGenerator.generateUniqueUsername(request.getEmail()))
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
        // Domains are deliberately not unique - several workspaces may sign up
        // under the same company domain. The admin's email still has to be
        // unique, which is what actually keeps the accounts distinct.
        String domain = request.getCompanyDomain().trim().toLowerCase();
        if (userRepository.existsByEmail(request.getAdminEmail())) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        Company company = companyRepository.save(Company.builder()
                .name(request.getCompanyName().trim())
                .domain(domain)
                .build());

        User admin = User.builder()
                .username(usernameGenerator.generateUniqueUsername(request.getAdminEmail()))
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

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        checkAccountLock(user);

        if (user.getPassword() == null) {
            // SSO-provisioned account (authProvider = SAML): there is no local
            // password to check. Fail with the same generic message as any
            // other bad credentials, WITHOUT registering a failed attempt -
            // an attacker probing this email with password guesses should
            // not be able to lock the account out of its legitimate SSO login.
            throw new BadCredentialsException("Invalid email or password");
        }

        try {
            // Spring Security's UserDetailsService/JWT subject/MFA challenge
            // tokens are all still keyed on the internal auto-generated
            // username (see UsernameGenerator) - only the login *request*
            // moved to email, so that machinery is untouched.
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword()));
        } catch (BadCredentialsException ex) {
            // Must commit independently - see LoginAttemptService's javadoc for why.
            loginAttemptService.registerFailedAttempt(user.getId());
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

    /**
     * Self-service password change for an already-authenticated user. Unlike
     * forgot/reset-password, there is no enumeration risk here - the JWT
     * already identifies the account - so the error can name the problem
     * directly ("current password is incorrect") instead of staying generic.
     */
    @Transactional
    public MessageResponse changePassword(User user, ChangePasswordRequest request) {
        if (user.getPassword() == null) {
            // SSO-provisioned account: nothing local to change.
            throw new InvalidCurrentPasswordException("This account does not have a local password to change");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCurrentPasswordException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);

        // A password change should end every other active session, the same
        // way a forgot-password reset does.
        refreshTokenService.revokeAllForUser(user);

        log.info("User '{}' changed their password", user.getUsername());
        return new MessageResponse("Password changed successfully.");
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

    private void resetFailedAttempts(User user) {
        if (user.getFailedAttempts() != 0) {
            user.setFailedAttempts(0);
            userRepository.save(user);
        }
    }
}
