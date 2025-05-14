package com.ghostHoliday.graduationExhibitions.dto.post;

import com.ghostHoliday.graduationExhibitions.domain.FileType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePostInfoTestResponse {
    private FileType fileType;
    private String cloudFrontUrl;
    private String s3Url;
}
