package com.ghostHoliday.graduationExhibitions.controller;

import com.ghostHoliday.graduationExhibitions.dto.*;
import com.ghostHoliday.graduationExhibitions.service.PostService;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import io.jsonwebtoken.Jwt;
import lombok.RequiredArgsConstructor;
import org.hibernate.sql.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class PostController {
    
    private final PostService postService;
    private final JwtUtility jwtUtility;

    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    @PutMapping("user/post/update/slideImage")
    public ResponseEntity<String> updateSlideImages(
            @ModelAttribute UpdateSlideImageDTO dto,
            @RequestHeader("Authorization") String token
    ) throws Exception {
        try {

            postService.updateSlideImage(dto, jwtUtility.getEmailFromToken(token));
            return ResponseEntity.ok("파일 업로드에 성공했습니다.");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("파일 업로드 실패: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    @PutMapping("user/post/update/teamPost")
    public ResponseEntity<String> updateTeamPost(
            @ModelAttribute UpdateTeamPostDTO dto,
            @RequestHeader("Authorization") String token
    ) throws Exception {
        try {
            postService.updatePostInfo(dto, jwtUtility.getEmailFromToken(token));
            return ResponseEntity.ok("파일 업로드에 성공했습니다.");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("파일 업로드 실패: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    @PutMapping("user/post/update/studentProfile")
    public ResponseEntity<String> updateStudentProfile(
            @ModelAttribute UpdateStudentProfileByPostDTO dto,
            @RequestHeader("Authorization") String token
            ) throws Exception {
        try {
            postService.updateStudentProfileByPost(dto, jwtUtility.getEmailFromToken(token));
            return ResponseEntity.ok("학생 정보를 수정했습니다.");
        } catch (Exception e){
            return ResponseEntity.status(500).body("학생 정보 수정에 실패했습니다." + e.getMessage());
        }
    }


    @PostMapping("public/post/search")
    public ResponseEntity<?> searchPost(@RequestBody SearchPostRequestDTO dto) throws Exception {
        try {
            SearchPostInfoDTO result = postService.searchPostInfo(dto.getUuid());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    @PostMapping("user/post/update/verify")
    public ResponseEntity<String> verifyEditPermission(@RequestBody verifyEditPermissionRequestDTO dto) throws Exception {
        try {
            boolean response = postService.verifyEditPermission(dto.getToken(), dto.getUuid());
            return ResponseEntity.ok("인증 성공");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }




}
