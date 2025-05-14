package com.ghostHoliday.graduationExhibitions.dto.post;

import com.ghostHoliday.graduationExhibitions.domain.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTeamPostDTO {
    private String teamUuid;
    private String title;
    private String content;
    private Category category;
    private String teamProfileImage;
    private String demoVideo;
    private String posterImage;
}
