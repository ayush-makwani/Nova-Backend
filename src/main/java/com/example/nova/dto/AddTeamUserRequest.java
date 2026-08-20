package com.example.nova.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddTeamUserRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Temp password is required")
    @Size(min = 10, max = 128, message = "Temp password must be between 10 and 128 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).+$",
            message = "Temp password must contain at least one uppercase letter, one lowercase letter, one digit and one special character"
    )
    private String tempPassword;
}
