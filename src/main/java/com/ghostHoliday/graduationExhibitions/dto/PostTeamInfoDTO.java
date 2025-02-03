package com.ghostHoliday.graduationExhibitions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostTeamInfoDTO {
    private String projectName;
    private String explanation;
    private String teamProfileImage;
    private List<String> slideImages;
    private String posterImage;
    private String demoVideo;
}
