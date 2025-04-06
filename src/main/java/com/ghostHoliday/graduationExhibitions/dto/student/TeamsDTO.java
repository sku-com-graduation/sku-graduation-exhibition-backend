package com.ghostHoliday.graduationExhibitions.dto.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamsDTO {
    private String encryptedTeamId;
    private String teamName;
}
