package com.ghostHoliday.graduationExhibitions.dto.team;

import com.ghostHoliday.graduationExhibitions.domain.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindTeamInfoByYearTeamsDTO {
    private String encryptedTeamId;
    private String encryptedProfessorId;
    private String name;
    private String professor;
    private Category category;
}
