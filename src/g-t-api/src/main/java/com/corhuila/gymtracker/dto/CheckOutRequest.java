package com.corhuila.gymtracker.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** PATCH /sessions/{sessionId}: the only transition is open -> closed, so the status must be "closed". */
public record CheckOutRequest(@NotNull @Pattern(regexp = "closed") String status) {
}
