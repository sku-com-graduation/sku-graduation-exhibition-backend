package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.Account;
import com.ghostHoliday.graduationExhibitions.domain.Home;
import com.ghostHoliday.graduationExhibitions.domain.Role;
import com.ghostHoliday.graduationExhibitions.repository.AccountRepository;
import com.ghostHoliday.graduationExhibitions.repository.HomeRepository;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import io.jsonwebtoken.Jwt;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {
    private final HomeRepository homeRepository;
    private final AccountRepository accountRepository;
    private final JwtUtility jwtUtility;


    public void updateExhibitionDate(String token, int year) throws AccessDeniedException {

        String userEmail = jwtUtility.getEmailFromToken(token);
        Account account = accountRepository.findAccountByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        if (!account.getRole().equals(Role.ADMIN)){
            throw new AccessDeniedException("권한이 없습니다.");
        }

        Home home = homeRepository.findByExhibitionYear(year).get();

    }
}
