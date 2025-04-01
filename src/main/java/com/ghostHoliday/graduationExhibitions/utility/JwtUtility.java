package com.ghostHoliday.graduationExhibitions.utility;

import com.ghostHoliday.graduationExhibitions.domain.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Service
public class JwtUtility {

    private final SecretKey secretKey;

    public JwtUtility() {
        this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }

    // ⏳ Access Token 만료 시간 (30분)
    private static final long ACCESS_TOKEN_EXPIRATION = 1000L * 60 * 1; // 2분

    // ⏳ Refresh Token 만료 시간 (24시간)
    private static final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60; // 1시간


    /**
     * 토큰 생성 (role 포함) 메서드
     */
    public String generateToken(String userEmail, Role role) {
        return Jwts.builder()
                .setSubject(userEmail)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION)) // 30분
                .addClaims(Map.of("role", role.name()))
                .signWith(secretKey)
                .compact();
    }


    /**
     * 토큰 검증 및 파싱 메서드
     */
    public Claims validateToken(String token) {

        try{
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰에서도 클레임을 가져오기 위해 getClaims() 사용
            return e.getClaims();
        } catch (JwtException e) {
            throw new RuntimeException("유효하지 않은 토큰입니다.", e);
        }

    }



    /**
     * Token 유효 여부 확인 메소드
     * 기존 vaildationToken을 재사용 함
     */
    public boolean isTokenValid(String token) {
        try {
            validateToken(token); // 기존 메서드 재사용
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 토큰 만료 여부 확인 메서드
     */
    public boolean isTokenExpired(String token) {
        return validateToken(token).getExpiration().before(new Date());
    }


    public String getEmailFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject(); // 이메일 반환
        } catch (ExpiredJwtException e) {
            // 만료된 토큰에서도 클레임을 가져오기 위해 getClaims() 사용
            return e.getClaims().getSubject();
        } catch (JwtException e) {
            throw new RuntimeException("유효하지 않은 토큰입니다.", e);
        }
    }





    /**
     * Refresh Token 생성 메서드
     */
    public String generateRefreshToken(String userEmail) {
        return Jwts.builder()
                .setSubject(userEmail)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION)) // 24시간
                .signWith(secretKey)
                .compact();
    }

    /**
     * HttpOnly 쿠키에서 Access Token을 추출하는 메서드
     */
//    public String extractAccessTokenFromCookie(HttpServletRequest request) {
//        if (request.getCookies() != null) {
//            for (Cookie cookie : request.getCookies()) {
//                if ("accessToken".equals(cookie.getName())) {
//                    return cookie.getValue();
//                }
//            }
//        }
//        return null;
//    }

    public String extractAccessTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    System.out.println("Found access token cookie: " + cookie.getValue());  // 디버그 로그 추가
                    return cookie.getValue();
                }
            }
        }
        return null;
    }




}