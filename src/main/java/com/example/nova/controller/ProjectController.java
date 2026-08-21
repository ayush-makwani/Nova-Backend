package com.example.nova.controller;

import com.example.nova.dto.CreateProjectRequest;
import com.example.nova.dto.ProjectDocumentRequest;
import com.example.nova.dto.ProjectDocumentUrlResponse;
import com.example.nova.dto.ProjectOptionResponse;
import com.example.nova.dto.ProjectResponse;
import com.example.nova.entity.User;
import com.example.nova.service.ProjectService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Validated
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@AuthenticationPrincipal User user,
                                                           @Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(user, request));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> listProjects(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(projectService.listProjects(user));
    }

    /** Lightweight id/name list for populating a "Select Project" dropdown (e.g. the Schedule Meeting form). */
    @GetMapping("/options")
    public ResponseEntity<List<ProjectOptionResponse>> listProjectOptions(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(projectService.listProjectOptions(user));
    }

    /** "+ Add Document" on the Companion Context Documents panel - accepts one or more document keys at once. */
    @PostMapping("/{projectId}/documents")
    public ResponseEntity<ProjectResponse> addDocuments(@AuthenticationPrincipal User user,
                                                          @PathVariable Long projectId,
                                                          @Valid @RequestBody ProjectDocumentRequest request) {
        return ResponseEntity.ok(projectService.addDocuments(user, projectId, request.getDocumentKeys()));
    }

    /** The "x" on a document chip in the Companion Context Documents panel. */
    @DeleteMapping("/{projectId}/documents")
    public ResponseEntity<ProjectResponse> removeDocument(@AuthenticationPrincipal User user,
                                                            @PathVariable Long projectId,
                                                            @RequestParam @NotBlank String documentKey) {
        return ResponseEntity.ok(projectService.removeDocument(user, projectId, documentKey));
    }

    /** Fetchable URL for each context document, resolved against the app's S3 configuration. */
    @GetMapping("/{projectId}/documents")
    public ResponseEntity<List<ProjectDocumentUrlResponse>> getDocumentUrls(@AuthenticationPrincipal User user,
                                                                              @PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.getDocumentUrls(user, projectId));
    }
}
