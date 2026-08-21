package com.example.nova.service;

import com.example.nova.dto.ProjectMemoryResponse;
import com.example.nova.dto.UpdateProjectMemoryRequest;
import com.example.nova.entity.Project;
import com.example.nova.entity.ProjectMemory;
import com.example.nova.entity.User;
import com.example.nova.exception.ProjectNotFoundException;
import com.example.nova.repository.ProjectMemoryRepository;
import com.example.nova.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemoryService {

    private final ProjectRepository projectRepository;
    private final ProjectMemoryRepository projectMemoryRepository;

    /** A project that has never finished a meeting has no memory yet - answer 200 with empty fields, not 404. */
    @Transactional(readOnly = true)
    public ProjectMemoryResponse getMemory(User user, Long projectId) {
        Project project = projectRepository.findByIdAndUser(projectId, user)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        return projectMemoryRepository.findByProject(project)
                .map(memory -> toResponse(project.getId(), memory))
                .orElseGet(() -> emptyResponse(project.getId()));
    }

    /**
     * Replaces the memory wholesale. The caller has already folded the new
     * meeting into the previous memory before sending this, so this never
     * merges - decisions/actions/notes are set to exactly what's provided.
     *
     * meetingsSummarised increments unconditionally on every call: there's no
     * meeting id in the payload to key an idempotent increment off of, and a
     * retried call incrementing twice is an acceptable drift on a display number.
     */
    @Transactional
    public ProjectMemoryResponse replaceMemory(User user, Long projectId, UpdateProjectMemoryRequest request) {
        Project project = projectRepository.findByIdAndUser(projectId, user)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        ProjectMemory memory = projectMemoryRepository.findByProject(project)
                .orElseGet(() -> ProjectMemory.builder().project(project).build());

        memory.setCompactMemory(request.getCompactMemory());
        memory.setDecisions(copyOrEmpty(request.getDecisions()));
        memory.setActions(copyOrEmpty(request.getActions()));
        memory.setNotes(copyOrEmpty(request.getNotes()));
        memory.setMeetingsSummarised(memory.getMeetingsSummarised() + 1);
        memory.setUpdatedAt(Instant.now());

        memory = projectMemoryRepository.save(memory);
        log.info("Replaced memory for project '{}' ({} meeting(s) summarised)",
                project.getName(), memory.getMeetingsSummarised());

        return toResponse(project.getId(), memory);
    }

    private List<String> copyOrEmpty(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    private ProjectMemoryResponse emptyResponse(Long projectId) {
        return ProjectMemoryResponse.builder()
                .projectId(projectId)
                .compactMemory("")
                .decisions(List.of())
                .actions(List.of())
                .notes(List.of())
                .meetingsSummarised(0)
                .updatedAt(null)
                .build();
    }

    private ProjectMemoryResponse toResponse(Long projectId, ProjectMemory memory) {
        return ProjectMemoryResponse.builder()
                .projectId(projectId)
                .compactMemory(memory.getCompactMemory())
                .decisions(memory.getDecisions())
                .actions(memory.getActions())
                .notes(memory.getNotes())
                .meetingsSummarised(memory.getMeetingsSummarised())
                .updatedAt(memory.getUpdatedAt())
                .build();
    }
}
