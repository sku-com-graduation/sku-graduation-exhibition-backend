package com.ghostHoliday.graduationExhibitions.dto.account;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterAccountRequest {
    private int year;
    private List<RegisterAccountDTO> registerAccounts;
}
