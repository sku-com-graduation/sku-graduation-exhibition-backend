package com.ghostHoliday.graduationExhibitions.controller;

import com.ghostHoliday.graduationExhibitions.dto.UpdateSlideImageDTO;
import com.ghostHoliday.graduationExhibitions.dto.UpdateStudentProfileByPostDTO;
import com.ghostHoliday.graduationExhibitions.dto.UpdateTeamPostDTO;
import com.ghostHoliday.graduationExhibitions.service.PostService;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import io.jsonwebtoken.Jwt;
import lombok.RequiredArgsConstructor;
import org.hibernate.sql.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PostController {
    
    private final PostService postService;
    private final JwtUtility jwtUtility;

    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    @PatchMapping("user/post/update/slideImage")
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
    @PatchMapping("user/post/update/teamPost")
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

    @PatchMapping("update/studentProfile")
    public ResponseEntity<String> updateStudentProfile(
            @RequestParam String token,
            @RequestParam UpdateStudentProfileByPostDTO updateStudentProfileByPostDTO,
            @RequestParam MultipartFile studentProfileImage
            ) throws Exception {
        try {
            postService.updateStudentProfileByPost(token, updateStudentProfileByPostDTO);
            return ResponseEntity.ok("학생 정보를 수정했습니다.");
        } catch (Exception e){
            return ResponseEntity.status(500).body("학생 정보 수정에 실패했습니다." + e.getMessage());
        }
    }


}
