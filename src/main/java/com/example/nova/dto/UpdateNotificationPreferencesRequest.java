package com.example.nova.dto;

import lombok.Data;

@Data
public class UpdateNotificationPreferencesRequest {
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
}
