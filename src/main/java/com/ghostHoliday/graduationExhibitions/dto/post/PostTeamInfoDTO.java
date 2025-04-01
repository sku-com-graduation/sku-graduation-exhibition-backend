package com.ghostHoliday.graduationExhibitions.dto.post;

import com.ghostHoliday.graduationExhibitions.domain.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostTeamInfoDTO {
    private String teamUuid;
    private String title;
    private String content;
    private Category category;
    private String teamProfileImage;
    private List<String> slideImages;
    private String posterImage;
    private String demoVideo;
}
