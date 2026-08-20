package com.example.nova.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 10, max = 128, message = "Password must be between 10 and 128 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).+$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one digit and one special character"
    )
    private String newPassword;

    @NotBlank(message = "Please confirm your new password")
    private String confirmPassword;

    @AssertTrue(message = "New password and confirmation do not match")
    public boolean isConfirmationMatching() {
        return newPassword == null || newPassword.equals(confirmPassword);
    }

    @AssertTrue(message = "New password must be different from the current password")
    public boolean isNewPasswordDifferentFromCurrent() {
        return currentPassword == null || newPassword == null || !currentPassword.equals(newPassword);
    }
}
