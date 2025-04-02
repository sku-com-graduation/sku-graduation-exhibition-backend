package com.ghostHoliday.graduationExhibitions.dto.home;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateExhibitionDateRequest {
    private String exhibitionYear;
    private String exhibitionDate;
    private String exhibitionHour;
}
