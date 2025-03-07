package com.ghostHoliday.graduationExhibitions.dto;

import com.ghostHoliday.graduationExhibitions.domain.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindTeamInfoByYearDTO {
    private String encryptedTeamId;
    private String encryptedProfessorId;
    private String name;
    private String professor;
    private Category category;
}
