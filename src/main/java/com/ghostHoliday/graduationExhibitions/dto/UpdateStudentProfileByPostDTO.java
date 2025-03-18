package com.ghostHoliday.graduationExhibitions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStudentProfileByPostDTO {
    private String teamUuId;
    private String encryptedStudentId;
    private String githubUrl;
    private String studentEmail;
    private String studentBlog;
    private String info;
    private MultipartFile profileImage;
}
