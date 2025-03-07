package com.ghostHoliday.graduationExhibitions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindAccountByYearResponseDTO {
    private String encryptedAccountId;
    private String teamName;
    private String userEmail;
    private LocalDateTime recent;
}
