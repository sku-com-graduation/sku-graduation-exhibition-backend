package com.ghostHoliday.graduationExhibitions.controller;


import com.ghostHoliday.graduationExhibitions.domain.Account;
import com.ghostHoliday.graduationExhibitions.dto.FindAccountByYearDTO;
import com.ghostHoliday.graduationExhibitions.dto.FindAccountByYearResponseDTO;
import com.ghostHoliday.graduationExhibitions.service.AccountService;
import com.ghostHoliday.graduationExhibitions.service.JwtUtility;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("account")
public class AccountController {
    private final AccountService accountService;
    private final JwtUtility jwtUtility;

    @PostMapping("/regist")
    public ResponseEntity<String> registAccountAndTeam(@RequestParam MultipartFile file) {
        try {
            accountService.registAccount(file);
            return ResponseEntity.status(HttpStatus.CREATED).body("계정 및 팀 정보가 성공적으로 등록되었습니다.");

        } catch (CsvException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("CSV 파일 처리 중 오류가 발생했습니다: " + e.getMessage());

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("파일 읽기 중 오류가 발생했습니다: " + e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("예기치 않은 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<FindAccountByYearResponseDTO> searchAccount(@RequestParam int year) {
        ArrayList<FindAccountByYearDTO> accounts = accountService.findAllAccountByYear(year);
        if (accounts.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(new FindAccountByYearResponseDTO(year, accounts));

    }

    @GetMapping("/delete")
    public ResponseEntity<String> deleteAccount(String token) {
        Account account = accountService.tokenToAccount(token);
        try{
            accountService.deleteAccount(account.getId());
            return ResponseEntity.ok(  account.getUserEmail() + " 계정을 성공적으로 삭제했습니다.");
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("계정을 삭제하지 못했습니다." + e.getMessage());
        }


    }

}
