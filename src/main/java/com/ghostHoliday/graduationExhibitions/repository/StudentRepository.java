package com.ghostHoliday.graduationExhibitions.repository;

import com.ghostHoliday.graduationExhibitions.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository <Student, Long> {

}