package com.ghostHoliday.graduationExhibitions.controller;


import com.ghostHoliday.graduationExhibitions.domain.Account;
import com.ghostHoliday.graduationExhibitions.dto.*;
import com.ghostHoliday.graduationExhibitions.service.AccountService;
import com.ghostHoliday.graduationExhibitions.service.EncryptionService;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import com.opencsv.exceptions.CsvException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final EncryptionService encryptionService;


    @PostMapping("public/account/login")
    public ResponseEntity<Object> login(@RequestBody LoginRequestDTO request) {
        try {
            LoginDTO dto = accountService.login(request.getUserName(), request.getPassword());

            return ResponseEntity.ok(dto);
        }catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "아이디 혹은 비밀번호가 잘못되었습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

    }

    @PostMapping("admin/account/regist")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
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

    @GetMapping("admin/account/search")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<List<FindAccountByYearResponseDTO>> searchAccount(@RequestParam int year) throws Exception {
        List<FindAccountByYearResponseDTO> accounts = accountService.findAllAccountByYear(year);
        if (accounts.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(accounts);

    }

    @DeleteMapping("admin/account/delete")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<String> deleteAccount(@RequestBody List<String> encryptedAccountIds) throws Exception {

        if (encryptedAccountIds == null || encryptedAccountIds.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("토큰 리스트가 비어 있습니다.");
        }
        ArrayList<Long> accountIds = new ArrayList<>();
        try {
            for (String encryptedAccountId : encryptedAccountIds) {
                Long accountId = encryptionService.decryptPrimaryKey(encryptedAccountId);
                accountIds.add(accountId);
            }
            accountService.deleteAccount(accountIds);
            return ResponseEntity.ok("계정을 성공적으로 삭제했습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("계정을 삭제하지 못했습니다." + e.getMessage());
        }

    }

    @PostMapping("admin/account/reset")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<String> resetAccount(@RequestBody List<String> encryptedAccountIds) throws Exception {
        ArrayList<Long> accountsId = new ArrayList<>();

        if (encryptedAccountIds == null || encryptedAccountIds.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("토큰 리스트가 비어 있습니다.");
        }

        for (String encryptedAccountId : encryptedAccountIds) {
            Long accountId = encryptionService.decryptPrimaryKey(encryptedAccountId);
            accountsId.add(accountId);
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


