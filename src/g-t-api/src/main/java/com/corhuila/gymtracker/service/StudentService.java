package com.corhuila.gymtracker.service;

import com.corhuila.gymtracker.dto.StudentRequest;
import com.corhuila.gymtracker.dto.StudentResponse;
import com.corhuila.gymtracker.entity.Role;
import com.corhuila.gymtracker.entity.Student;
import com.corhuila.gymtracker.entity.User;
import com.corhuila.gymtracker.exception.ApiException;
import com.corhuila.gymtracker.exception.ErrorCode;
import com.corhuila.gymtracker.repository.SessionRepository;
import com.corhuila.gymtracker.repository.StudentRepository;
import com.corhuila.gymtracker.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {

    private final StudentRepository students;
    private final UserRepository users;
    private final SessionRepository sessions;

    public StudentService(StudentRepository students, UserRepository users, SessionRepository sessions) {
        this.students = students;
        this.users = users;
        this.sessions = sessions;
    }

    /** BR-12: a trainer sees only their own students, the admin sees all of them. */
    @Transactional(readOnly = true)
    public List<StudentResponse> list(UUID callerId, Role role) {
        List<Student> found = role == Role.ADMIN
                ? students.findAllByOrderByNameAscIdAsc()
                : students.findByTrainerIdOrderByNameAscIdAsc(callerId);
        return found.stream().map(StudentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse get(UUID studentId, UUID callerId, Role role) {
        return StudentResponse.from(requireAccessible(studentId, callerId, role));
    }

    /** The new student belongs to the caller, taken from the token (BR-12); the document is unique (BR-02). */
    @Transactional
    public StudentResponse create(UUID callerId, StudentRequest request) {
        // A token can outlive its user (a trainer deleted after logging in): that caller is no longer authenticated.
        User trainer = users.findById(callerId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
        requireAvailableDocument(students.existsByDocument(request.document()));
        return StudentResponse.from(students.save(new Student(trainer, request.name(), request.document())));
    }

    @Transactional
    public StudentResponse update(UUID studentId, UUID callerId, Role role, StudentRequest request) {
        Student student = requireAccessible(studentId, callerId, role);
        requireAvailableDocument(students.existsByDocumentAndIdNot(request.document(), studentId));
        student.setName(request.name());
        student.setDocument(request.document());
        return StudentResponse.from(student);
    }

    /** BR-10: a student with sessions cannot be deleted. */
    @Transactional
    public void delete(UUID studentId, UUID callerId, Role role) {
        Student student = requireAccessible(studentId, callerId, role);
        if (sessions.existsByStudentId(studentId)) {
            throw new ApiException(ErrorCode.STUDENT_HAS_SESSIONS);
        }
        students.delete(student);
    }

    /** Another trainer's student answers "not found", never "forbidden", so its existence is not revealed. */
    @Transactional(readOnly = true)
    public Student requireAccessible(UUID studentId, UUID callerId, Role role) {
        Student student = students.findById(studentId)
                .orElseThrow(() -> new ApiException(ErrorCode.STUDENT_NOT_FOUND));
        if (role == Role.TRAINER && !student.getTrainer().getId().equals(callerId)) {
            throw new ApiException(ErrorCode.STUDENT_NOT_FOUND);
        }
        return student;
    }

    private void requireAvailableDocument(boolean alreadyRegistered) {
        if (alreadyRegistered) {
            throw new ApiException(ErrorCode.DOCUMENT_ALREADY_REGISTERED);
        }
    }
}
