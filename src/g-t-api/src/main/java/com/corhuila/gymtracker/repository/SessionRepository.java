package com.corhuila.gymtracker.repository;

import com.corhuila.gymtracker.entity.Session;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    List<Session> findByStudentIdOrderByCheckInAtDescIdAsc(UUID studentId);

    boolean existsByStudentId(UUID studentId);

    boolean existsByStudentIdAndCheckOutAtIsNull(UUID studentId);
}
