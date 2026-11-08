package com.corhuila.gymtracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.corhuila.gymtracker.dto.StudentRequest;
import com.corhuila.gymtracker.entity.Role;
import com.corhuila.gymtracker.entity.Student;
import com.corhuila.gymtracker.dto.StudentResponse;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;
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

    @Test
    void createStudent_withValidDocument_assignsCallerAsTrainer() {
        UUID trainerId = UUID.randomUUID();
        User trainer = trainer(trainerId);
        when(users.findById(trainerId)).thenReturn(Optional.of(trainer));
        when(students.save(any(Student.class))).thenAnswer(invocation -> {
            Student saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            return saved;
        });

        StudentResponse result = studentService.create(trainerId, new StudentRequest("Ana Torres", "1082345678"));

        assertThat(result.id()).isNotNull();
        assertThat(result.name()).isEqualTo("Ana Torres");
        assertThat(result.document()).isEqualTo("1082345678");
        assertThat(result.trainerId()).isEqualTo(trainerId);
        verify(students).save(org.mockito.ArgumentMatchers.argThat(saved -> saved.getTrainer() == trainer));
    }

    @Test
    void createStudent_whenCallerNoLongerExists_isRejected() {
        UUID callerId = UUID.randomUUID();
        when(users.findById(callerId)).thenReturn(Optional.empty());

        assertError(() -> studentService.create(callerId, new StudentRequest("Ana", "123")),
                ErrorCode.UNAUTHENTICATED);
        verify(students, never()).save(any());
    }

    @Test
    void listStudents_asTrainer_usesOnlyCallerStudents() {
        UUID callerId = UUID.randomUUID();
        Student own = student(UUID.randomUUID(), callerId);
        when(students.findByTrainerIdOrderByNameAscIdAsc(callerId)).thenReturn(List.of(own));

        assertThat(studentService.list(callerId, Role.TRAINER))
                .extracting(StudentResponse::id).containsExactly(own.getId());
        verify(students, never()).findAllByOrderByNameAscIdAsc();
    }

    @Test
    void listStudents_asAdmin_includesDifferentTrainers() {
        Student first = student(UUID.randomUUID(), UUID.randomUUID());
        Student second = student(UUID.randomUUID(), UUID.randomUUID());
        when(students.findAllByOrderByNameAscIdAsc()).thenReturn(List.of(first, second));

        assertThat(studentService.list(UUID.randomUUID(), Role.ADMIN))
                .extracting(StudentResponse::id).containsExactly(first.getId(), second.getId());
    }

    @Test
    void getStudent_asAnotherTrainer_hidesItsExistence() {
        UUID studentId = UUID.randomUUID();
        when(students.findById(studentId)).thenReturn(Optional.of(student(studentId, UUID.randomUUID())));

        assertError(() -> studentService.get(studentId, UUID.randomUUID(), Role.TRAINER),
                ErrorCode.STUDENT_NOT_FOUND);
    }

    @Test
    void getStudent_whenMissing_returnsNotFound() {
        UUID studentId = UUID.randomUUID();
        when(students.findById(studentId)).thenReturn(Optional.empty());

        assertError(() -> studentService.get(studentId, UUID.randomUUID(), Role.ADMIN),
                ErrorCode.STUDENT_NOT_FOUND);
    }

    @Test
    void updateStudent_asOwner_changesDetailsAndKeepsTrainer() {
        UUID trainerId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Student existing = student(studentId, trainerId);
        when(students.findById(studentId)).thenReturn(Optional.of(existing));

        StudentResponse result = studentService.update(studentId, trainerId, Role.TRAINER,
                new StudentRequest("Updated name", "987654321"));

        assertThat(result.name()).isEqualTo("Updated name");
        assertThat(result.document()).isEqualTo("987654321");
        assertThat(result.trainerId()).isEqualTo(trainerId);
        assertThat(existing.getName()).isEqualTo("Updated name");
        assertThat(existing.getDocument()).isEqualTo("987654321");
        verify(students).existsByDocumentAndIdNot("987654321", studentId);
    }

    @Test
    void updateStudent_withDuplicateDocument_doesNotChangeDetails() {
        UUID trainerId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Student existing = student(studentId, trainerId);
        when(students.findById(studentId)).thenReturn(Optional.of(existing));
        when(students.existsByDocumentAndIdNot("taken", studentId)).thenReturn(true);

        assertError(() -> studentService.update(studentId, trainerId, Role.TRAINER,
                new StudentRequest("Changed", "taken")), ErrorCode.DOCUMENT_ALREADY_REGISTERED);
        assertThat(existing.getName()).isEqualTo("Ana Torres");
        assertThat(existing.getDocument()).isEqualTo("1082345678");
    }

    @Test
    void updateStudent_asAnotherTrainer_doesNotChangeDetails() {
        UUID studentId = UUID.randomUUID();
        Student existing = student(studentId, UUID.randomUUID());
        when(students.findById(studentId)).thenReturn(Optional.of(existing));

        assertError(() -> studentService.update(studentId, UUID.randomUUID(), Role.TRAINER,
                new StudentRequest("Changed", "987")), ErrorCode.STUDENT_NOT_FOUND);
        assertThat(existing.getName()).isEqualTo("Ana Torres");
        assertThat(existing.getDocument()).isEqualTo("1082345678");
    }

    @Test
    void deleteStudent_withSessions_isRejected() {
        UUID studentId = UUID.randomUUID();
        Student existing = student(studentId, UUID.randomUUID());
        when(students.findById(studentId)).thenReturn(Optional.of(existing));
        when(sessions.existsByStudentId(studentId)).thenReturn(true);

        assertError(() -> studentService.delete(studentId, UUID.randomUUID(), Role.ADMIN),
                ErrorCode.STUDENT_HAS_SESSIONS);
        verify(students, never()).delete(any(Student.class));
    }

    @Test
    void deleteStudent_withoutSessions_deletesStudent() {
        UUID studentId = UUID.randomUUID();
        Student existing = student(studentId, UUID.randomUUID());
        when(students.findById(studentId)).thenReturn(Optional.of(existing));

        studentService.delete(studentId, UUID.randomUUID(), Role.ADMIN);

        verify(students).delete(existing);
    }

    private static User trainer(UUID id) {
        User trainer = new User("trainer1", "Trainer One", "test-hash", Role.TRAINER);
        ReflectionTestUtils.setField(trainer, "id", id);
        return trainer;
    }

    private static Student student(UUID id, UUID trainerId) {
        Student student = new Student(trainer(trainerId), "Ana Torres", "1082345678");
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }

    private static void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable action,
                                    ErrorCode code) {
        assertThatThrownBy(action).isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(code));
    }
}
