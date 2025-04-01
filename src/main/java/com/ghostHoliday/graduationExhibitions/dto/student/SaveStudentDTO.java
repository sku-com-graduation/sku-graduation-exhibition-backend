package com.ghostHoliday.graduationExhibitions.dto.student;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaveStudentDTO {
    private String studentName;
    private String studentNumber;
}
