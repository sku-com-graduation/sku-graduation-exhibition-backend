package com.ghostHoliday.graduationExhibitions.dto.post;

import lombok.Data;
import java.util.List;

@Data
public class MultipartCompleteRequestDTO {
    private String key;
    private String uploadId;
    private List<CompletedPartDTO> parts;
}
