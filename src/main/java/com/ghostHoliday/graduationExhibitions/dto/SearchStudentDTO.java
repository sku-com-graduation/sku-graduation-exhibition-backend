package com.ghostHoliday.graduationExhibitions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchStudentDTO{
    private Long studentId;
    private Long teamId;
    private String name;
    private String studentNumber;
    private String role;
    private String exhibitionYear;
}
