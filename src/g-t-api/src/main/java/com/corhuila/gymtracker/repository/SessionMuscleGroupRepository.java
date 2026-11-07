package com.corhuila.gymtracker.repository;

import com.corhuila.gymtracker.entity.MuscleGroup;
import com.corhuila.gymtracker.entity.SessionMuscleGroup;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionMuscleGroupRepository extends JpaRepository<SessionMuscleGroup, UUID> {

    boolean existsBySessionId(UUID sessionId);

    boolean existsBySessionIdAndMuscleGroup(UUID sessionId, MuscleGroup muscleGroup);
}
