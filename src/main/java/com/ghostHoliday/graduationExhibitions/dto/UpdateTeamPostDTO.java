package com.ghostHoliday.graduationExhibitions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTeamPostDTO {
    private String encryptedTeamId;
    private MultipartFile teamProfileImg;
    private MultipartFile demo;
    private MultipartFile poster;
}
