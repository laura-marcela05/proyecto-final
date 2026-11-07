package com.corhuila.gymtracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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
}
