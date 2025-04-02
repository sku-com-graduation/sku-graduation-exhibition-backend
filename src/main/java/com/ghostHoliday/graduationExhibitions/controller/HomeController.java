package com.ghostHoliday.graduationExhibitions.controller;

import com.ghostHoliday.graduationExhibitions.dto.home.CreateExhibitionDateRequest;
import com.ghostHoliday.graduationExhibitions.dto.home.SearchExhitibitionDateResponse;
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

import java.util.List;

@RestController
@RequestMapping("api/")
@RequiredArgsConstructor
public class HomeController {
    private final HomeService homeService;
    private final HttpOnlyService httpOnlyService;
    private final JwtUtility jwtUtility;

    @PostMapping("admin/home/create/infos")
    public ResponseEntity<?> createExhibitionDates(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody List<CreateExhibitionDateRequest> requests
    ) {
        try {
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);
            String token = jwtUtility.extractAccessTokenFromCookie(request);
            homeService.createExhibitionDates(requests,token);
            return ResponseEntity.ok("전시 년도 정보를 생성했습니다.");
        }catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }



    @PutMapping("admin/home/update/info")
    public ResponseEntity<?> updateExhibitionDate(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody UpdateExhibitionDateRequest dto) {
        try {
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);
            String token = jwtUtility.extractAccessTokenFromCookie(request);
            homeService.updateExhibitionDate(token,dto);
            return ResponseEntity.ok("전시 년도 정보를 수정했습니다.");
        }catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("admin/home/search/infos")
    public ResponseEntity<?> searchExhibitionDates(
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);
            String token = jwtUtility.extractAccessTokenFromCookie(request);
            List<SearchExhitibitionDateResponse> responses = homeService.searchExhibitionDates(token);
            return ResponseEntity.ok(responses);
        }catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("public/home/search/info")
    public ResponseEntity<?> searchExhibitionDate(
            @RequestParam String year
    ) {
        try {
            SearchExhitibitionDateResponse result = homeService.searchExhibitionDate(year);
            return ResponseEntity.ok(result);
        }catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }



}

