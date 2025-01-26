package com.ghostHoliday.graduationExhibitions.utility;


import com.ghostHoliday.graduationExhibitions.domain.Team;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;


@Service
public class FileUtility {
    
    // 사진 확장자 추출기
    public String getFileExtension(String fileName) {
        // 파일 이름에서 확장자 추출
        String extension = null;
        if (fileName != null && (fileName.endsWith(".jpg") || fileName.endsWith(".png"))) {
            extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        }
        return extension;
    }


}
