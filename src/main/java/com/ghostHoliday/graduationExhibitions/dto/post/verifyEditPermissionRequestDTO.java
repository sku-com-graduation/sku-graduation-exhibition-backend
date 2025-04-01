package com.ghostHoliday.graduationExhibitions.dto.post;

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
