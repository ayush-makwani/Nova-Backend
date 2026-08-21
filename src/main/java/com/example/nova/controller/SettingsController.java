package com.example.nova.controller;

import com.example.nova.dto.NotificationPreferencesResponse;
import com.example.nova.dto.UpdateNotificationPreferencesRequest;
import com.example.nova.entity.User;
import com.example.nova.service.NotificationPreferencesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final NotificationPreferencesService notificationPreferencesService;

    /** Settings > Notifications tab. */
    @GetMapping("/notifications")
    public ResponseEntity<NotificationPreferencesResponse> getNotificationPreferences(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationPreferencesService.getPreferences(user));
    }

    @PatchMapping("/notifications")
    public ResponseEntity<NotificationPreferencesResponse> updateNotificationPreferences(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateNotificationPreferencesRequest request) {
        return ResponseEntity.ok(notificationPreferencesService.updatePreferences(user, request));
    }
}
