package com.ghostHoliday.graduationExhibitions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfessorDTO{
    private Long professorId;
    private String token;
    private String name;
    private String email;
    private boolean tenure;

}
