package com.ghostHoliday.graduationExhibitions.repository;

import com.ghostHoliday.graduationExhibitions.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository <Student, Long> {

    // studentNumber를 기준으로 학생을 찾는 메서드
    Student findByStudentNumber(String studentNumber);

}