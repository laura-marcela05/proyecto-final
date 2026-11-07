package com.corhuila.gymtracker.dto;

import com.corhuila.gymtracker.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Size(max = 50) String username,
        @NotNull @Size(min = 8, max = 72) String password,
        @NotNull Role role) {

    public UserCreateRequest {
        fullName = Trimmed.of(fullName);
        username = Trimmed.of(username);
    }
}
