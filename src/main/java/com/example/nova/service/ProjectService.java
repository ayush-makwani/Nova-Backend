package com.example.nova.service;

import com.example.nova.dto.CreateProjectRequest;
import com.example.nova.dto.ProjectOptionResponse;
import com.example.nova.dto.ProjectResponse;
import com.example.nova.entity.Companion;
import com.example.nova.entity.Meeting;
import com.example.nova.entity.Project;
import com.example.nova.entity.User;
import com.example.nova.exception.CompanionNotFoundException;
import com.example.nova.exception.ProjectAlreadyExistsException;
import com.example.nova.repository.CompanionRepository;
import com.example.nova.repository.MeetingRepository;
import com.example.nova.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CompanionRepository companionRepository;
    private final MeetingRepository meetingRepository;

    @Transactional
    public ProjectResponse createProject(User user, CreateProjectRequest request) {
        String name = request.getName().trim();
        if (projectRepository.existsByUserAndNameIgnoreCase(user, name)) {
            throw new ProjectAlreadyExistsException("A project named '" + name + "' already exists");
        }

        // Optional: link an existing companion at creation time. Not required -
        // a project can exist companion-less - and not exclusive - the same
        // companion may already be linked to other projects.
        Companion companion = null;
        if (request.getCompanionId() != null) {
            companion = companionRepository.findByIdAndUser(request.getCompanionId(), user)
                    .orElseThrow(() -> new CompanionNotFoundException("Companion not found"));
        }

        List<String> tags = request.getTags() == null
                ? new ArrayList<>()
                : request.getTags().stream().map(String::trim).collect(Collectors.toList());
        List<String> documentKeys = request.getDocumentKeys() == null
                ? new ArrayList<>()
                : request.getDocumentKeys().stream().map(String::trim).collect(Collectors.toList());

        Project project = Project.builder()
                .user(user)
                .name(name)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .tags(tags)
                .textContext(request.getTextContext() != null ? request.getTextContext().trim() : null)
                .documentKeys(documentKeys)
                .voiceNoteKey(request.getVoiceNoteKey() != null ? request.getVoiceNoteKey().trim() : null)
                .companion(companion)
                .build();
        project = projectRepository.save(project);

        log.info("User '{}' created project '{}'{}", user.getUsername(), name,
                companion != null ? ", assigned companion '" + companion.getName() + "'" : "");

        return toResponse(project);
    }

    // Each row's companion comes straight off the Project entity now; keeping
    // this in one transaction still avoids lazy-association surprises when
    // computing each companion's meeting stats below.
    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects(User user) {
        return projectRepository.findAllByUserOrderByCreatedAtAsc(user).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Lightweight id/name pairs for populating a "Select Project" dropdown. */
    @Transactional(readOnly = true)
    public List<ProjectOptionResponse> listProjectOptions(User user) {
        return projectRepository.findAllByUserOrderByCreatedAtAsc(user).stream()
                .map(project -> ProjectOptionResponse.builder().id(project.getId()).name(project.getName()).build())
                .collect(Collectors.toList());
    }

    private ProjectResponse toResponse(Project project) {
        Companion companion = project.getCompanion();
        int meetingsCount = 0;
        Instant lastMeetingAt = null;
        if (companion != null) {
            meetingsCount = (int) meetingRepository.countByCompanion(companion);
            lastMeetingAt = meetingRepository.findFirstByCompanionOrderByScheduledAtDesc(companion)
                    .map(Meeting::getScheduledAt)
                    .orElse(null);
        }

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .tags(project.getTags())
                .companionId(companion != null ? companion.getId() : null)
                .companionName(companion != null ? companion.getName() : null)
                .companionPresenceStatus(companion != null ? companion.getPresenceStatus() : null)
                .meetingsCount(meetingsCount)
                .contextFilesCount(project.getDocumentKeys().size())
                .documentKeys(project.getDocumentKeys())
                .voiceNoteKey(project.getVoiceNoteKey())
                .lastMeetingAt(lastMeetingAt)
                .createdAt(project.getCreatedAt())
                .build();
    }
}
