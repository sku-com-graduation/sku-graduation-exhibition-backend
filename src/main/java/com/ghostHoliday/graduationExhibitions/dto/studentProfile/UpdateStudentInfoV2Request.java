package com.ghostHoliday.graduationExhibitions.dto.studentProfile;

import com.ghostHoliday.graduationExhibitions.domain.FileType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateStudentInfoV2Request {
    private String encryptedStudentId;
    private FileType fileType;
    private String extension;
}
