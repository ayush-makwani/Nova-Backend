package com.example.nova.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.team-user")
public class TeamUserProperties {

    /** Placeholder login page linked from the welcome email; not wired to a real flow yet. */
    private String loginUrl = "http://localhost:3000/login";
}
