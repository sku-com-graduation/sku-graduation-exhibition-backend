package com.ghostHoliday.graduationExhibitions.service;


import com.ghostHoliday.graduationExhibitions.domain.Role;
import com.ghostHoliday.graduationExhibitions.domain.Student;
import com.ghostHoliday.graduationExhibitions.domain.StudentProfile;
import com.ghostHoliday.graduationExhibitions.domain.Team;
import com.ghostHoliday.graduationExhibitions.dto.SaveStudentDTO;
import com.ghostHoliday.graduationExhibitions.dto.SearchStudentDTO;
import com.ghostHoliday.graduationExhibitions.dto.UpdateStudentDTO;
import com.ghostHoliday.graduationExhibitions.repository.StudentRepository;
import com.ghostHoliday.graduationExhibitions.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {
    private final StudentRepository studentRepository;
    private final StudentProfileService studentProfileService;
    private final TeamRepository teamRepository;

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

    @Transactional
    public void deleteStudents(List<Long> ids) {
        // 학생들을 삭제하는 로직
        List<Student> students = studentRepository.findAllById(ids);
        if (students.isEmpty()) {
            throw new RuntimeException("해당 학생들이 존재하지 않습니다.");
        }


        // 학생들의 프로필 이미지 삭제
        students.forEach(student -> {
            if (student.getStudentProfile() != null) {
                String imagePath = student.getStudentProfile().getStudentProfileUrl();
                deleteProfileImage(imagePath); // 이미지 삭제
            }
        });


        studentRepository.deleteAll(students);
    }


    // 프로필 이미지 삭제 메서드
    private void deleteProfileImage(String imagePath) {
        // 상대 경로로 저장된 이미지 경로가 들어옴
        if (imagePath != null && !imagePath.isEmpty()) {
            // 상대 경로를 기반으로 실제 파일 경로를 찾기 위해 애플리케이션 루트 경로를 기준으로 설정
            Path path = Paths.get("StudentProfileImage", imagePath);  // 상대 경로에 파일이 위치함
            File file = path.toFile();

            if (file.exists()) {
                // 파일이 존재하면 삭제
                boolean deleted = file.delete();
                if (!deleted) {
                    throw new RuntimeException("이미지 파일 삭제 실패");
                }
            }
        }
    }


    @Transactional
    public void updateStudents(List<UpdateStudentDTO> dto) {
        for (UpdateStudentDTO studentDTO : dto) {
            // 학생을 조회하여 업데이트
            Student student = studentRepository.findById(studentDTO.getStudentId())
                    .orElseThrow(() -> new RuntimeException("학생을 찾을 수 없습니다."));

            // Role Enum 처리 (Role 값이 비어있지 않으면 업데이트)
            if (studentDTO.getRole() != null && !studentDTO.getRole().isEmpty()) {
                Role role = Role.valueOf(studentDTO.getRole().toUpperCase());  // role을 Enum으로 변환
                student.setRole(role);  // Role을 Enum으로 설정
            }

            // Team 객체 처리 (teamId 값이 비어있지 않으면 팀 업데이트)
            if (studentDTO.getTeamId() != null) {
                Team team = teamRepository.findById(studentDTO.getTeamId())
                        .orElseThrow(() -> new RuntimeException("팀을 찾을 수 없습니다."));
                student.setTeam(team);  // Team 객체를 설정
            }

            // 각 항목들에 대해서 값이 비어있지 않으면 업데이트
            if (studentDTO.getName() != null && !studentDTO.getName().isEmpty()) {
                student.setName(studentDTO.getName());
            }

            if (studentDTO.getStudentNumber() != null && !studentDTO.getStudentNumber().isEmpty()) {
                student.setStudentNumber(studentDTO.getStudentNumber());
            }

            if (studentDTO.getExhibitionYear() != null && !studentDTO.getExhibitionYear().isEmpty()) {
                student.setExhibitionYear(studentDTO.getExhibitionYear());
            }

            // 변경된 학생 정보 저장
            studentRepository.save(student);
        }
    }

    public List<SearchStudentDTO> searchStudentsByYear(String exhibitionYear) {
        // exhibitionYear로 학생 검색
        List<Student> students = studentRepository.findByExhibitionYear(exhibitionYear);

        // Student -> SearchStudentDTO로 변환
        return students.stream()
                .map(student -> new SearchStudentDTO(
                        student.getId(),
                        student.getTeam() != null ? student.getTeam().getId() : null, // 팀 ID
                        student.getName(),
                        student.getStudentNumber(),
                        student.getRole().name(), // Role을 문자열로 변환
                        student.getExhibitionYear()
                ))
                .collect(Collectors.toList());
    }



}
