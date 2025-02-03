package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.Professor;
import com.ghostHoliday.graduationExhibitions.dto.FindProfessorDTO;
import com.ghostHoliday.graduationExhibitions.dto.RegistProfessorDTO;
import com.ghostHoliday.graduationExhibitions.dto.UpdateProfessorDTO;
import com.ghostHoliday.graduationExhibitions.repository.ProfessorRepository;
import com.ghostHoliday.graduationExhibitions.utility.FileUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfessorService {

    private final ProfessorRepository professorRepository;
    private final FileUtility fileUtility;

    @Transactional
    public void registProfessor(RegistProfessorDTO dto) throws IOException {
        
        // 교수 객체 생성
        Professor professor = new Professor();
        professor.setName(dto.getName());
        professor.setEmail(dto.getEmail());
        professor.setTenure(dto.isTenure());
        MultipartFile profileImage = dto.getProfileImage();

        // 프로필 이미지 처리 (파일이 존재하는 경우)
        if (profileImage != null && !profileImage.isEmpty()) {
            String profileImagePath = saveProfessorImage(profileImage);
            professor.setImageUrl(profileImagePath);
        }


        // 교수 이미지 저장 경로 설정
        String uploadDir = "ProfessorImage";

        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 파일 확장자 추출 (jpg 또는 png)
        String extension = fileUtility.getImageFileExtension(profileImage .getOriginalFilename());
        if (extension == null) {
            throw new RuntimeException("지원되지 않는 파일 형식입니다.");
        }
        professorRepository.save(professor);
    }


    @Transactional
    public void updateProfessor(UpdateProfessorDTO dto, MultipartFile professorImage) throws IOException {
        // 기존 교수 정보를 찾아옵니다.
        Professor professor = professorRepository.findById(dto.getProfessorId())
                .orElseThrow(() -> new RuntimeException("교수 정보를 찾을 수 없습니다."));

        // 교수 정보 업데이트
        professor.setName(dto.getName());
        professor.setEmail(dto.getEmail());
        professor.setTenure(dto.isTenure());

        // 기존 이미지 경로 확인 후 삭제 (경로가 있는 경우에만 삭제)
        String existingImagePath = professor.getImageUrl();
        if (existingImagePath != null && !existingImagePath.isEmpty()) {
            deleteImage(existingImagePath);
        }

        // 프로필 이미지 처리 (파일이 존재하는 경우)
        if (professorImage != null && !professorImage.isEmpty()) {
            // 새로운 이미지 저장
            String profileImagePath = saveProfessorImage(professorImage);
            professor.setImageUrl(profileImagePath);
        }

        // 교수 정보 업데이트
        professorRepository.save(professor);
    }

    // 이미지 삭제 메소드
    private void deleteImage(String imagePath) throws IOException {
        if (imagePath != null && !imagePath.isEmpty()) {
            Path path = Paths.get(imagePath);
            Files.deleteIfExists(path);  // 파일이 존재하면 삭제
        }
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
        String extension = fileUtility.getImageFileExtension(professorImage.getOriginalFilename());
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


    public List<FindProfessorDTO> findAllProfessors() {

        return professorRepository.findAll().stream()
                .map(this::convertToFindProfessorDTO)
                .collect(Collectors.toList());


    }

    private FindProfessorDTO convertToFindProfessorDTO(Professor professor) {
        FindProfessorDTO dto = new FindProfessorDTO();
        dto.setProfessorId(professor.getId());
        dto.setName(professor.getName());
        dto.setEmail(professor.getEmail());
        dto.setTenure(professor.isTenure());

        // 이미지 경로가 있다면 해당 이미지를 Base64로 변환
        if (professor.getImageUrl() != null) {
            try {
                String base64Image = convertImageToBase64(professor.getImageUrl());
                dto.setProfileImg(base64Image);
            } catch (IOException e) {
                e.printStackTrace();
                // 예외 처리, 기본 이미지 또는 null 반환
                dto.setProfileImg(null);
            }
        }
        return dto;
    }

    // 이미지 경로를 받아 Base64로 변환하는 메소드
    private String convertImageToBase64(String imagePath) throws IOException {
        File imageFile = new File(imagePath);
        FileInputStream fileInputStream = new FileInputStream(imageFile);

        byte[] imageBytes = fileInputStream.readAllBytes();
        fileInputStream.close();

        return Base64.getEncoder().encodeToString(imageBytes);
    }


    @Transactional
    public void deleteProfessors(List<Long> ids) {
        for (Long id : ids) {
            // 교수 정보 가져오기
            Professor professor = professorRepository.findById(id).get();

            // 이미지 파일 경로 가져오기
            String imagePath = professor.getImageUrl();
            if (imagePath != null && !imagePath.isEmpty()) {
                // 이미지 파일 경로에서 파일을 삭제
                Path path = Paths.get(imagePath);
                try {
                    Files.deleteIfExists(path);  // 이미지 파일 삭제
                } catch (IOException e) {
                    throw new RuntimeException("Failed to delete image file: " + imagePath, e);
                }
            }

            // 교수 객체 삭제
            professorRepository.delete(professor);
        }
    }

}
