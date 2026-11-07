package com.corhuila.gymtracker.security;

import com.corhuila.gymtracker.entity.Role;
import java.util.UUID;

/** The caller, as read from a valid token: the principal of every authenticated request. */
public record AuthenticatedUser(UUID id, Role role) {
}
