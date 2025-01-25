package com.ghostHoliday.graduationExhibitions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTeamInfoDTO {
    private Long teamId;
    private Long professorId;
    private String name;
    private String exhibitionYear;
    private Long category;
}
