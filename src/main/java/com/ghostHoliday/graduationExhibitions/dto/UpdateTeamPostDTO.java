package com.ghostHoliday.graduationExhibitions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTeamPostDTO {
    String encryptionTeamId;
    MultipartFile teamProfileImg;
    MultipartFile demo;
    MultipartFile poster;
}
