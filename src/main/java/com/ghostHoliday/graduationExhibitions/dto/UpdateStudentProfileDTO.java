package com.ghostHoliday.graduationExhibitions.dto;

import com.ghostHoliday.graduationExhibitions.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStudentProfileDTO {
    private String token;
    private String name;
    private String studentNumber;
    private String info;
    private Role role;
    private String githubUrl;
    private String studentEmail;
    private String studentBlog;
}
