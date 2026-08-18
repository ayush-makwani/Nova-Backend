package com.example.nova.controller;

import com.example.nova.dto.CreateMeetingRequest;
import com.example.nova.dto.MeetingResponse;
import com.example.nova.entity.User;
import com.example.nova.service.MeetingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    public ResponseEntity<MeetingResponse> createMeeting(@AuthenticationPrincipal User user,
                                                           @Valid @RequestBody CreateMeetingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(meetingService.createMeeting(user, request));
    }

    /** Meetings across every companion, scoped to one calendar month - for the Meeting Calendar view. */
    @GetMapping
    public ResponseEntity<List<MeetingResponse>> listForMonth(@AuthenticationPrincipal User user,
                                                                @RequestParam int year,
                                                                @RequestParam int month) {
        return ResponseEntity.ok(meetingService.listForMonth(user, year, month));
    }
}
