package com.ghostHoliday.graduationExhibitions.controller;

import com.ghostHoliday.graduationExhibitions.dto.SaveStudentDTO;
import com.ghostHoliday.graduationExhibitions.service.StudentService;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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


}
