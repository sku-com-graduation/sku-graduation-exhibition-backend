package com.ghostHoliday.graduationExhibitions.repository;

import com.ghostHoliday.graduationExhibitions.domain.Professor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {

    Optional<Professor> findByName(String professorName);
}
