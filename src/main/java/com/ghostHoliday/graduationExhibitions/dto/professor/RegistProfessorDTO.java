package com.ghostHoliday.graduationExhibitions.dto.professor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistProfessorDTO{
    private String name;
    private String email;
    private boolean tenure;
    private String profileImage;

}
