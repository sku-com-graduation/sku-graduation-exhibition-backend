package com.ghostHoliday.graduationExhibitions.utility;

import com.ghostHoliday.graduationExhibitions.exception.UnauthorizedException;
import com.ghostHoliday.graduationExhibitions.service.HttpOnlyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

import java.io.IOException;
import java.util.Collections;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtility jwtUtility;
    private final HttpOnlyService httpOnlyService;

    public JwtAuthenticationFilter(JwtUtility jwtUtility, HttpOnlyService httpOnlyService) {
        this.jwtUtility = jwtUtility;
        this.httpOnlyService = httpOnlyService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/api/public")  // /public/** 경로는 필터 제외
                || path.equals("/");
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String token = extractToken(request);


        if (token == null) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "토큰이 제공되지 않았습니다.");
            return;
        }

        Claims claims = null;

        try {
            claims = jwtUtility.validateToken(token);
        } catch (ExpiredJwtException e) {


            // 🔹 RefreshToken을 이용하여 새 AccessToken 발급
            token = httpOnlyService.refreshTokenIfNeeded(request, response);

            if (token == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "AccessToken 및 RefreshToken이 만료되었습니다.");
                return;
            }


            // 새 AccessToken으로 다시 Claims 검증
            claims = jwtUtility.validateToken(token);

        } catch (JwtException e) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 JWT 토큰입니다.");
            return;
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "JWT 인증 과정에서 오류가 발생했습니다.");
            return;
        }

        authenticateUser(claims);

        if (requestURI.startsWith("/account/admin") && !claims.get("role", String.class).equals("ADMIN")) {
            sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "관리자 권한이 필요합니다.");
            return;
        }

        filterChain.doFilter(request, response);
    }





//    private String extractToken(HttpServletRequest request) {
//        String bearerToken = request.getHeader("Authorization");
//        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
//            return bearerToken.substring(7);
//        }
//        return null;
//    }

    private String extractToken(HttpServletRequest request) {
        // 쿠키에서 토큰을 찾기
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {  // jakarta.servlet.http.Cookie 사용
                if ("accessToken".equals(cookie.getName())) {  // "accessToken" 쿠키가 있는지 확인
                    return cookie.getValue();  // 토큰 반환
                }
            }
        }
        return null;  // 쿠키에 토큰이 없으면 null 반환
    }


    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }



    private void authenticateUser(Claims claims) {
        String role = claims.get("role", String.class);

        if (role == null) {
            throw new JwtException("유효한 역할이 없습니다.");
        }

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                claims.getSubject(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}