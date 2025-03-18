package com.ghostHoliday.graduationExhibitions.dto;

import com.ghostHoliday.graduationExhibitions.domain.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTeamPostDTO {
    private String teamUuId;
    private String title;
    private String content;
    private Category category;
    private MultipartFile teamProfileImage;
    private MultipartFile demo;
    private MultipartFile posterImg;
}
