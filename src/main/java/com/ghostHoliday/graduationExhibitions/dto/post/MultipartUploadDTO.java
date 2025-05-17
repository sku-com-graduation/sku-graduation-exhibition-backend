package com.ghostHoliday.graduationExhibitions.dto.post;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MultipartUploadDTO {
    private String key;
    private String uploadId;
    private List<PartUrl> s3Urls;
    private String cloudFrontUrl;

    @Data
    @AllArgsConstructor
    public static class PartUrl {
        private int partNumber;
        private String url;
    }
}