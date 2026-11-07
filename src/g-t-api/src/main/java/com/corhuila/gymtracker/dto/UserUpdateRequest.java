package com.corhuila.gymtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The role is not here on purpose: it never changes (BR-03). An absent or empty password keeps the current one. */
public record UserUpdateRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Size(max = 50) String username,
        @Pattern(regexp = "(?s)(|.{8,72})") String password) {

    public UserUpdateRequest {
        fullName = Trimmed.of(fullName);
        username = Trimmed.of(username);
    }

    public boolean hasPassword() {
        return password != null && !password.isEmpty();
    }
}
