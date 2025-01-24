package com.ghostHoliday.graduationExhibitions.dto;

import com.ghostHoliday.graduationExhibitions.domain.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostByTeamDTO {
    private String token;
    private String title;
    private String content;
    private String teamProfileUrl;
    private String demoUrl;
    private String posterUrl;
    private Category category;
}
