package com.corhuila.gymtracker.service;

import com.corhuila.gymtracker.dto.SessionResponse;
import com.corhuila.gymtracker.entity.MuscleGroup;
import com.corhuila.gymtracker.entity.Role;
import com.corhuila.gymtracker.entity.Session;
import com.corhuila.gymtracker.entity.SessionMuscleGroup;
import com.corhuila.gymtracker.entity.SessionStatus;
import com.corhuila.gymtracker.entity.Student;
import com.corhuila.gymtracker.exception.ApiException;
import com.corhuila.gymtracker.exception.ErrorCode;
import com.corhuila.gymtracker.repository.SessionMuscleGroupRepository;
import com.corhuila.gymtracker.repository.SessionRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private final SessionRepository sessions;
    private final SessionMuscleGroupRepository muscleGroups;
    private final StudentService studentService;

    public SessionService(SessionRepository sessions, SessionMuscleGroupRepository muscleGroups,
                          StudentService studentService) {
        this.sessions = sessions;
        this.muscleGroups = muscleGroups;
        this.studentService = studentService;
    }

    /** BR-07: one open session per student. Two simultaneous check-ins: the database index lets one through. */
    @Transactional
    public SessionResponse checkIn(UUID studentId, UUID callerId, Role role) {
        Student student = studentService.requireAccessible(studentId, callerId, role);
        if (sessions.existsByStudentIdAndCheckOutAtIsNull(studentId)) {
            throw new ApiException(ErrorCode.SESSION_ALREADY_OPEN);
        }
        try {
            return SessionResponse.from(sessions.saveAndFlush(new Session(student, Instant.now())));
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(ErrorCode.SESSION_ALREADY_OPEN);
        }
    }

    /** Newest first; {@code status} is "open", "closed" or null for every session. */
    @Transactional(readOnly = true)
    public List<SessionResponse> list(UUID studentId, UUID callerId, Role role, String status) {
        SessionStatus wanted = parseStatus(status);
        studentService.requireAccessible(studentId, callerId, role);
        return sessions.findByStudentIdOrderByCheckInAtDescIdAsc(studentId).stream()
                .map(SessionResponse::from)
                .filter(session -> wanted == null || session.status() == wanted)
                .toList();
    }

    /** HU-03: logging a group already logged is a no-op, so the call can be repeated safely. */
    @Transactional
    public SessionResponse logMuscleGroup(UUID sessionId, String muscleGroup, UUID callerId, Role role) {
        Session session = requireOwnSession(sessionId, callerId, role);
        requireOpen(session);
        MuscleGroup group = parseMuscleGroup(muscleGroup);
        if (!muscleGroups.existsBySessionIdAndMuscleGroup(sessionId, group)) {
            muscleGroups.saveAndFlush(new SessionMuscleGroup(session, group));
        }
        return SessionResponse.from(session);
    }

    /** BR-01: a session closes only with at least one muscle group; once closed it never reopens. */
    @Transactional
    public SessionResponse checkOut(UUID sessionId, UUID callerId, Role role) {
        Session session = requireOwnSession(sessionId, callerId, role);
        requireOpen(session);
        if (!muscleGroups.existsBySessionId(sessionId)) {
            throw new ApiException(ErrorCode.MUSCLE_GROUPS_REQUIRED);
        }
        session.setCheckOutAt(Instant.now());
        return SessionResponse.from(session);
    }

    /** HU-10: the admin removes a session; its muscle groups go with it (cascade in the database). */
    @Transactional
    public void delete(UUID sessionId, UUID callerId, Role role) {
        sessions.delete(requireOwnSession(sessionId, callerId, role));
    }

    private Session requireOwnSession(UUID sessionId, UUID callerId, Role role) {
        Session session = sessions.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
        if (role == Role.TRAINER && !session.getStudent().getTrainer().getId().equals(callerId)) {
            throw new ApiException(ErrorCode.SESSION_NOT_FOUND);
        }
        return session;
    }

    private void requireOpen(Session session) {
        if (session.getCheckOutAt() != null) {
            throw new ApiException(ErrorCode.SESSION_ALREADY_CLOSED);
        }
    }

    /** BR-05: only the closed set of six values is accepted. */
    private MuscleGroup parseMuscleGroup(String value) {
        return Arrays.stream(MuscleGroup.values())
                .filter(group -> group.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.MUSCLE_GROUP_NOT_ALLOWED));
    }

    private SessionStatus parseStatus(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(SessionStatus.values())
                .filter(status -> status.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR));
    }
}
