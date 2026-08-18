package com.example.nova.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class CreateCompanionRequest {

    @Min(value = 1, message = "At least 1 companion is required")
    @Max(value = 20, message = "No more than 20 companions can be requested at once")
    private int quantity = 1;

    // One meeting-identity email per companion, in the same order as they'll be created.
    @NotEmpty(message = "At least one companion email is required")
    @Size(max = 20, message = "No more than 20 companions can be requested at once")
    private List<@NotBlank(message = "Email must not be blank") @Email(message = "Each email must be a valid email address") String> emails;

    @AssertTrue(message = "Number of emails must match the number of companions")
    public boolean isEmailCountValid() {
        return emails == null || quantity == emails.size();
    }
}
