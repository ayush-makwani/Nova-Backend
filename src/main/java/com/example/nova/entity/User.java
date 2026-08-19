package com.example.nova.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(name = "uk_users_sso_identity", columnNames = {"sso_registration_id", "sso_subject_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String email;

    // Nullable: SSO-provisioned accounts (authProvider = SAML) have no local
    // password. AuthService.login() rejects local login for such accounts
    // with the same generic "Invalid username or password" response used for
    // any other bad credentials, before ever touching the lockout counter.
    private String password;

    @Column(name = "full_name", length = 100)
    private String fullName;

    // Nullable: only set for accounts created via the "Company / Team" signup
    // flow. The admin who completes that flow gets ROLE_ADMIN over this workspace.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    @Column(name = "account_non_locked")
    private boolean accountNonLocked = true;

    @Builder.Default
    @Column(name = "failed_attempts")
    private int failedAttempts = 0;

    @Column(name = "lock_time")
    private Instant lockTime;

    // ---- Multi-Factor Authentication ----
    @Builder.Default
    @Column(name = "mfa_enabled")
    private boolean mfaEnabled = false;

    @Column(name = "mfa_secret")
    private String mfaSecret;

    @Builder.Default
    @Column(name = "credentials_non_expired")
    private boolean credentialsNonExpired = true;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    // ---- Enterprise SSO (SAML) ----
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    /** The SAML relying-party registrationId this account is linked to (null for LOCAL accounts). */
    @Column(name = "sso_registration_id", length = 100)
    private String ssoRegistrationId;

    /** The SAML NameID (or other stable subject identifier) asserted by the IdP for this account. */
    @Column(name = "sso_subject_id", length = 255)
    private String ssoSubjectId;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.passwordChangedAt = Instant.now();
    }

    // ---- UserDetails contract ----
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
