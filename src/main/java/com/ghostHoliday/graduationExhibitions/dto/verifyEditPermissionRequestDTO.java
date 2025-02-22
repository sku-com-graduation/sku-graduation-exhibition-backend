package com.ghostHoliday.graduationExhibitions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class verifyEditPermissionRequestDTO {
    private String token;
    private String uuid;
}
