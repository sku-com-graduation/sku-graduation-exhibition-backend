package com.ghostHoliday.graduationExhibitions.dto.account;

import com.ghostHoliday.graduationExhibitions.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginDTO {
    private String accessToken;
    private Role role;
    private String teamName;
    private String uuid;
    private LocalDateTime recent;
}
