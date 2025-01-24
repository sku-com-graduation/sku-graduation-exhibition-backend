package com.ghostHoliday.graduationExhibitions.service;


import com.ghostHoliday.graduationExhibitions.domain.Student;
import com.ghostHoliday.graduationExhibitions.domain.StudentProfile;
import com.ghostHoliday.graduationExhibitions.dto.SaveStudentDTO;
import com.ghostHoliday.graduationExhibitions.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {
    private final StudentRepository studentRepository;
    private final StudentProfileService studentProfileService;

    @Transactional
    public void saveStudents( List<SaveStudentDTO> dto) {

        // 학생 정보와 프로필을 저장
        List<Student> students = dto.stream()
                .map(saveStudentDTO -> {
                    // 빈 StudentProfile 생성
                    StudentProfile studentProfile = studentProfileService.saveStudentProfile();

                    // SaveStudentDTO를 Student로 변환
                    Student student = new Student();
                    student.setName(saveStudentDTO.getStudentName());
                    student.setStudentNumber(saveStudentDTO.getStudentNumber());
                    student.setStudentProfile(studentProfile); // Student와 StudentProfile 연관 설정

                    // exhibitionYear는 기본값으로 시스템의 현재 연도가 자동 설정됨
                    return student;
                })
                .collect(Collectors.toList());

        // Student 리스트 저장
        studentRepository.saveAll(students); // 여러 학생을 한 번에 저장
    }

}
