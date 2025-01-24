package com.ghostHoliday.graduationExhibitions.dto;


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
