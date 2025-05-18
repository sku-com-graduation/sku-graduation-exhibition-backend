package com.ghostHoliday.graduationExhibitions.dto.post;

import com.ghostHoliday.graduationExhibitions.domain.FileType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePostInfoV2Request {
    private String teamUuid;
    private List<FileInfoDTO> imageInfos;
    private VideoInfo videoInfo;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VideoInfo {
        private FileType fileType;
        private String contentType;
        private String extension;
        private long fileSize;
        private long partSize;
    }
}
