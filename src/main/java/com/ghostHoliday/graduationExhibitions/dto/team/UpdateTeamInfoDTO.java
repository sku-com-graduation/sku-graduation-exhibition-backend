package com.ghostHoliday.graduationExhibitions.dto.team;

import com.ghostHoliday.graduationExhibitions.domain.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTeamInfoDTO {
    private String encryptedTeamId;
    private String encryptedProfessorId;
    private String name;
    private int exhibitionYear;
    private Category category;
}
