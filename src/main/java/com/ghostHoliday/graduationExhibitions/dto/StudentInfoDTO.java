package com.ghostHoliday.graduationExhibitions.dto;

import com.ghostHoliday.graduationExhibitions.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentInfoDTO {

    private String name;
    private String info;
    private Role role;
    private String githubUrl;
    private String studentEmail;
    private String studentBlog;
    private String profileImage;
}
