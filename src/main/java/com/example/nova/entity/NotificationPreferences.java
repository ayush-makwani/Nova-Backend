package com.example.nova.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Per-user notification toggle set (Settings > Notifications screen). Lazily created with defaults on first access. */
@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ---- Meetings ----
    @Column(name = "meeting_start_reminder", nullable = false)
    @Builder.Default
    private boolean meetingStartReminder = true;

    @Column(name = "companion_joined_meeting", nullable = false)
    @Builder.Default
    private boolean companionJoinedMeeting = true;

    @Column(name = "meeting_ended", nullable = false)
    @Builder.Default
    private boolean meetingEnded = false;

    @Column(name = "mom_delivered", nullable = false)
    @Builder.Default
    private boolean momDelivered = true;

    // ---- Action Items ----
    @Column(name = "new_action_items_created", nullable = false)
    @Builder.Default
    private boolean newActionItemsCreated = true;

    @Column(name = "action_item_due_soon", nullable = false)
    @Builder.Default
    private boolean actionItemDueSoon = true;

    // ---- System ----
    @Column(name = "companion_offline_alert", nullable = false)
    @Builder.Default
    private boolean companionOfflineAlert = true;

    @Column(name = "weekly_usage_digest", nullable = false)
    @Builder.Default
    private boolean weeklyUsageDigest = false;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
