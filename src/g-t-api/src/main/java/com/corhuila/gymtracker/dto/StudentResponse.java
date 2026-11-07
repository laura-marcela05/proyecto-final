package com.corhuila.gymtracker.dto;

import com.corhuila.gymtracker.entity.Student;
import java.util.UUID;

public record StudentResponse(UUID id, String name, String document, UUID trainerId, String trainerName) {

    public static StudentResponse from(Student student) {
        return new StudentResponse(student.getId(), student.getName(), student.getDocument(),
                student.getTrainer().getId(), student.getTrainer().getFullName());
    }
}
