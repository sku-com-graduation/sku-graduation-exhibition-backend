package com.ghostHoliday.graduationExhibitions.service;


import com.ghostHoliday.graduationExhibitions.domain.Student;
import com.ghostHoliday.graduationExhibitions.domain.StudentProfile;
import com.ghostHoliday.graduationExhibitions.dto.UpdateStudentProfileDTO;
import com.ghostHoliday.graduationExhibitions.repository.StudentProfileRepository;
import com.ghostHoliday.graduationExhibitions.repository.StudentRepository;
import com.ghostHoliday.graduationExhibitions.utility.FileUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final StudentRepository studentRepository;
    private final FileUtility fileUtility;

    @Transactional
    public StudentProfile saveStudentProfile(){
        StudentProfile studentProfile = studentProfileRepository.save(new StudentProfile());
        return studentProfile;
    }

    @Transactional
    public StudentProfile updateStudentProfile(UpdateStudentProfileDTO dto, MultipartFile profileImage){

        // 학번으로 학생 조회
        Student student = studentRepository.findByStudentNumber(dto.getStudentNumber());

        if (student == null){
            throw new RuntimeException("해당 학번 없음");
        }

        StudentProfile studentProfile = student.getStudentProfile();

        // DTO에서 전달된 값으로 프로필 업데이트
        if (dto.getInfo() != null) {
            studentProfile.setInfo(dto.getInfo());
        }
        if (dto.getGithubUrl() != null) {
            studentProfile.setGithubUrl(dto.getGithubUrl());
        }
        if (dto.getStudentEmail() != null) {
            studentProfile.setStudentEmail(dto.getStudentEmail());
        }
        if (dto.getStudentBlog() != null) {
            studentProfile.setStudentBlog(dto.getStudentBlog());
        }


        // 프로필 이미지 처리 (파일이 존재하는 경우)
        if (profileImage != null && !profileImage.isEmpty()) {
            String profileImagePath = saveProfileImage(profileImage, dto.getStudentNumber());
            studentProfile.setStudentProfileUrl(profileImagePath);
        }


        // 업데이트된 프로필 저장
        return studentProfileRepository.save(studentProfile);

    }


    private String saveProfileImage(MultipartFile profileImage, String studentNumber) {
        // 프로필 이미지 저장 경로 설정
        String uploadDir = "StudentProfileImage";  // 현재 작업 디렉토리 내 'StudentProfileImage' 폴더

        // 디렉토리가 존재하지 않으면 생성
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();  // 디렉토리 생성
        }

        // 파일 확장자 추출 (jpg 또는 png)
        String extension = fileUtility.getFileExtension(profileImage.getOriginalFilename());
        if (extension == null) {
            throw new RuntimeException("지원되지 않는 파일 형식입니다.");
        }

        // 학번을 파일명으로 사용하고, 확장자 추가
        String fileName = studentNumber + "." + extension;
        Path targetLocation = Paths.get(uploadDir, fileName);

        // 기존 파일이 존재하면 삭제
        File existingFile = targetLocation.toFile();
        if (existingFile.exists()) {
            boolean deleted = existingFile.delete();
            if (!deleted) {
                throw new RuntimeException("기존 파일 삭제 실패");
            }
        }

        try {
            // 파일을 지정한 위치로 저장
            Files.copy(profileImage.getInputStream(), targetLocation);
        } catch (IOException e) {
            throw new RuntimeException("Error saving profile image", e);
        }

        // 상대 경로를 반환
        return "/" + fileName;  // 상대 경로 반환
    }

}
