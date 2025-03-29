package com.ghostHoliday.graduationExhibitions.controller;

import com.ghostHoliday.graduationExhibitions.dto.SaveStudentDTO;
import com.ghostHoliday.graduationExhibitions.dto.SearchStudentDTO;
import com.ghostHoliday.graduationExhibitions.dto.UpdateStudentDTO;
import com.ghostHoliday.graduationExhibitions.exception.UnauthorizedException;
import com.ghostHoliday.graduationExhibitions.service.EncryptionService;
import com.ghostHoliday.graduationExhibitions.service.HttpOnlyService;
import com.ghostHoliday.graduationExhibitions.service.StudentService;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.expression.Ids;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/admin/student")
public class StudentController {
    private final StudentService studentService;
    private final EncryptionService encryptionService;
    private final HttpOnlyService httpOnlyService;

    // CSV 파일을 받아서 처리하는 메소드
    @PostMapping("/save")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<String> saveStudents(
            HttpServletRequest request,
            HttpServletResponse response,  // accessToken 재발급을 위해 추가
            @RequestParam("file") MultipartFile file) {
        try {

            // 서비스 계층에서 accessToken 검증 및 재발급 처리
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);


            // CSV 파일을 OpenCSV로 읽음
            try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream() , StandardCharsets.UTF_8))) {
                List<String[]> rows = csvReader.readAll(); // 파일의 모든 내용을 읽어옴

                // CSV 파일에서 각 행을 SaveStudentDTO로 변환
                List<SaveStudentDTO> dto = rows.stream()
                        .map(row -> new SaveStudentDTO(row[0], row[1])) // 각 행을 SaveStudentDTO로 변환
                        .collect(Collectors.toList());

                // 변환된 DTO 객체들을 데이터베이스에 저장
                studentService.saveStudents(dto);
            }

            // 저장 완료 메시지 반환
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("학생 정보가 성공적으로 저장되었습니다.");
        } catch (UnauthorizedException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        }
        catch (Exception e) {
            // 예외 발생 시 실패 메시지 반환
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("학생 정보 저장 중 오류가 발생했습니다. 파일 형식 또는 내용 확인을 해주세요.");
        }
    }
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteStudents(
            HttpServletRequest request,
            HttpServletResponse response,  // accessToken 재발급을 위해 추가
            @RequestBody List<String> encryptedStudentIds) {

        ArrayList<Long> teamIds = new ArrayList<>();
        try {

            // 서비스 계층에서 accessToken 검증 및 재발급 처리
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);


            // 학생 삭제 로직 호출
            for (String encryptedTeamId : encryptedStudentIds) {
                Long accountId = encryptionService.decryptPrimaryKey(encryptedTeamId);
                teamIds.add(accountId);
            }
            studentService.deleteStudents(teamIds);

            // 성공적인 처리 후 응답
            return ResponseEntity.ok("학생들이 성공적으로 삭제되었습니다.");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        }catch (Exception e) {
            // 예외 발생 시, 에러 메시지와 함께 500 응답
            return ResponseEntity.status(500).body("학생 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @PutMapping("/update")
    public ResponseEntity<String> updateStudents(
            HttpServletRequest request,
            HttpServletResponse response,  // accessToken 재발급을 위해 추가
            @RequestBody List<UpdateStudentDTO> dto) {
        try {

            // 서비스 계층에서 accessToken 검증 및 재발급 처리
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);

            // 학생들 리스트를 서비스로 전달하여 처리
            studentService.updateStudents(dto);
            // 정상 처리되었으면 성공 메시지 반환
            return ResponseEntity.ok("학생 정보 업데이트 성공");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        } catch (Exception e) {
            // 예외가 발생하면 적절한 에러 메시지 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("학생 정보 업데이트 실패: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<?> searchStudents(
            HttpServletRequest request,
            HttpServletResponse response,  // accessToken 재발급을 위해 추가
            @RequestParam("year") String year) {

        try {
            // 서비스 계층에서 accessToken 검증 및 재발급 처리
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);

            // 학생 리스트를 조회하는 로직 (Service 호출)
            List<SearchStudentDTO> students = studentService.searchStudentsByYear(year);

            if (students.isEmpty()) {
                // 학생들이 없을 때는 204 상태 코드와 함께 빈 리스트 반환
                return ResponseEntity.noContent().build();
            }

            // 결과가 있으면 200 OK와 함께 응답
            return ResponseEntity.ok(students);

        } catch (UnauthorizedException e) {
            // 오류 메시지를 ErrorResponse 객체로 래핑하여 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage()));
        } catch (Exception e) {
            // 예기치 않은 오류 처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(("서버 내부 오류가 발생했습니다."));
        }
    }



}
