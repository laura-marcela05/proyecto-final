package com.corhuila.gymtracker.dto;

import com.corhuila.gymtracker.entity.Role;

public record LoginResponse(String token, Role role, String fullName) {
}
