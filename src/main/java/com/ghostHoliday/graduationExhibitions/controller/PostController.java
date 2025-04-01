package com.ghostHoliday.graduationExhibitions.controller;

import com.ghostHoliday.graduationExhibitions.dto.post.*;
import com.ghostHoliday.graduationExhibitions.exception.UnauthorizedException;
import com.ghostHoliday.graduationExhibitions.service.HttpOnlyService;
import com.ghostHoliday.graduationExhibitions.service.PostService;
import com.ghostHoliday.graduationExhibitions.utility.JwtUtility;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/")
@RequiredArgsConstructor
public class PostController {
    
    private final PostService postService;
    private final JwtUtility jwtUtility;
    private final HttpOnlyService httpOnlyService;

    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    @PutMapping("user/post/update/slideImage")
    public ResponseEntity<String> updateSlideImages(
            HttpServletRequest request,
            HttpServletResponse response,  // accessToken 재발급을 위해 추가
            @ModelAttribute UpdateSlideImageDTO dto
    ) throws Exception {
        try {

            // 서비스 계층에서 accessToken 검증 및 재발급 처리
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);
            String token = jwtUtility.extractAccessTokenFromCookie(request);

            postService.updateSlideImage(dto, jwtUtility.getEmailFromToken(token));
            return ResponseEntity.ok("파일 업로드에 성공했습니다.");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("파일 업로드 실패: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    @PutMapping("user/post/update/teamPost")
    public ResponseEntity<String> updateTeamPost(
            HttpServletRequest request,
            HttpServletResponse response,  // accessToken 재발급을 위해 추가
            @ModelAttribute UpdateTeamPostDTO dto
    ) throws Exception {
        try {

            // 서비스 계층에서 accessToken 검증 및 재발급 처리
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);
            String token = jwtUtility.extractAccessTokenFromCookie(request);


            postService.updatePostInfo(dto, jwtUtility.getEmailFromToken(token));
            return ResponseEntity.ok("파일 업로드에 성공했습니다.");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        }  catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("파일 업로드 실패: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    @PutMapping("user/post/update/studentProfile")
    public ResponseEntity<String> updateStudentProfile(
            HttpServletRequest request,
            HttpServletResponse response,  // accessToken 재발급을 위해 추가
            @ModelAttribute UpdateStudentProfileByPostDTO dto
            ) throws Exception {
        try {
            // 서비스 계층에서 accessToken 검증 및 재발급 처리
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);
            String token = jwtUtility.extractAccessTokenFromCookie(request);

            postService.updateStudentProfileByPost(dto, jwtUtility.getEmailFromToken(token));
            return ResponseEntity.ok("학생 정보를 수정했습니다.");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        }  catch (Exception e){
            return ResponseEntity.status(500).body("학생 정보 수정에 실패했습니다." + e.getMessage());
        }
    }

    @PostMapping("public/post/search")
    public ResponseEntity<?> searchPost(
            @RequestBody SearchPostRequestDTO dto) throws Exception {
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
    @PostMapping("user/post/edit/search")
    public ResponseEntity<?> searchEditPostInfo(
            HttpServletRequest request,
            HttpServletResponse response,  // accessToken 재발급을 위해 추가
            @RequestBody EditStudentInfoRequestDTO dto) throws Exception {
        try {

            // 서비스 계층에서 accessToken 검증 및 재발급 처리
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);

            String token = jwtUtility.extractAccessTokenFromCookie(request);


            List<EditStudentInfoResponseDTO> result = postService.searchEditPostInfo(token, dto.getUuid());
            return ResponseEntity.ok(result);
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        }  catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }





    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    @PostMapping("user/post/update/verify")
    public ResponseEntity<String> verifyEditPermission(
            HttpServletRequest request,
            HttpServletResponse response,  // accessToken 재발급을 위해 추가
            @RequestBody verifyEditPermissionRequestDTO dto) throws Exception {
        try {

            // 서비스 계층에서 accessToken 검증 및 재발급 처리
            String accessToken = httpOnlyService.refreshTokenIfNeeded(request, response);
            boolean result = postService.verifyEditPermission(dto.getToken(), dto.getUuid());

            return ResponseEntity.ok("인증 성공");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 인증입니다. 다시 로그인 해주세요: " + e.getMessage());
        }  catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }


}