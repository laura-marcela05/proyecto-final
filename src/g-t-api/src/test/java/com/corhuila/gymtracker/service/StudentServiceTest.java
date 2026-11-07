package com.corhuila.gymtracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.corhuila.gymtracker.dto.StudentRequest;
import com.corhuila.gymtracker.entity.Role;
import com.corhuila.gymtracker.entity.User;
import com.corhuila.gymtracker.exception.ApiException;
import com.corhuila.gymtracker.exception.ErrorCode;
import com.corhuila.gymtracker.repository.SessionRepository;
import com.corhuila.gymtracker.repository.StudentRepository;
import com.corhuila.gymtracker.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository students;

    @Mock
    private UserRepository users;

    @Mock
    private SessionRepository sessions;

    @InjectMocks
    private StudentService studentService;

    @Test
    void createStudent_whenDocumentTaken_isRejected() {
        UUID trainerId = UUID.randomUUID();
        when(users.findById(trainerId)).thenReturn(Optional.of(new User("trainer1", "Laura Gómez", "hash", Role.TRAINER)));
        when(students.existsByDocument("1082345678")).thenReturn(true);

        assertThatThrownBy(() -> studentService.create(trainerId, new StudentRequest("Ana Torres", "1082345678")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode())
                        .isEqualTo(ErrorCode.DOCUMENT_ALREADY_REGISTERED));
        verify(students, never()).save(any());
    }
}
