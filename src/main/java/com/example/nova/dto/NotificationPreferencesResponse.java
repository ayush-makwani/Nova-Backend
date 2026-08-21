package com.example.nova.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class NotificationPreferencesResponse {
    // Meetings
    private boolean meetingStartReminder;
    private boolean companionJoinedMeeting;
    private boolean meetingEnded;
    private boolean momDelivered;

    // Action items
    private boolean newActionItemsCreated;
    private boolean actionItemDueSoon;

    // System
    private boolean companionOfflineAlert;
    private boolean weeklyUsageDigest;

    private Instant updatedAt;
}
