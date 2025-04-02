package com.ghostHoliday.graduationExhibitions.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostHoliday.graduationExhibitions.dto.studentProfile.UpdateStudentProfileDTO;
import com.ghostHoliday.graduationExhibitions.exception.UnauthorizedException;
import com.ghostHoliday.graduationExhibitions.service.HttpOnlyService;
import com.ghostHoliday.graduationExhibitions.service.StudentProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/admin")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StudentProfileService studentProfileService;
    private final HttpOnlyService httpOnlyService;

    @PutMapping("/updateProfile")
    public ResponseEntity<String> updateStudentProfile(
            HttpServletRequest request,
            HttpServletResponse response,  // accessToken 재발급을 위해 추가
            @RequestParam("dto") String dtoJson,  // JSON 데이터를 String으로 받기
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) {

        // JSON 문자열을 DTO로 변환
        ObjectMapper objectMapper = new ObjectMapper();
        UpdateStudentProfileDTO dto;
        try {
            dto = objectMapper.readValue(dtoJson, UpdateStudentProfileDTO.class);
        }catch (JsonProcessingException e) {
            return ResponseEntity.status(400).body("Invalid JSON format");
        }


        try {
            // 프로필 업데이트 서비스 호출
            studentProfileService.updateStudentProfile(dto, profileImage);

            // 성공적인 처리 후 응답
            return ResponseEntity.ok("Profile updated successfully");

        } catch (RuntimeException e) {
            // 예외 발생 시, 에러 메시지와 함께 400 응답
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        } catch (Exception e) {
            // 일반적인 예외 처리 (예: 서버 오류)
            return ResponseEntity.status(500).body("Internal server error: " + e.getMessage());
        }
    }
}
