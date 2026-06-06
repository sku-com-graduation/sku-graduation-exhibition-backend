package com.ghostHoliday.graduationExhibitions.dto.professor;

import com.ghostHoliday.graduationExhibitions.domain.FileType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfessorInfoV2 {
    private String encryptedProfessorId;
    private FileType fileType;
    private String contentType;
    private String extension;
}
