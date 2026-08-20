package com.example.nova.controller;

import com.example.nova.dto.AddTeamUserRequest;
import com.example.nova.dto.AssignCompanionRequest;
import com.example.nova.dto.TeamUserResponse;
import com.example.nova.entity.User;
import com.example.nova.service.TeamUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team-users")
@RequiredArgsConstructor
public class TeamUserController {

    private final TeamUserService teamUserService;

    /** Company admins only - adds a user to the admin's own company workspace. */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<TeamUserResponse> addTeamUser(@AuthenticationPrincipal User admin,
                                                          @Valid @RequestBody AddTeamUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamUserService.addTeamUser(admin, request));
    }

    /** Company admins only - every member of the admin's own company, with their assigned companion (if any). */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<TeamUserResponse>> listTeamUsers(@AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(teamUserService.listTeamUsers(admin));
    }

    /** Company admins only - pairs a companion (picked from GET /api/companions/unassigned) with this team user. */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/companion")
    public ResponseEntity<TeamUserResponse> assignCompanion(@AuthenticationPrincipal User admin,
                                                              @PathVariable Long userId,
                                                              @Valid @RequestBody AssignCompanionRequest request) {
        return ResponseEntity.ok(teamUserService.assignCompanion(admin, userId, request.getCompanionId()));
    }
}
