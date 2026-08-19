package com.example.nova.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/** "Company / Team" account type - steps 2 ("Company") + 3 ("Admin Account") of the signup wizard. */
@Data
public class CompanySignupRequest {

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String companyName;

    @NotBlank(message = "Company domain is required")
    @Size(min = 3, max = 100)
    @Pattern(
            regexp = "^[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$",
            message = "Company domain must be a valid domain, e.g. yourcompany.com"
    )
    private String companyDomain;

    @NotBlank(message = "Admin name is required")
    @Size(min = 2, max = 100, message = "Admin name must be between 2 and 100 characters")
    private String adminName;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Admin email must be a valid email address")
    @Size(max = 100)
    private String adminEmail;

    @NotBlank(message = "Password is required")
    @Size(min = 10, max = 128, message = "Password must be between 10 and 128 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).+$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one digit and one special character"
    )
    private String adminPassword;
}
