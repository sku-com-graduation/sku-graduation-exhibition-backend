package com.ghostHoliday.graduationExhibitions.dto.account;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import software.amazon.awssdk.annotations.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentInfoByAccountDTO {
    private String studentName;
    private String studentNumber;
    @NotNull @NotBlank
    private String studentEmail;
}
