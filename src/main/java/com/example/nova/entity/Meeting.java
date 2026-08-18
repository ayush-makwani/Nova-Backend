package com.example.nova.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meetings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "companion_id", nullable = false)
    private Companion companion;

    @Column(nullable = false, length = 150)
    private String title;

    // Project the companion was working under when this meeting happened;
    // recorded independently of the companion's current project assignment.
    @Column(length = 100)
    private String project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingPlatform platform;

    // Join URL for the companion (and attendees) to connect with.
    @Column(name = "meeting_url", nullable = false, length = 500)
    private String meetingUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "meeting_attendees", joinColumns = @JoinColumn(name = "meeting_id"))
    @OrderColumn(name = "attendee_order")
    @Column(name = "attendee", length = 100)
    @Builder.Default
    private List<String> attendees = new ArrayList<>();

    @Column(name = "attendee_count", nullable = false)
    private int attendeeCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MeetingStatus status = MeetingStatus.SCHEDULED;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    // True once the companion has actually joined a LIVE meeting; drives the "Companion joined" badge.
    @Column(name = "companion_joined", nullable = false)
    @Builder.Default
    private boolean companionJoined = false;

    // ---- Per-meeting companion behavior overrides ----
    @Column(name = "auto_join_meetings", nullable = false)
    @Builder.Default
    private boolean autoJoinMeetings = true;

    @Column(name = "send_mom_automatically", nullable = false)
    @Builder.Default
    private boolean sendMomAutomatically = true;

    @Column(name = "respond_in_voice", nullable = false)
    @Builder.Default
    private boolean respondInVoice = false;

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
