package com.ghostHoliday.graduationExhibitions.dto.post;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfessorInfoDTO {

    private String professorImage;
    private String professorName;
    private String professorEmail;
    private boolean ProfessorTenure;

}
