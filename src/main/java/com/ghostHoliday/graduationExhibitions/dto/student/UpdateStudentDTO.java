package com.ghostHoliday.graduationExhibitions.dto.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStudentDTO{
    private String encryptedStudentId;
    private String encryptedTeamId;
    private String name;
    private String studentNumber;
    private String role;
    private String exhibitionYear;
}