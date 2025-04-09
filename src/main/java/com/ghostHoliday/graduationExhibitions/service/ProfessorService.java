package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.Professor;
import com.ghostHoliday.graduationExhibitions.dto.professor.FindProfessorDTO;
import com.ghostHoliday.graduationExhibitions.dto.professor.RegistProfessorDTO;
import com.ghostHoliday.graduationExhibitions.dto.professor.UpdateProfessorDTO;
import com.ghostHoliday.graduationExhibitions.repository.ProfessorRepository;
import com.ghostHoliday.graduationExhibitions.repository.TeamRepository;
import com.ghostHoliday.graduationExhibitions.utility.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfessorService {

    private final ProfessorRepository professorRepository;
    private final S3Uploader s3Uploader;
    private final EncryptionService encryptionService;
    private final TeamRepository teamRepository;

    @Transactional
    public void registProfessor(RegistProfessorDTO dto) throws IOException {
        Professor professor = new Professor();
        professor.setName(dto.getName());
        professor.setEmail(dto.getEmail());
        professor.setTenure(dto.isTenure());

        MultipartFile profileImage = dto.getProfileImage();
        if (profileImage != null && !profileImage.isEmpty()) {
            String imageUrl = s3Uploader.upload(profileImage, "professor");
            professor.setImageUrl(imageUrl);
        }

        professorRepository.save(professor);
    }

    @Transactional
    public void updateProfessor(UpdateProfessorDTO dto) throws Exception {
        Professor professor = professorRepository.findById(
                        encryptionService.decryptPrimaryKey(dto.getEncryptedProfessorId()))
                .orElseThrow(() -> new RuntimeException("교수 정보를 찾을 수 없습니다."));

        professor.setName(dto.getName());
        professor.setEmail(dto.getEmail());
        professor.setTenure(dto.isTenure());

        MultipartFile profileImage = dto.getProfileImage();

        if (profileImage != null && !profileImage.isEmpty()) {
            // 새 이미지가 들어왔으면 기존 이미지 삭제 후 새 이미지 저장
            s3Uploader.delete(professor.getImageUrl());
            String imageUrl = s3Uploader.upload(profileImage, "professor");
            professor.setImageUrl(imageUrl);
        } else {
            // 이미지가 비어 있으면 기존 이미지 삭제 + 필드 null 처리
            if (professor.getImageUrl() != null && !professor.getImageUrl().isEmpty()) {
                s3Uploader.delete(professor.getImageUrl());
                professor.setImageUrl(null);
            }
        }

        professorRepository.save(professor);
    }


    public List<FindProfessorDTO> findAllProfessors() {
        return professorRepository.findAll().stream()
                .map(this::convertToFindProfessorDTO)
                .collect(Collectors.toList());
    }

    private FindProfessorDTO convertToFindProfessorDTO(Professor professor) {
        FindProfessorDTO dto = new FindProfessorDTO();
        try {
            dto.setEncryptedProfessorId(encryptionService.encryptPrimaryKey(professor.getId()));
            dto.setName(professor.getName());
            dto.setEmail(professor.getEmail());
            dto.setTenure(professor.isTenure());
            dto.setProfileImage(professor.getImageUrl());
        } catch (Exception e) {
            e.printStackTrace();
            dto.setProfileImage(null);
        }
        return dto;
    }

    @Transactional
    public void deleteProfessors(List<String> encryptedIds) throws Exception {
        for (String encryptedId : encryptedIds) {

            Long professorId = encryptionService.decryptPrimaryKey(encryptedId);
            Professor professor = professorRepository.findById(

                    encryptionService.decryptPrimaryKey(encryptedId)).get();


            // 해당 교수에 담당된 모든 팀에서 professorId를 NULL로 업데이트
            teamRepository.updateProfessorIdToNull(professorId);

            s3Uploader.delete(professor.getImageUrl());
            professorRepository.delete(professor);
        }
    }
}
