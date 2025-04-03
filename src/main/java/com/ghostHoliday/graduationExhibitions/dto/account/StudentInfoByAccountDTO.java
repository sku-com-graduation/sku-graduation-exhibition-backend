package com.ghostHoliday.graduationExhibitions.dto.account;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentInfoByAccountDTO {
    private String studentName;
    private String studentNumber;
    private String studentEmail;
}
