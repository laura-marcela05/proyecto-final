package com.corhuila.gymtracker.dto;

import com.corhuila.gymtracker.entity.Role;
import com.corhuila.gymtracker.entity.User;
import java.util.UUID;

/** Never carries the password or its hash. */
public record UserResponse(UUID id, String username, String fullName, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getRole());
    }
}
