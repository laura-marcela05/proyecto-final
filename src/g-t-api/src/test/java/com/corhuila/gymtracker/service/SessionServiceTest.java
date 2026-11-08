package com.corhuila.gymtracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import com.corhuila.gymtracker.dto.SessionResponse;
import com.corhuila.gymtracker.entity.MuscleGroup;
import com.corhuila.gymtracker.entity.SessionMuscleGroup;
import com.corhuila.gymtracker.entity.SessionStatus;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.corhuila.gymtracker.entity.Role;
import com.corhuila.gymtracker.entity.Session;
import com.corhuila.gymtracker.entity.Student;
import com.corhuila.gymtracker.entity.User;
import com.corhuila.gymtracker.exception.ApiException;
import com.corhuila.gymtracker.exception.ErrorCode;
import com.corhuila.gymtracker.repository.SessionMuscleGroupRepository;
import com.corhuila.gymtracker.repository.SessionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessions;

    @Mock
    private SessionMuscleGroupRepository muscleGroups;

    @Mock
    private StudentService studentService;

    @InjectMocks
    private SessionService sessionService;

    @Test
    void checkOut_withoutMuscleGroups_isRejected() {
        UUID sessionId = UUID.randomUUID();
        User trainer = new User("trainer1", "Laura Gómez", "hash", Role.TRAINER);
        Session openSession = new Session(new Student(trainer, "Ana Torres", "1082345678"), Instant.now());
        when(sessions.findById(sessionId)).thenReturn(Optional.of(openSession));
        when(muscleGroups.existsBySessionId(sessionId)).thenReturn(false);

        assertThatThrownBy(() -> sessionService.checkOut(sessionId, UUID.randomUUID(), Role.ADMIN))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(ErrorCode.MUSCLE_GROUPS_REQUIRED));
        assertThat(openSession.getCheckOutAt()).isNull();
    }

    @Test
    void checkIn_whenAlreadyOpen_rejectsWithoutSaving() {
        UUID studentId = UUID.randomUUID();
        UUID trainerId = UUID.randomUUID();
        when(studentService.requireAccessible(studentId, trainerId, Role.TRAINER))
                .thenReturn(student(studentId, trainerId));
        when(sessions.existsByStudentIdAndCheckOutAtIsNull(studentId)).thenReturn(true);

        assertError(() -> sessionService.checkIn(studentId, trainerId, Role.TRAINER), ErrorCode.SESSION_ALREADY_OPEN);
        verify(sessions, never()).saveAndFlush(any(Session.class));
    }

    @Test
    void checkIn_withNoOpenSession_createsOpenSession() {
        UUID studentId = UUID.randomUUID();
        UUID trainerId = UUID.randomUUID();
        when(studentService.requireAccessible(studentId, trainerId, Role.TRAINER))
                .thenReturn(student(studentId, trainerId));
        when(sessions.saveAndFlush(any(Session.class))).thenAnswer(invocation -> {
            Session saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            return saved;
        });

        SessionResponse result = sessionService.checkIn(studentId, trainerId, Role.TRAINER);

        assertThat(result.id()).isNotNull();
        assertThat(result.studentId()).isEqualTo(studentId);
        assertThat(result.status()).isEqualTo(SessionStatus.open);
        assertThat(result.checkOutAt()).isNull();
        assertThat(result.durationMinutes()).isNull();
    }

    @Test
    void checkIn_whenDatabaseRejectsInsert_translatesConflict() {
        UUID studentId = UUID.randomUUID();
        UUID trainerId = UUID.randomUUID();
        when(studentService.requireAccessible(studentId, trainerId, Role.TRAINER))
                .thenReturn(student(studentId, trainerId));
        when(sessions.saveAndFlush(any(Session.class)))
                .thenThrow(new DataIntegrityViolationException("simulated unique index violation"));

        assertError(() -> sessionService.checkIn(studentId, trainerId, Role.TRAINER), ErrorCode.SESSION_ALREADY_OPEN);
    }

    @Test
    void checkOut_withMuscleGroup_closesSession() {
        UUID trainerId = UUID.randomUUID();
        Session session = session(trainerId);
        when(sessions.findById(session.getId())).thenReturn(Optional.of(session));
        when(muscleGroups.existsBySessionId(session.getId())).thenReturn(true);
        Instant before = Instant.now();

        SessionResponse result = sessionService.checkOut(session.getId(), trainerId, Role.TRAINER);

        assertThat(result.status()).isEqualTo(SessionStatus.closed);
        assertThat(result.checkOutAt()).isBetween(before, Instant.now());
        assertThat(session.getCheckOutAt()).isEqualTo(result.checkOutAt());
        assertThat(result.durationMinutes()).isNotNegative();
    }

    @Test
    void checkOut_whenAlreadyClosed_keepsTimestamp() {
        UUID trainerId = UUID.randomUUID();
        Session session = session(trainerId);
        Instant closedAt = Instant.parse("2026-01-01T11:00:00Z");
        session.setCheckOutAt(closedAt);
        when(sessions.findById(session.getId())).thenReturn(Optional.of(session));

        assertError(() -> sessionService.checkOut(session.getId(), trainerId, Role.TRAINER), ErrorCode.SESSION_ALREADY_CLOSED);
        assertThat(session.getCheckOutAt()).isEqualTo(closedAt);
        verify(muscleGroups, never()).existsBySessionId(any());
    }

    @Test
    void responseForClosedSession_truncatesDurationToWholeMinutes() {
        Session session = session(UUID.randomUUID());
        session.setCheckOutAt(Instant.parse("2026-01-01T11:05:59Z"));

        assertThat(SessionResponse.from(session).durationMinutes()).isEqualTo(65L);
    }

    @Test
    void logMuscleGroup_whenMissing_savesRequestedGroup() {
        UUID trainerId = UUID.randomUUID();
        Session session = session(trainerId);
        when(sessions.findById(session.getId())).thenReturn(Optional.of(session));

        sessionService.logMuscleGroup(session.getId(), "chest", trainerId, Role.TRAINER);

        org.mockito.ArgumentCaptor<SessionMuscleGroup> saved = org.mockito.ArgumentCaptor.forClass(SessionMuscleGroup.class);
        verify(muscleGroups).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getMuscleGroup()).isEqualTo(MuscleGroup.chest);
    }

    @Test
    void logMuscleGroup_whenRepeated_doesNotInsertAgain() {
        UUID trainerId = UUID.randomUUID();
        Session session = session(trainerId);
        when(sessions.findById(session.getId())).thenReturn(Optional.of(session));
        when(muscleGroups.existsBySessionIdAndMuscleGroup(session.getId(), MuscleGroup.chest)).thenReturn(true);

        sessionService.logMuscleGroup(session.getId(), "chest", trainerId, Role.TRAINER);

        verify(muscleGroups, never()).saveAndFlush(any(SessionMuscleGroup.class));
    }

    @Test
    void logMuscleGroup_withUnknownGroup_isRejected() {
        UUID trainerId = UUID.randomUUID();
        Session session = session(trainerId);
        when(sessions.findById(session.getId())).thenReturn(Optional.of(session));

        assertError(() -> sessionService.logMuscleGroup(session.getId(), "invalid", trainerId, Role.TRAINER),
                ErrorCode.MUSCLE_GROUP_NOT_ALLOWED);
        verify(muscleGroups, never()).saveAndFlush(any(SessionMuscleGroup.class));
    }

    @Test
    void logMuscleGroup_whenClosed_isRejected() {
        UUID trainerId = UUID.randomUUID();
        Session session = session(trainerId);
        session.setCheckOutAt(Instant.parse("2026-01-01T11:00:00Z"));
        when(sessions.findById(session.getId())).thenReturn(Optional.of(session));

        assertError(() -> sessionService.logMuscleGroup(session.getId(), "chest", trainerId, Role.TRAINER),
                ErrorCode.SESSION_ALREADY_CLOSED);
        verify(muscleGroups, never()).saveAndFlush(any(SessionMuscleGroup.class));
    }

    @Test
    void checkOut_asAnotherTrainer_hidesSession() {
        Session session = session(UUID.randomUUID());
        when(sessions.findById(session.getId())).thenReturn(Optional.of(session));

        assertError(() -> sessionService.checkOut(session.getId(), UUID.randomUUID(), Role.TRAINER),
                ErrorCode.SESSION_NOT_FOUND);
        assertThat(session.getCheckOutAt()).isNull();
    }

    @Test
    void delete_whenMissing_returnsNotFound() {
        UUID id = UUID.randomUUID();
        when(sessions.findById(id)).thenReturn(Optional.empty());

        assertError(() -> sessionService.delete(id, UUID.randomUUID(), Role.ADMIN), ErrorCode.SESSION_NOT_FOUND);
        verify(sessions, never()).delete(any(Session.class));
    }

    @Test
    void delete_asAdmin_deletesSession() {
        Session session = session(UUID.randomUUID());
        when(sessions.findById(session.getId())).thenReturn(Optional.of(session));

        sessionService.delete(session.getId(), UUID.randomUUID(), Role.ADMIN);

        verify(sessions).delete(session);
    }

    @Test
    void list_withClosedFilter_returnsOnlyClosedSessions() {
        UUID trainerId = UUID.randomUUID();
        Student student = student(UUID.randomUUID(), trainerId);
        Session open = new Session(student, Instant.parse("2026-01-02T10:00:00Z"));
        Session closed = new Session(student, Instant.parse("2026-01-01T10:00:00Z"));
        closed.setCheckOutAt(Instant.parse("2026-01-01T11:00:00Z"));
        when(sessions.findByStudentIdOrderByCheckInAtDescIdAsc(student.getId())).thenReturn(List.of(open, closed));

        assertThat(sessionService.list(student.getId(), trainerId, Role.TRAINER, "closed"))
                .extracting(SessionResponse::status).containsExactly(SessionStatus.closed);
        verify(studentService).requireAccessible(student.getId(), trainerId, Role.TRAINER);
    }

    @Test
    void list_withInvalidStatus_isRejectedBeforeQuery() {
        assertError(() -> sessionService.list(UUID.randomUUID(), UUID.randomUUID(), Role.TRAINER, "invalid"),
                ErrorCode.VALIDATION_ERROR);
        verifyNoInteractions(sessions, studentService);
    }

    private static Student student(UUID id, UUID trainerId) {
        User trainer = new User("trainer1", "Trainer One", "test-hash", Role.TRAINER);
        ReflectionTestUtils.setField(trainer, "id", trainerId);
        Student student = new Student(trainer, "Ana Torres", "1082345678");
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }

    private static Session session(UUID trainerId) {
        Session session = new Session(student(UUID.randomUUID(), trainerId), Instant.parse("2026-01-01T10:00:00Z"));
        ReflectionTestUtils.setField(session, "id", UUID.randomUUID());
        return session;
    }

    private static void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, ErrorCode code) {
        assertThatThrownBy(action).isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(code));
    }
}
