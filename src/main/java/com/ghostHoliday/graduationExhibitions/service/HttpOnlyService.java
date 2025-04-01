package com.ghostHoliday.graduationExhibitions.service;

import com.ghostHoliday.graduationExhibitions.domain.Account;
import com.ghostHoliday.graduationExhibitions.domain.RefreshToken;
import com.ghostHoliday.graduationExhibitions.exception.UnauthorizedException;
import com.ghostHoliday.graduationExhibitions.repository.AccountRepository;
import com.ghostHoliday.graduationExhibitions.repository.RefreshTokenRepository;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HttpOnlyService {

    private final JwtUtility jwtUtility;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountRepository accountRepository;

    public String refreshTokenIfNeeded(HttpServletRequest request, HttpServletResponse response) throws UnauthorizedException {
        // 쿠키에서 accessToken 가져오기
        String accessToken = jwtUtility.extractAccessTokenFromCookie(request);
        System.out.println(accessToken + " 입니다 ~~~-----------");

        // accessToken 검증
        if (accessToken == null || !jwtUtility.isTokenValid(accessToken)) {
            // accessToken이 유효하지 않으면 refreshToken 가져와서 검증
            String refreshToken = jwtUtility.extractAccessTokenFromCookie(request);

            if (refreshToken == null || !jwtUtility.isTokenValid(refreshToken) || jwtUtility.isTokenExpired(refreshToken)) {
                throw new UnauthorizedException("유효하지 않거나 만료된 Refresh Token입니다.");
            }

            // refreshToken이 유효하면, refreshToken을 DB에서 확인하고 갱신
            String email = jwtUtility.getEmailFromToken(refreshToken);
            Account account = accountRepository.findAccountByUserEmail(email).get();

            // 해당 이메일로 refreshToken 조회
            RefreshToken existingRefreshToken = refreshTokenRepository.findByAccount(account).get();
            if (existingRefreshToken == null || !existingRefreshToken.getRefreshToken().equals(refreshToken)) {
                throw new UnauthorizedException("Refresh Token이 유효하지 않거나 만료되었습니다.");
            }

            // refreshToken이 유효하면 새로운 accessToken 발급
            String newAccessToken = jwtUtility.generateToken(email, account.getRole()); // 새로운 accessToken 생성

            // 새 accessToken을 HttpOnly 쿠키에 저장
            ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", newAccessToken)
                    .httpOnly(true)
                    .secure(true)  // HTTPS 환경에서만 전송 (테스트 시 false 가능)
                    .path("/")
                    .maxAge(60 * 60)  // 1시간 유지
                    .sameSite("Strict")
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

            // refreshToken 갱신: 새로운 refreshToken을 생성하여 DB에 갱신
            String newRefreshToken = jwtUtility.generateRefreshToken(email); // 새로운 refreshToken 생성
            existingRefreshToken.setRefreshToken(newRefreshToken);
            existingRefreshToken.setExpiryDate(LocalDateTime.now().plusHours(1)); // refreshToken의 만료 시간을 1시간으로 설정

            // DB에서 갱신된 refreshToken 저장
            refreshTokenRepository.save(existingRefreshToken);
        }
        return accessToken;
    }
}
