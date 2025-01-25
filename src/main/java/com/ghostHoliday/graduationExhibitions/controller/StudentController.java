package com.ghostHoliday.graduationExhibitions.controller;

import com.ghostHoliday.graduationExhibitions.dto.SaveStudentDTO;
import com.ghostHoliday.graduationExhibitions.dto.UpdateStudentDTO;
import com.ghostHoliday.graduationExhibitions.service.StudentService;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("student")
public class StudentController {
    private final StudentService studentService;

    // CSV 파일을 받아서 처리하는 메소드
    @PostMapping("/save")
    public ResponseEntity<String> saveStudents(@RequestParam("file") MultipartFile file) {
        try {
            // CSV 파일을 OpenCSV로 읽음
            try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
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
        } catch (Exception e) {
            // 예외 발생 시 실패 메시지 반환
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("학생 정보 저장 중 오류가 발생했습니다. 파일 형식 또는 내용 확인을 해주세요.");
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteStudents(@RequestBody List<Long> ids) {
        try {
            // 학생 삭제 로직 호출
            studentService.deleteStudents(ids);

            // 성공적인 처리 후 응답
            return ResponseEntity.ok("학생들이 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            // 예외 발생 시, 에러 메시지와 함께 500 응답
            return ResponseEntity.status(500).body("학생 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }


    @PatchMapping("/update")
    public ResponseEntity<String> updateStudents(@RequestBody List<UpdateStudentDTO> dto) {
        try {
            // 학생들 리스트를 서비스로 전달하여 처리
            studentService.updateStudents(dto);
            // 정상 처리되었으면 성공 메시지 반환
            return ResponseEntity.ok("학생 정보 업데이트 성공");
        } catch (Exception e) {
            // 예외가 발생하면 적절한 에러 메시지 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("학생 정보 업데이트 실패: " + e.getMessage());
        }
    }



}
