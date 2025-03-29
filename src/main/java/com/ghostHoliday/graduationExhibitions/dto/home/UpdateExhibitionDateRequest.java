package com.ghostHoliday.graduationExhibitions.dto.home;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateExhibitionDateRequest {
    private String year;
    private String exhibitionDate;
    private String exhibitionHour;
}
