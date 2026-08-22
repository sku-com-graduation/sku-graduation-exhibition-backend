package com.ghostHoliday.graduationExhibitions.controller;

import com.ghostHoliday.graduationExhibitions.dto.student.SaveStudentDTO;
import com.ghostHoliday.graduationExhibitions.dto.student.SearchStudentDTO;
import com.ghostHoliday.graduationExhibitions.dto.student.UpdateStudentDTO;
import com.ghostHoliday.graduationExhibitions.service.EncryptionService;

import com.ghostHoliday.graduationExhibitions.service.StudentService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/admin/student")
public class StudentController {
    private final StudentService studentService;
    private final EncryptionService encryptionService;

    // CSV 파일을 받아서 처리하는 메소드
    @PostMapping("/save")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> saveStudent(

            @RequestBody List<SaveStudentDTO> dto) {
        try {
                studentService.saveStudent(dto);

            // 저장 완료 메시지 반환
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("학생 정보가 성공적으로 저장되었습니다.");
        }  catch (Exception e) {
            // 예외 발생 시 실패 메시지 반환
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("학생 정보 저장 중 오류가 발생했습니다. 파일 형식 또는 내용 확인을 해주세요.");
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteStudents(

            @RequestBody List<String> encryptedStudentIds) {

        ArrayList<Long> teamIds = new ArrayList<>();
        try {

            // 학생 삭제 로직 호출
            for (String encryptedTeamId : encryptedStudentIds) {
                Long accountId = encryptionService.decryptPrimaryKey(encryptedTeamId);
                teamIds.add(accountId);
            }
            studentService.deleteStudents(teamIds);

            // 성공적인 처리 후 응답
            return ResponseEntity.ok("학생들이 성공적으로 삭제되었습니다.");
        }catch (Exception e) {
            // 예외 발생 시, 에러 메시지와 함께 500 응답
            return ResponseEntity.status(500).body("학생 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/update")
    public ResponseEntity<String> updateStudents(

            @RequestBody List<UpdateStudentDTO> dto) {
        try {

            // 학생들 리스트를 서비스로 전달하여 처리
            studentService.updateStudents(dto);
            // 정상 처리되었으면 성공 메시지 반환
            return ResponseEntity.ok("학생 정보 업데이트 성공");
        }catch (Exception e) {
            // 예외가 발생하면 적절한 에러 메시지 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("학생 정보 업데이트 실패: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<?> searchStudents(

            @RequestParam("year") String year){

        try {

            // 학생 리스트를 조회하는 로직 (Service 호출)
            SearchStudentDTO responses = studentService.searchStudentsByYear(year);

            // 결과가 있으면 200 OK와 함께 응답
            return ResponseEntity.ok(responses);

        }catch (Exception e) {
            // 예기치 않은 오류 처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(("서버 내부 오류가 발생했습니다."));
        }
    }

}
