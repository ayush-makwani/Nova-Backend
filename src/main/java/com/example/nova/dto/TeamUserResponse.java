package com.example.nova.dto;

import com.example.nova.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
public class TeamUserResponse {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private Set<Role> roles;
    private boolean currentUser; // true for the "YOU" badge; JSON key is "currentUser" (Jackson strips the is- getter prefix)
    private Long companionId;
    private String companionName;
    private String companionEmail; // null -> no companion assigned yet
    private Instant createdAt;
}
