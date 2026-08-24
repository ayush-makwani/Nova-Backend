package com.example.nova.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A team/company workspace created via the "Company / Team" signup flow.
 *
 * <p>{@code domain} is intentionally NOT unique - several workspaces may sign
 * up under the same company domain. Companion identity emails remain unique on
 * their own (see {@code CompanionService.uniqueCompanionEmail}).
 */
@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    // Namespaces companion identity emails, e.g. nova-1@<domain>. Lower-cased at creation.
    @Column(nullable = false, length = 100)
    private String domain;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
