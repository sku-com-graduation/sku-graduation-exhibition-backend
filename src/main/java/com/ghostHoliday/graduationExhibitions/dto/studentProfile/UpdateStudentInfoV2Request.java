package com.ghostHoliday.graduationExhibitions.dto.studentProfile;

import com.ghostHoliday.graduationExhibitions.domain.FileType;
import com.ghostHoliday.graduationExhibitions.dto.post.FileInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.tomcat.jni.FileInfo;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateStudentInfoV2Request {
    private String encryptedStudentId;
    private FileType fileType;
    private String extention;
}
