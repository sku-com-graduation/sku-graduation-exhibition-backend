package com.ghostHoliday.graduationExhibitions.dto.account;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindStudentInfoByCreateAccountResponse {
    private List<ProfessorInfosByCreateAccount> professors;
    private List<StudentInfosByCreateAccount> students;
}
