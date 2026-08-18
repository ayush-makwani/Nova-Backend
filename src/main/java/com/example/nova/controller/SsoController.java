package com.example.nova.controller;

import com.example.nova.dto.AuthResponse;
import com.example.nova.dto.SsoExchangeRequest;
import com.example.nova.dto.SsoProviderResponse;
import com.example.nova.service.SsoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public endpoints that bridge browser-redirect SAML SSO into this API's own
 * JWT session model. The actual SAML protocol endpoints
 * ({@code /saml2/authenticate/{registrationId}}, {@code /login/saml2/sso/{registrationId}},
 * {@code /saml2/service-provider-metadata/{registrationId}}) are provided
 * directly by Spring Security's saml2Login support (see SamlSsoConfig) and
 * only exist when {@code app.security.sso.saml.enabled=true}.
 */
@RestController
@RequestMapping("/api/auth/sso")
@RequiredArgsConstructor
public class SsoController {

    private final SsoService ssoService;

    /** Lists configured SAML identity providers so the frontend can render "Login with ..." buttons. */
    @GetMapping("/providers")
    public ResponseEntity<List<SsoProviderResponse>> providers() {
        return ResponseEntity.ok(ssoService.listProviders());
    }

    /** Step 2 of SSO login: trade the one-time code from the SAML redirect callback for real tokens. */
    @PostMapping("/exchange")
    public ResponseEntity<AuthResponse> exchange(@Valid @RequestBody SsoExchangeRequest request,
                                                   HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ssoService.exchangeCode(request.getCode(), httpRequest));
    }
}
