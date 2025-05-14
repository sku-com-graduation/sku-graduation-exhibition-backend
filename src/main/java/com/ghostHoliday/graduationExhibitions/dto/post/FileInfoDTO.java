package com.ghostHoliday.graduationExhibitions.dto.post;

import com.ghostHoliday.graduationExhibitions.domain.FileType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileInfoDTO {
    private FileType fileType;
    private String extention;
}
