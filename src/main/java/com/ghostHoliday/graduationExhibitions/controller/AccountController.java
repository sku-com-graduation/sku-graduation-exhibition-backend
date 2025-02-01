package com.ghostHoliday.graduationExhibitions.controller;


import com.ghostHoliday.graduationExhibitions.domain.Account;
import com.ghostHoliday.graduationExhibitions.dto.FindAccountByYearDTO;
import com.ghostHoliday.graduationExhibitions.dto.FindAccountByYearResponseDTO;
import com.ghostHoliday.graduationExhibitions.service.AccountService;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("account")
public class AccountController {
    private final AccountService accountService;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password) {
        try {
            String token = accountService.login(username, password);
            return ResponseEntity.ok(token);
        }catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("아이디 혹은 비밀번호가 잘못되었습니다.");
        }

    }

    @PostMapping("/regist")
    public ResponseEntity<String> registAccountAndTeam(@RequestBody MultipartFile file) {
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

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteAccount(@RequestParam String token) {
        Account account = accountService.tokenToAccount(token);
        try {
            accountService.deleteAccount(account.getId());
            return ResponseEntity.ok(account.getUserEmail() + " 계정을 성공적으로 삭제했습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("계정을 삭제하지 못했습니다." + e.getMessage());
        }

    }

    @PostMapping("/reset")
    public ResponseEntity<String> resetAccount(@RequestBody List<String> tokens) {
        ArrayList<Long> accountsId = new ArrayList<>();

        if (tokens == null || tokens.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("토큰 리스트가 비어 있습니다.");
        }

        for (String token : tokens) {
            Account account = accountService.tokenToAccount(token);
            accountsId.add(account.getId());
        }

        try {
            accountService.resetAccount(accountsId);
            return ResponseEntity.ok("계정을 성공적으로 초기화했습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("계정 초기화 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}


