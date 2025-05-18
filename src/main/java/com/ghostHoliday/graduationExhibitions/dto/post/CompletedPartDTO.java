package com.ghostHoliday.graduationExhibitions.dto.post;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CompletedPartDTO {
    private int partNumber;
    @JsonProperty("eTag")
    private String eTag;
}
