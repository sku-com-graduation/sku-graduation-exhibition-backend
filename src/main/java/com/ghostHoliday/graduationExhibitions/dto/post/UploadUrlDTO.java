package com.ghostHoliday.graduationExhibitions.dto.post;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadUrlDTO {
    private String cloudFrontUrl;
    private String s3Url;
}
