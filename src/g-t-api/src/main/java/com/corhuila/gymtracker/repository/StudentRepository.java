package com.corhuila.gymtracker.repository;

import com.corhuila.gymtracker.entity.Student;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, UUID> {

    boolean existsByDocument(String document);

    boolean existsByDocumentAndIdNot(String document, UUID id);

    boolean existsByTrainerId(UUID trainerId);

    @EntityGraph(attributePaths = "trainer")
    List<Student> findAllByOrderByNameAscIdAsc();

    @EntityGraph(attributePaths = "trainer")
    List<Student> findByTrainerIdOrderByNameAscIdAsc(UUID trainerId);
}
