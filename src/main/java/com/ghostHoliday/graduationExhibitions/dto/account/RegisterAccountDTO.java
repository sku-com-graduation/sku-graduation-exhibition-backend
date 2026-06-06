package com.ghostHoliday.graduationExhibitions.dto.account;

import com.ghostHoliday.graduationExhibitions.domain.Category;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterAccountDTO {
    private String teamName;
    private Category category;
    private String professorName;
    @Valid
    private List<StudentInfoByAccountDTO> studentInfos;
}
