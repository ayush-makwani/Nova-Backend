package com.example.nova.controller;

import com.example.nova.dto.CreateProjectRequest;
import com.example.nova.dto.ProjectOptionResponse;
import com.example.nova.dto.ProjectResponse;
import com.example.nova.entity.User;
import com.example.nova.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
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
}
