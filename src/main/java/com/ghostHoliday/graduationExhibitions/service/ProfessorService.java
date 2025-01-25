package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.Professor;
import com.ghostHoliday.graduationExhibitions.dto.RegistProfessorDTO;
import com.ghostHoliday.graduationExhibitions.repository.ProfessorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfessorService {

    private final ProfessorRepository professorRepository;
    private final FileUtility fileUtility;

    @Transactional
    public void registProfessor( RegistProfessorDTO dto, MultipartFile professorImage) throws IOException {
        
        // 교수 객체 생성
        Professor professor = new Professor();
        professor.setName(dto.getName());
        professor.setEmail(dto.getEmail());
        professor.setTenure(dto.isTenure());


        // 프로필 이미지 처리 (파일이 존재하는 경우)
        if (professorImage != null && !professorImage.isEmpty()) {
            String profileImagePath = saveProfessorImage(professorImage);
            professor.setImageUrl(profileImagePath);
        }


        // 교수 이미지 저장 경로 설정
        String uploadDir = "ProfessorImage";

        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 파일 확장자 추출 (jpg 또는 png)
        String extension = fileUtility.getFileExtension(professorImage .getOriginalFilename());
        if (extension == null) {
            throw new RuntimeException("지원되지 않는 파일 형식입니다.");
        }



        professorRepository.save(professor);
    }


    // 프로필 이미지 저장 메소드
    private String saveProfessorImage(MultipartFile professorImage) throws IOException {
        // 저장할 디렉토리 경로
        String uploadDir = "ProfessorImage";
        File dir = new File(uploadDir);

        // 디렉토리가 없으면 생성
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 파일 확장자 추출
        String extension = fileUtility.getFileExtension(professorImage.getOriginalFilename());
        if (extension == null || (!extension.equalsIgnoreCase("jpg") && !extension.equalsIgnoreCase("png"))) {
            throw new RuntimeException("지원되지 않는 파일 형식입니다.");
        }

        // 고유한 파일 이름 생성 (UUID 사용)
        String fileName = UUID.randomUUID().toString() + "." + extension;

        // 파일을 실제 경로에 저장
        Path path = Paths.get(uploadDir, fileName);
        Files.write(path, professorImage.getBytes());

        // 저장된 파일 경로 반환
        return path.toString();
    }


}
