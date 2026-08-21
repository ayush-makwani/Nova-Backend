package com.example.nova.service;

import com.example.nova.dto.CreateMeetingRequest;
import com.example.nova.dto.MeetingResponse;
import com.example.nova.entity.Companion;
import com.example.nova.entity.Meeting;
import com.example.nova.entity.MeetingStatus;
import com.example.nova.entity.Project;
import com.example.nova.entity.User;
import com.example.nova.exception.CompanionNotFoundException;
import com.example.nova.exception.InvalidCalendarRangeException;
import com.example.nova.exception.ProjectNotFoundException;
import com.example.nova.repository.CompanionRepository;
import com.example.nova.repository.MeetingRepository;
import com.example.nova.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final CompanionRepository companionRepository;
    private final ProjectRepository projectRepository;

    /**
     * Schedules a meeting under a project. The companion is resolved from the
     * project's assignment - matching the "NOVA will automatically assign the
     * right companion" copy on the scheduling screen - rather than chosen directly.
     */
    @Transactional
    public MeetingResponse createMeeting(User user, CreateMeetingRequest request) {
        Project project = projectRepository.findByIdAndUser(request.getProjectId(), user)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        Companion companion = project.getCompanion();
        if (companion == null) {
            throw new CompanionNotFoundException("Selected project has no companion assigned");
        }

        List<String> attendees = request.getAttendees() == null
                ? new ArrayList<>()
                : request.getAttendees().stream().map(String::trim).collect(Collectors.toList());

        Meeting meeting = Meeting.builder()
                .companion(companion)
                .title(request.getTitle().trim())
                .project(project.getName())
                .platform(request.getPlatform())
                .meetingUrl(request.getMeetingUrl().trim())
                .attendees(attendees)
                .attendeeCount(attendees.size())
                .status(MeetingStatus.SCHEDULED)
                .scheduledAt(request.getScheduledAt())
                .autoJoinMeetings(request.isAutoJoinMeetings())
                .sendMomAutomatically(request.isSendMomAutomatically())
                .respondInVoice(request.isRespondInVoice())
                .recordMeetingAudio(request.isRecordMeetingAudio())
                .build();
        meeting = meetingRepository.save(meeting);

        log.info("User '{}' scheduled meeting '{}' for companion '{}' on project '{}'",
                user.getUsername(), meeting.getTitle(), companion.getName(), project.getName());

        return toResponse(meeting);
    }

    /** Lists meetings a companion attended/is scheduled for, optionally narrowed to one project. */
    // Meeting.companion is a lazy association; open-in-view is disabled, so the
    // mapping to MeetingResponse must happen inside a transaction (see toResponse).
    @Transactional(readOnly = true)
    public List<MeetingResponse> listForCompanion(User user, Long companionId, String project) {
        Companion companion = companionRepository.findByIdAndUser(companionId, user)
                .orElseThrow(() -> new CompanionNotFoundException("Companion not found"));

        List<Meeting> meetings = (project == null || project.isBlank())
                ? meetingRepository.findAllByCompanionOrderByScheduledAtDesc(companion)
                : meetingRepository.findAllByCompanionAndProjectIgnoreCaseOrderByScheduledAtDesc(companion, project.trim());

        return meetings.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /** A single project's meeting history (Project Details screen) - the companion + project name it was scheduled under. */
    @Transactional(readOnly = true)
    public List<MeetingResponse> listForProject(Companion companion, String projectName) {
        return meetingRepository.findAllByCompanionAndProjectIgnoreCaseOrderByScheduledAtDesc(companion, projectName).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** All of a user's meetings (across every companion) in a given calendar month, for the Meeting Calendar view. */
    @Transactional(readOnly = true)
    public List<MeetingResponse> listForMonth(User user, int year, int month) {
        if (month < 1 || month > 12) {
            throw new InvalidCalendarRangeException("month must be between 1 and 12");
        }
        if (year < 1970 || year > 9999) {
            throw new InvalidCalendarRangeException("year is out of range");
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        Instant start = yearMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        return meetingRepository
                .findAllByCompanion_UserAndScheduledAtGreaterThanEqualAndScheduledAtLessThanOrderByScheduledAtAsc(user, start, end)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private MeetingResponse toResponse(Meeting meeting) {
        return MeetingResponse.builder()
                .id(meeting.getId())
                .title(meeting.getTitle())
                .companionId(meeting.getCompanion().getId())
                .companionName(meeting.getCompanion().getName())
                .project(meeting.getProject())
                .platform(meeting.getPlatform())
                .meetingUrl(meeting.getMeetingUrl())
                .attendees(meeting.getAttendees())
                .attendeeCount(meeting.getAttendeeCount())
                .status(meeting.getStatus())
                .scheduledAt(meeting.getScheduledAt())
                .companionJoined(meeting.isCompanionJoined())
                .autoJoinMeetings(meeting.isAutoJoinMeetings())
                .sendMomAutomatically(meeting.isSendMomAutomatically())
                .respondInVoice(meeting.isRespondInVoice())
                .recordMeetingAudio(meeting.isRecordMeetingAudio())
                .build();
    }
}
