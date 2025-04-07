package com.ghostHoliday.graduationExhibitions.dto.team;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindTeamInfoByYearProfessorsDTO {
    private String encryptedProfessorId;
    private String professorName;
}
