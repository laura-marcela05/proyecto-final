package com.corhuila.gymtracker.dto;

import com.corhuila.gymtracker.entity.MuscleGroup;
import com.corhuila.gymtracker.entity.Session;
import com.corhuila.gymtracker.entity.SessionMuscleGroup;
import com.corhuila.gymtracker.entity.SessionStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** status and durationMinutes are derived here, from the two timestamps (BR-04), never stored. */
public record SessionResponse(
        UUID id,
        UUID studentId,
        SessionStatus status,
        Instant checkInAt,
        Instant checkOutAt,
        Long durationMinutes,
        List<MuscleGroup> muscleGroups) {

    public static SessionResponse from(Session session) {
        Instant checkOut = session.getCheckOutAt();
        SessionStatus status = checkOut == null ? SessionStatus.open : SessionStatus.closed;
        Long minutes = checkOut == null ? null : Duration.between(session.getCheckInAt(), checkOut).toMinutes();
        List<MuscleGroup> groups = session.getMuscleGroups().stream().map(SessionMuscleGroup::getMuscleGroup).toList();
        return new SessionResponse(session.getId(), session.getStudent().getId(), status,
                session.getCheckInAt(), checkOut, minutes, groups);
    }
}
