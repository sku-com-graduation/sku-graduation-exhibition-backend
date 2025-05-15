package com.ghostHoliday.graduationExhibitions.dto.post;

import com.ghostHoliday.graduationExhibitions.domain.Category;
import com.ghostHoliday.graduationExhibitions.domain.Operation;
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
    private Operation teamProfileImageOperation;
    private String teamProfileImagePath;
    private String demoVideo;
    private Operation demoVideoOperation;
    private String demoVideoPath;
    private String posterImage;
    private Operation posterImageOperation;
    private String posterImagePath;
}
