package com.ghostHoliday.graduationExhibitions.dto;

import com.ghostHoliday.graduationExhibitions.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginDTO {
    String token;
    Role role;
    String teamName;
}
