package com.ghostHoliday.graduationExhibitions.controller;

import com.ghostHoliday.graduationExhibitions.dto.home.UpdateExhibitionDateRequest;
import com.ghostHoliday.graduationExhibitions.exception.UnauthorizedException;
import com.ghostHoliday.graduationExhibitions.service.HomeService;
import com.ghostHoliday.graduationExhibitions.service.HttpOnlyService;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/")
@RequiredArgsConstructor
public class HomeController {
    private final HomeService homeService;
    private final HttpOnlyService httpOnlyService;
    private final JwtUtility jwtUtility;

    @PutMapping("admin/home/update/info")
    public ResponseEntity<?> updateHome(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody UpdateExhibitionDateRequest dto) {
        try {
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);
            String token = jwtUtility.extractAccessTokenFromCookie(request);
            homeService.updateExhibitionDate(token,dto);
            return ResponseEntity.ok("정보 수정 성공");
        }catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.noContent().build();
        }
    }
}

