package com.corhuila.gymtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of both POST and PUT: the trainer and the sessions are not part of it (BR-12). */
public record StudentRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 20) String document) {

    public StudentRequest {
        name = Trimmed.of(name);
        document = Trimmed.of(document);
    }
}
