package com.ghostHoliday.graduationExhibitions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseFindTeamInfoByYearDTO {
    private int year;
    private ArrayList<TeamInfoDTO> teams;

}
