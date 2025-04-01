package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.Account;
import com.ghostHoliday.graduationExhibitions.domain.Home;
import com.ghostHoliday.graduationExhibitions.domain.Role;
import com.ghostHoliday.graduationExhibitions.dto.home.SearchExhitibitionDateResponse;
import com.ghostHoliday.graduationExhibitions.dto.home.UpdateExhibitionDateRequest;
import com.ghostHoliday.graduationExhibitions.repository.AccountRepository;
import com.ghostHoliday.graduationExhibitions.repository.HomeRepository;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import io.jsonwebtoken.Jwt;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {
    private final HomeRepository homeRepository;
    private final AccountRepository accountRepository;
    private final JwtUtility jwtUtility;
    private final EncryptionService encryptionService;

    @Transactional
    public List<SearchExhitibitionDateResponse> searchExhitibitionDate(String token) throws Exception {
        String userEmail = jwtUtility.getEmailFromToken(token);
        Account account = accountRepository.findAccountByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        if (!account.getRole().equals(Role.ADMIN)){
            throw new AccessDeniedException("권한이 없습니다.");
        }

        List<SearchExhitibitionDateResponse> responses = new ArrayList<>();
        for (Home home : homeRepository.findAll()) {
            SearchExhitibitionDateResponse response = new SearchExhitibitionDateResponse();
            response.setEncryptedHomeId(encryptionService.encryptPrimaryKey(home.getId()));
            response.setExhibitionYear(home.getExhibitionYear());
            response.setExhibitionDate(home.getExhibitionDate());
            response.setExhibitionHour(home.getExhibitionHour());

            responses.add(response);
        }

        return responses;
    }


    @Transactional
    public void updateExhibitionDate(String token, UpdateExhibitionDateRequest request) throws Exception {

        String userEmail = jwtUtility.getEmailFromToken(token);
        Account account = accountRepository.findAccountByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        if (!account.getRole().equals(Role.ADMIN)){
            throw new AccessDeniedException("권한이 없습니다.");
        }

        if (!homeRepository.existsByExhibitionYear(request.getExhibitionYear())){
            throw new IllegalStateException("해당 년도의 정보를 찾을 수 없습니다.");
        }

        Home home = homeRepository.findById(encryptionService.decryptPrimaryKey(request.getEncryptedHomeId())).orElseThrow();
        home.setExhibitionYear(request.getExhibitionYear());
        home.setExhibitionDate(request.getExhibitionDate());
        home.setExhibitionHour(request.getExhibitionHour());

    }
}
