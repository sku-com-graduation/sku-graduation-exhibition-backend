package com.ghostHoliday.graduationExhibitions.dto.account;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentInfosByCreateAccount {
    private String studentName;
    private String studentNumber;
    private String studentEmail;
}
