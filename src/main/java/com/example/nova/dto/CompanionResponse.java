package com.example.nova.dto;

import com.example.nova.entity.CompanionPresenceStatus;
import com.example.nova.entity.CompanionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class CompanionResponse {
    private Long id;
    private String name;
    private Integer seatNumber;
    private String email;
    private String voice;
    private List<String> projects; // names of every project this companion is linked to; empty -> "Unassigned"
    private Long assignedUserId; // null -> not paired with a team member (Team Users screen)
    private CompanionStatus status;
    private CompanionPresenceStatus presenceStatus;
    private int meetingsCount;
    private Instant lastMeetingAt; // null until the companion has attended a meeting
    private boolean autoJoinMeetings;
    private boolean sendMomAutomatically;
    private boolean respondInVoice;
    private boolean recordMeetingAudio;
    private BigDecimal pricePerMonth;
    private Instant createdAt;
}
