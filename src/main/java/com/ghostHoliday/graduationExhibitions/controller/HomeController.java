package com.ghostHoliday.graduationExhibitions.controller;

import com.ghostHoliday.graduationExhibitions.dto.home.UpdateExhibitionDateRequest;
import com.ghostHoliday.graduationExhibitions.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/")
@RequiredArgsConstructor
public class HomeController {
    private final HomeService homeService;

    @PutMapping("admin/home/update/info")
    public ResponseEntity<?> updateHome(
            @RequestHeader("Authorization") String token,
            @RequestBody UpdateExhibitionDateRequest request) {
        try {
            homeService.updateExhibitionDate(token, request);
            return ResponseEntity.ok("정보 수정 성공");
        } catch (Exception e) {
            return ResponseEntity.noContent().build();
        }
    }
}

