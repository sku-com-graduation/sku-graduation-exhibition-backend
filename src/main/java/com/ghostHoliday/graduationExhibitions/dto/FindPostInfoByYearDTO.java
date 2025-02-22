package com.ghostHoliday.graduationExhibitions.dto;

import com.ghostHoliday.graduationExhibitions.domain.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindPostInfoByYearDTO {
    private String uuid;
    private String teamName;
    private String title;
    private String teamProfileImage;
    private Category category;
}
