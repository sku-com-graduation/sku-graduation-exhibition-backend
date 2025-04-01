package com.ghostHoliday.graduationExhibitions.controller;


import com.ghostHoliday.graduationExhibitions.dto.account.FindAccountByYearResponseDTO;
import com.ghostHoliday.graduationExhibitions.dto.account.LoginDTO;
import com.ghostHoliday.graduationExhibitions.dto.account.LoginRequestDTO;
import com.ghostHoliday.graduationExhibitions.exception.UnauthorizedException;
import com.ghostHoliday.graduationExhibitions.service.AccountService;
import com.ghostHoliday.graduationExhibitions.service.EncryptionService;
import com.ghostHoliday.graduationExhibitions.service.HttpOnlyService;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import com.opencsv.exceptions.CsvException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("api/")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final EncryptionService encryptionService;
    private final HttpOnlyService httpOnlyService;
    private final JwtUtility jwtUtility;


    @PostMapping("public/account/login")
    public ResponseEntity<Object> login(
            @RequestBody LoginRequestDTO request
            ) {
        try {
            return accountService.login(request);


        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "아이디 혹은 비밀번호가 잘못되었습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }


    @PostMapping("admin/account/regist")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<String> registAccountAndTeam(
            HttpServletRequest request,
            HttpServletResponse response,  // accessToken 재발급을 위해 추가
            @RequestBody MultipartFile file) {

        try {

            // 서비스 계층에서 accessToken 검증 및 재발급 처리
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);


            // accessToken이 유효하면 요청 처리
            accountService.registAccount(file);
            return ResponseEntity.status(HttpStatus.CREATED).body("계정 및 팀 정보가 성공적으로 등록되었습니다.");

        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
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


