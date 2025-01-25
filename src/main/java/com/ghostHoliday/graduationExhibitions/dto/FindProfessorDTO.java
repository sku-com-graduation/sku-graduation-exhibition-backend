package com.ghostHoliday.graduationExhibitions.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindProfessorDTO{

    private Long professorId;
    private String name;
    private String email;
    private String profileImg;
    private Boolean tenure;

}