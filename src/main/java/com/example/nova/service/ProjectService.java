package com.example.nova.service;

import com.example.nova.dto.CreateProjectRequest;
import com.example.nova.dto.ProjectDocumentUrlResponse;
import com.example.nova.dto.ProjectOptionResponse;
import com.example.nova.dto.ProjectResponse;
import com.example.nova.entity.AwsCredential;
import com.example.nova.entity.Companion;
import com.example.nova.entity.Meeting;
import com.example.nova.entity.Project;
import com.example.nova.entity.User;
import com.example.nova.exception.AwsCredentialNotFoundException;
import com.example.nova.exception.CompanionNotFoundException;
import com.example.nova.exception.ProjectAlreadyExistsException;
import com.example.nova.exception.ProjectNotFoundException;
import com.example.nova.repository.AwsCredentialRepository;
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
    private final AwsCredentialRepository awsCredentialRepository;

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

    /** Adds one or more document keys to the project's context documents ("+ Add Document"). */
    @Transactional
    public ProjectResponse addDocuments(User user, Long projectId, List<String> documentKeys) {
        Project project = projectRepository.findByIdAndUser(projectId, user)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        documentKeys.stream().map(String::trim).forEach(project.getDocumentKeys()::add);
        project = projectRepository.save(project);

        log.info("User '{}' added {} context document(s) to project '{}'",
                user.getUsername(), documentKeys.size(), project.getName());

        return toResponse(project);
    }

    /** Removes one document key from the project's context documents (the "x" on each document chip). */
    @Transactional
    public ProjectResponse removeDocument(User user, Long projectId, String documentKey) {
        Project project = projectRepository.findByIdAndUser(projectId, user)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        project.getDocumentKeys().remove(documentKey.trim());
        project = projectRepository.save(project);

        log.info("User '{}' removed a context document from project '{}'", user.getUsername(), project.getName());

        return toResponse(project);
    }

    /** Resolves the project's context document keys into fetchable URLs against the app's S3 configuration. */
    @Transactional(readOnly = true)
    public List<ProjectDocumentUrlResponse> getDocumentUrls(User user, Long projectId) {
        Project project = projectRepository.findByIdAndUser(projectId, user)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        if (project.getDocumentKeys().isEmpty()) {
            return List.of();
        }

        AwsCredential credential = awsCredentialRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new AwsCredentialNotFoundException("AWS credentials are not configured yet"));

        return project.getDocumentKeys().stream()
                .map(key -> ProjectDocumentUrlResponse.builder()
                        .documentKey(key)
                        .url(resolveUrl(credential.getUrl(), key))
                        .build())
                .collect(Collectors.toList());
    }

    private String resolveUrl(String baseUrl, String key) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String path = key.startsWith("/") ? key.substring(1) : key;
        return base + "/" + path;
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
