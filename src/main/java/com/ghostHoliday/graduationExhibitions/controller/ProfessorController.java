package com.ghostHoliday.graduationExhibitions.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostHoliday.graduationExhibitions.dto.FindProfessorDTO;
import com.ghostHoliday.graduationExhibitions.dto.RegistProfessorDTO;
import com.ghostHoliday.graduationExhibitions.service.JwtUtility;
import com.ghostHoliday.graduationExhibitions.service.ProfessorService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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
    private final JwtUtility jwtUtility;


    @PostMapping("/save")
    public ResponseEntity<String> registProfessor(
            @RequestParam("dto") String dtoJson,
            @RequestParam(value = "professorImage", required = false) MultipartFile professorImage ){

        try{
            // 토큰 검증
            // Claims claims = jwtUtility.validateToken(token); // 토큰 유효성 검사
        }
        catch (IllegalArgumentException | JwtException e) {
            // 토큰이 유효하지 않거나 검증 중 에러가 발생하면 401 Unauthorized 응답
            return ResponseEntity.status(401).body(null);  // 또는 적절한 오류 메시지
        }



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


    @GetMapping("/findAll")
    public ResponseEntity<List<FindProfessorDTO>> findAllProfessors(@RequestHeader("Authorization") String token) {
        // "Bearer " 부분을 제외한 실제 JWT 토큰만 추출
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);  // "Bearer " 길이만큼 잘라냄
        }

        try {
            // 토큰 검증
            // Claims claims = jwtUtility.validateToken(token); // 토큰 유효성 검사

            // 토큰이 유효하면 교수 목록을 반환
            List<FindProfessorDTO> professors = professorService.findAllProfessors();
            return ResponseEntity.ok(professors);

        } catch (IllegalArgumentException | JwtException e) {
            // 토큰이 유효하지 않거나 검증 중 에러가 발생하면 401 Unauthorized 응답
            return ResponseEntity.status(401).body(null);  // 또는 적절한 오류 메시지
        }
    }



    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteStudents(
            @RequestHeader("Authorization") String token,
            @RequestBody List<Long> ids) {

        // "Bearer " 부분을 제외한 실제 JWT 토큰만 추출
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);  // "Bearer " 길이만큼 잘라냄
        }

        try {

            // 토큰 검증
            // Claims claims = jwtUtility.validateToken(token); // 토큰 유효성 검사


            // 학생 삭제 로직 호출
            professorService.deleteProfessors(ids);

            // 성공적인 처리 후 응답
            return ResponseEntity.ok("교수 정보가 성공적으로 삭제되었습니다.");
        } catch (IllegalArgumentException | JwtException e) {
            // 예외 발생 시, 에러 메시지와 함께 500 응답
            return ResponseEntity.status(401).body("인증 오류가 발생했습니다: " + e.getMessage());
        } catch (Exception e){
            return ResponseEntity.status(500).body("교수 정보 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

}
