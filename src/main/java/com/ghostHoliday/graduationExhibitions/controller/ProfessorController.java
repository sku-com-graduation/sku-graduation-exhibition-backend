package com.ghostHoliday.graduationExhibitions.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.ghostHoliday.graduationExhibitions.dto.professor.FindProfessorDTO;
import com.ghostHoliday.graduationExhibitions.dto.professor.RegistProfessorDTO;
import com.ghostHoliday.graduationExhibitions.exception.UnauthorizedException;
import com.ghostHoliday.graduationExhibitions.service.HttpOnlyService;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import com.ghostHoliday.graduationExhibitions.service.ProfessorService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/admin/professor")
public class ProfessorController {
    private final ProfessorService professorService;
    private final JwtUtility jwtUtility;
    private final HttpOnlyService httpOnlyService;

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/regist")
    public ResponseEntity<String> registProfessor(
            HttpServletRequest request,
            HttpServletResponse response,  // accessToken 재발급을 위해 추가
            @ModelAttribute RegistProfessorDTO dto){
                try {
                    // 서비스 계층에서 accessToken 검증 및 재발급 처리
                    String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);


                    // 학생들 리스트를 서비스로 전달하여 처리
                    professorService.registProfessor(dto);
                    // 정상 처리되었으면 성공 메시지 반환
                    return ResponseEntity.ok("교수 정보 생성 성공");

                } catch (UnauthorizedException e) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
                } catch (Exception e) {
                    // 예외가 발생하면 적절한 에러 메시지 반환
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("교수 정보 생성 실패: " + e.getMessage());

                }
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/update")
    public ResponseEntity<String> updateProfessor(
            HttpServletRequest request,
            HttpServletResponse response,  // accessToken 재발급을 위해 추가
            @ModelAttribute UpdateProfessorDTO dto) {


        try {

            // 서비스 계층에서 accessToken 검증 및 재발급 처리
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);

            // 교수 정보 업데이트
            professorService.updateProfessor(dto);
            return ResponseEntity.ok("교수 정보 변경 성공");

        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid JSON format");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("교수 정보 변경 실패: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<?> findAllProfessors(
            HttpServletRequest request,
            HttpServletResponse response )
    {
        try {
            // 서비스 계층에서 accessToken 검증 및 재발급 처리
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);

            List<FindProfessorDTO> professors = professorService.findAllProfessors();
            return ResponseEntity.ok(professors);

        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("잘못된 요청입니다: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("서버 내부 오류가 발생했습니다.");
        }
    }




    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteStudents(
            HttpServletRequest request,
            HttpServletResponse response,  // accessToken 재발급을 위해 추가
            @RequestBody List<String> encryptedProfessorIds) {
        try {

            // 서비스 계층에서 accessToken 검증 및 재발급 처리
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);

            // 학생 삭제 로직 호출
            professorService.deleteProfessors(encryptedProfessorIds);

            // 성공적인 처리 후 응답
            return ResponseEntity.ok("교수 정보가 성공적으로 삭제되었습니다.");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        }  catch (IllegalArgumentException | JwtException e) {
            // 예외 발생 시, 에러 메시지와 함께 500 응답
            return ResponseEntity.status(401).body("인증 오류가 발생했습니다: " + e.getMessage());
        } catch (Exception e){
            return ResponseEntity.status(500).body("교수 정보 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProfessorDTO{
        private String encryptedProfessorId;
        private String name;
        private String email;
        private boolean tenure;
        private MultipartFile profileImage;

    }
}
