package com.ghostHoliday.graduationExhibitions.dto.post;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchPostInfoDTO {
    private List<StudentInfoDTO> students;
    private PostTeamInfoDTO teamInfo;
}
