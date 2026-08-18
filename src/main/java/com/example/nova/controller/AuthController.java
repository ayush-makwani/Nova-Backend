package com.example.nova.controller;

import com.example.nova.dto.*;
import com.example.nova.entity.User;
import com.example.nova.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest));
    }

    /** Step 2 of login when the account has MFA enabled. */
    @PostMapping("/mfa/verify")
    public ResponseEntity<AuthResponse> verifyMfa(@Valid @RequestBody MfaVerifyRequest request,
                                                    HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.verifyMfaAndIssueTokens(request, httpRequest));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request,
                                                       HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.refreshToken(request, httpRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.logout(request));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<MessageResponse> logoutAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.logoutAllDevices(user));
    }

    // ------------------------------------------------------------------
    // Multi-factor authentication enrolment (requires an authenticated session)
    // ------------------------------------------------------------------

    @PostMapping("/mfa/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.setupMfa(user));
    }

    @PostMapping("/mfa/enable")
    public ResponseEntity<MessageResponse> enableMfa(@AuthenticationPrincipal User user,
                                                       @Valid @RequestBody MfaEnableRequest request) {
        return ResponseEntity.ok(authService.enableMfa(user, request));
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<MessageResponse> disableMfa(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.disableMfa(user));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal User user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("fullName", user.getFullName());
        profile.put("roles", user.getRoles());
        profile.put("mfaEnabled", user.isMfaEnabled());
        return ResponseEntity.ok(profile);
    }
}
