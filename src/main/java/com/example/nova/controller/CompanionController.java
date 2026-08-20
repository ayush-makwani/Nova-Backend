package com.example.nova.controller;

import com.example.nova.dto.CompanionOptionResponse;
import com.example.nova.dto.CompanionResponse;
import com.example.nova.dto.CompanionSettingsResponse;
import com.example.nova.dto.CreateCompanionRequest;
import com.example.nova.dto.CreateCompanionResponse;
import com.example.nova.dto.MeetingResponse;
import com.example.nova.dto.UpdateCompanionSettingsRequest;
import com.example.nova.dto.UpdateCompanionVoiceRequest;
import com.example.nova.dto.VoiceResponse;
import com.example.nova.entity.User;
import com.example.nova.service.CompanionService;
import com.example.nova.service.MeetingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companions")
@RequiredArgsConstructor
public class CompanionController {

    private final CompanionService companionService;
    private final MeetingService meetingService;

    @PostMapping
    public ResponseEntity<CreateCompanionResponse> createCompanions(@AuthenticationPrincipal User user,
                                                                      @Valid @RequestBody CreateCompanionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companionService.createCompanions(user, request));
    }

    @GetMapping
    public ResponseEntity<List<CompanionResponse>> listCompanions(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(companionService.listCompanions(user));
    }

    /** Company admins only - companions across the company not yet paired with a team member (Team Users dropdown). */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/unassigned")
    public ResponseEntity<List<CompanionOptionResponse>> listUnassignedCompanionOptions(@AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(companionService.listUnassignedCompanionOptions(admin));
    }

    @GetMapping("/{id}/settings")
    public ResponseEntity<CompanionSettingsResponse> getSettings(@AuthenticationPrincipal User user,
                                                                   @PathVariable Long id) {
        return ResponseEntity.ok(companionService.getSettings(user, id));
    }

    @PatchMapping("/{id}/settings")
    public ResponseEntity<CompanionSettingsResponse> updateSettings(@AuthenticationPrincipal User user,
                                                                      @PathVariable Long id,
                                                                      @Valid @RequestBody UpdateCompanionSettingsRequest request) {
        return ResponseEntity.ok(companionService.updateSettings(user, id, request));
    }

    @GetMapping("/{id}/voices")
    public ResponseEntity<List<VoiceResponse>> listVoices(@AuthenticationPrincipal User user,
                                                            @PathVariable Long id) {
        return ResponseEntity.ok(companionService.listVoices(user, id));
    }

    @PatchMapping("/{id}/voice")
    public ResponseEntity<List<VoiceResponse>> updateVoice(@AuthenticationPrincipal User user,
                                                             @PathVariable Long id,
                                                             @Valid @RequestBody UpdateCompanionVoiceRequest request) {
        return ResponseEntity.ok(companionService.updateVoice(user, id, request));
    }

    /** Meetings attended/scheduled for this companion, optionally narrowed to one project via ?project=. */
    @GetMapping("/{id}/meetings")
    public ResponseEntity<List<MeetingResponse>> listMeetings(@AuthenticationPrincipal User user,
                                                                @PathVariable Long id,
                                                                @RequestParam(required = false) String project) {
        return ResponseEntity.ok(meetingService.listForCompanion(user, id, project));
    }
}
