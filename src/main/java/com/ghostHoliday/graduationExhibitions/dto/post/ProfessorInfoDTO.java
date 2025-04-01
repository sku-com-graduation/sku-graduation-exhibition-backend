package com.ghostHoliday.graduationExhibitions.dto.post;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfessorInfoDTO {

    String professorImage;
    String professorName;
    String professorEmail;
    boolean ProfessorTenure;

}
