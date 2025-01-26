package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.Account;
import com.ghostHoliday.graduationExhibitions.domain.Professor;
import com.ghostHoliday.graduationExhibitions.domain.Role;
import com.ghostHoliday.graduationExhibitions.domain.Team;
import com.ghostHoliday.graduationExhibitions.repository.AccountRepository;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TokenService {

    private final AccountRepository accountRepository;
    private final JwtUtility jwtUtility;

    public boolean isAdmin(String token) {
        String userEmail = jwtUtility.validateToken(token).getSubject();
        Account account = accountRepository.findAccountByUserEmail(userEmail).get();

        return account.getRole().equals(Role.ADMIN);
    }
}
