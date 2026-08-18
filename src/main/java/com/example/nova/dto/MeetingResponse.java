package com.example.nova.dto;

import com.example.nova.entity.MeetingPlatform;
import com.example.nova.entity.MeetingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class MeetingResponse {
    private Long id;
    private String title;
    private Long companionId;
    private String companionName;
    private String project; // null -> shown as "Unassigned"
    private MeetingPlatform platform;
    private String meetingUrl;
    private List<String> attendees;
    private int attendeeCount;
    private MeetingStatus status;
    private Instant scheduledAt;
    private boolean companionJoined;
    private boolean autoJoinMeetings;
    private boolean sendMomAutomatically;
    private boolean respondInVoice;
    private boolean recordMeetingAudio;
}
