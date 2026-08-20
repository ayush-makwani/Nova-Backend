package com.example.nova.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "companions", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Companion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    // Per-user sequential seat number, shown to the user as "SEAT #<n>".
    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    // Meeting identity: the calendar app is expected to invite this address so
    // the companion can join. Unique across all companions, not just per-user.
    @Column(nullable = false, length = 100)
    private String email;

    // Assigned a default at creation time; changeable later from the AI Voice screen.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voice_id", nullable = false)
    private Voice voice;

    // Nullable: unassigned companions show as "Unassigned" until linked to a project.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    // Nullable: which team member this companion is paired with (Team Users
    // screen, company accounts only). Distinct from `user` above, which is
    // the purchasing/billing owner - for a company account that's always the
    // admin, regardless of which team member the companion is assigned to.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    @Column(name = "price_per_month", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CompanionStatus status = CompanionStatus.ACTIVE;

    // Live presence (idle / in a meeting), independent of the billing status above.
    @Enumerated(EnumType.STRING)
    @Column(name = "presence_status", nullable = false, length = 20)
    @Builder.Default
    private CompanionPresenceStatus presenceStatus = CompanionPresenceStatus.IDLE;

    @Column(name = "meetings_count", nullable = false)
    @Builder.Default
    private int meetingsCount = 0;

    // Nullable: no meetings attended yet.
    @Column(name = "last_meeting_at")
    private Instant lastMeetingAt;

    // ---- Behavior settings (Companion Settings screen) ----
    @Column(name = "auto_join_meetings", nullable = false)
    @Builder.Default
    private boolean autoJoinMeetings = true;

    @Column(name = "send_mom_automatically", nullable = false)
    @Builder.Default
    private boolean sendMomAutomatically = true;

    @Column(name = "respond_in_voice", nullable = false)
    @Builder.Default
    private boolean respondInVoice = true;

    @Column(name = "record_meeting_audio", nullable = false)
    @Builder.Default
    private boolean recordMeetingAudio = false;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
