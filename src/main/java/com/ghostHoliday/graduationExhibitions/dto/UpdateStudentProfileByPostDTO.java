package com.ghostHoliday.graduationExhibitions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStudentProfileByPostDTO {
    private String token;
    private String encryptedTeamId;
    private String encryptedStudentId;
    private String info;
    private String githubUrl;
    private String studentEmail;
    private String studentBlog;
}
