package com.ghostHoliday.graduationExhibitions.repository;

import com.ghostHoliday.graduationExhibitions.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository <Student, Long> {

    // studentNumber를 기준으로 학생을 찾는 메서드
    Student findByStudentNumber(String studentNumber);

    // exhibitionYear를 기준으로 학생 리스트를 반환
    List<Student> findByExhibitionYear(String exhibitionYear);
}