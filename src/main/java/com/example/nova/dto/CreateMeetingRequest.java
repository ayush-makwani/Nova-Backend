package com.example.nova.dto;

import com.example.nova.entity.MeetingPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class CreateMeetingRequest {

    @NotBlank(message = "Meeting title is required")
    @Size(min = 2, max = 150, message = "Meeting title must be between 2 and 150 characters")
    private String title;

    // The companion is resolved server-side from the project it's assigned to, not chosen directly.
    @NotNull(message = "A project must be selected")
    private Long projectId;

    @NotNull(message = "Meeting platform is required")
    private MeetingPlatform platform;

    @NotBlank(message = "Meeting URL is required")
    @Size(max = 500)
    private String meetingUrl;

    @NotNull(message = "Date and time are required")
    private Instant scheduledAt;

    @Size(max = 50, message = "No more than 50 attendees are allowed")
    private List<@NotBlank @Size(max = 100) String> attendees;

    private boolean autoJoinMeetings = true;
    private boolean sendMomAutomatically = true;
    private boolean respondInVoice = false;
    private boolean recordMeetingAudio = false;
}
