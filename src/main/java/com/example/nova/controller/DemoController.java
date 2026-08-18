package com.example.nova.controller;

import com.example.nova.entity.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Demonstrates protected + role-restricted endpoints secured by the JWT filter. */
@RestController
@RequestMapping("/api")
public class DemoController {

    @GetMapping("/user/dashboard")
    public Map<String, Object> userDashboard(@AuthenticationPrincipal User user) {
        return Map.of("message", "Welcome " + user.getUsername(), "roles", user.getAuthorities());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/dashboard")
    public Map<String, Object> adminDashboard(@AuthenticationPrincipal User user) {
        return Map.of("message", "Welcome admin " + user.getUsername());
    }
}
