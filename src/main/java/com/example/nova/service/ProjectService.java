package com.example.nova.service;

import com.example.nova.dto.CreateProjectRequest;
import com.example.nova.dto.ProjectOptionResponse;
import com.example.nova.dto.ProjectResponse;
import com.example.nova.entity.Companion;
import com.example.nova.entity.Meeting;
import com.example.nova.entity.Project;
import com.example.nova.entity.User;
import com.example.nova.exception.NoAvailableCompanionException;
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

        // Auto-assign the oldest companion not already linked to a project, matching
        // the "<Companion> will be assigned" preview shown before the user confirms.
        Companion companion = companionRepository.findFirstByUserAndProjectIsNullOrderBySeatNumberAsc(user)
                .orElseThrow(() -> new NoAvailableCompanionException(
                        "No available companion to assign - every companion is already linked to a project"));

        List<String> tags = request.getTags() == null
                ? new ArrayList<>()
                : request.getTags().stream().map(String::trim).collect(Collectors.toList());

        Project project = Project.builder()
                .user(user)
                .name(name)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .tags(tags)
                .textContext(request.getTextContext() != null ? request.getTextContext().trim() : null)
                .contextFilesCount(0)
                .build();
        project = projectRepository.save(project);

        companion.setProject(project);
        companionRepository.save(companion);

        log.info("User '{}' created project '{}', assigned companion '{}'", user.getUsername(), name, companion.getName());

        return toResponse(project, companion);
    }

    public List<ProjectResponse> listProjects(User user) {
        return projectRepository.findAllByUserOrderByCreatedAtAsc(user).stream()
                .map(project -> toResponse(project, companionRepository.findByProject(project).orElse(null)))
                .collect(Collectors.toList());
    }

    /** Lightweight id/name pairs for populating a "Select Project" dropdown. */
    public List<ProjectOptionResponse> listProjectOptions(User user) {
        return projectRepository.findAllByUserOrderByCreatedAtAsc(user).stream()
                .map(project -> ProjectOptionResponse.builder().id(project.getId()).name(project.getName()).build())
                .collect(Collectors.toList());
    }

    private ProjectResponse toResponse(Project project, Companion companion) {
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
                .contextFilesCount(project.getContextFilesCount())
                .lastMeetingAt(lastMeetingAt)
                .createdAt(project.getCreatedAt())
                .build();
    }
}
