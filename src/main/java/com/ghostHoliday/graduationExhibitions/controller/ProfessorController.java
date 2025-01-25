package com.ghostHoliday.graduationExhibitions.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostHoliday.graduationExhibitions.dto.RegistProfessorDTO;
import com.ghostHoliday.graduationExhibitions.dto.UpdateStudentDTO;
import com.ghostHoliday.graduationExhibitions.dto.UpdateStudentProfileDTO;
import com.ghostHoliday.graduationExhibitions.service.ProfessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("professor")
public class ProfessorController {
    private final ProfessorService professorService;


    @PostMapping("/save")
    public ResponseEntity<String> registProfessor(
            @RequestParam("dto") String dtoJson,
            @RequestParam(value = "professorImage", required = false) MultipartFile professorImage ){



        // JSON 문자열을 DTO로 변환
        ObjectMapper objectMapper = new ObjectMapper();
        RegistProfessorDTO dto;

        try {
            dto = objectMapper.readValue(dtoJson, RegistProfessorDTO.class);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(400).body("Invalid JSON format");
        }

                try {
                    // 학생들 리스트를 서비스로 전달하여 처리
                    professorService.registProfessor(dto, professorImage);
                    // 정상 처리되었으면 성공 메시지 반환
                    return ResponseEntity.ok("교수 정보 생성 성공");

                } catch (Exception e) {
                    // 예외가 발생하면 적절한 에러 메시지 반환
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("교수 정보 생성 실패: " + e.getMessage());


            }
        }


}
