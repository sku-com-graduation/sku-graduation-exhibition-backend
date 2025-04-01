package com.ghostHoliday.graduationExhibitions.dto.professor;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindProfessorDTO{

    private String encryptedProfessorId;
    private String name;
    private String email;
    private String profileImg;
    private Boolean tenure;

}