package com.ghostHoliday.graduationExhibitions.dto.post;

import com.ghostHoliday.graduationExhibitions.domain.Operation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStudentProfileByPostDTO {
    private String teamUuid;
    private String encryptedStudentId;
    private String githubUrl;
    private String studentEmail;
    private String studentBlog;
    private String info;
    private String profileImage;
    private Operation profileImageOperation;
}
