package com.corhuila.gymtracker.controller;

import com.corhuila.gymtracker.dto.StudentRequest;
import com.corhuila.gymtracker.dto.StudentResponse;
import com.corhuila.gymtracker.security.AuthenticatedUser;
import com.corhuila.gymtracker.service.StudentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public List<StudentResponse> list(@AuthenticationPrincipal AuthenticatedUser caller) {
        return studentService.list(caller.id(), caller.role());
    }

    @PostMapping
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request,
                                                  @AuthenticationPrincipal AuthenticatedUser caller) {
        StudentResponse created = studentService.create(caller.id(), request);
        return ResponseEntity.created(URI.create("/api/v1/students/" + created.id())).body(created);
    }

    @GetMapping("/{studentId}")
    public StudentResponse get(@PathVariable UUID studentId, @AuthenticationPrincipal AuthenticatedUser caller) {
        return studentService.get(studentId, caller.id(), caller.role());
    }

    @PutMapping("/{studentId}")
    public StudentResponse update(@PathVariable UUID studentId, @Valid @RequestBody StudentRequest request,
                                  @AuthenticationPrincipal AuthenticatedUser caller) {
        return studentService.update(studentId, caller.id(), caller.role(), request);
    }

    @DeleteMapping("/{studentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID studentId, @AuthenticationPrincipal AuthenticatedUser caller) {
        studentService.delete(studentId, caller.id(), caller.role());
        return ResponseEntity.noContent().build();
    }
}
