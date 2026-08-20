package com.example.nova.controller;

import com.example.nova.dto.AddTeamUserRequest;
import com.example.nova.dto.TeamUserResponse;
import com.example.nova.entity.User;
import com.example.nova.service.TeamUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
