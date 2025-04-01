package com.ghostHoliday.graduationExhibitions.dto.post;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSlideImageDTO {
    private String teamUuid;
    private List<MultipartFile> files;
}
