package com.corhuila.gymtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 50) String username,
        @NotEmpty @Size(max = 72) String password) {

    public LoginRequest {
        username = Trimmed.of(username);
    }
}
