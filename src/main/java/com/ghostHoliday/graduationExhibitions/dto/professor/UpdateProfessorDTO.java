package com.ghostHoliday.graduationExhibitions.dto.professor;

import com.ghostHoliday.graduationExhibitions.domain.Operation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfessorDTO{
    private String encryptedProfessorId;
    private String name;
    private String email;
    private boolean tenure;
    private String profileImage;
    private Operation profileImageOperation;
}
