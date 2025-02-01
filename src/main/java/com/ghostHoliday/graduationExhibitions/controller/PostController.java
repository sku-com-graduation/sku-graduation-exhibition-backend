package com.ghostHoliday.graduationExhibitions.controller;

import com.ghostHoliday.graduationExhibitions.dto.UpdateSlideImageDTO;
import com.ghostHoliday.graduationExhibitions.dto.UpdateStudentProfileByPostDTO;
import com.ghostHoliday.graduationExhibitions.service.PostService;
import lombok.RequiredArgsConstructor;
import org.hibernate.sql.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping ("post")
@RequiredArgsConstructor
public class PostController {
    
    private final PostService postService;
    
    @PatchMapping("update/slideImage")
    public ResponseEntity<String> updateSlideImages(
            @RequestParam("token") String token,
            @RequestParam("encryptionTeamId") String encryptionTeamId,
            @RequestParam("files") List<MultipartFile> files) throws Exception {
        try {
            postService.updateSlideImage(token,encryptionTeamId,files);
            return ResponseEntity.ok("파일 업로드에 성공했습니다.");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("파일 업로드 실패: " + e.getMessage());
        }
    }

    @PatchMapping("update/teamPost")
    public ResponseEntity<String> updateTeamPost(
            @RequestParam("token") String token,
            @RequestParam("encryptionTeamId") String encryptionTeamId,
            @RequestParam("teamProfileImg") MultipartFile teamProfileImg,
            @RequestParam("demo") MultipartFile demo,
            @RequestParam("poster") MultipartFile poster)
            throws Exception {
        try {
            postService.updatePostInfo(token,encryptionTeamId,teamProfileImg,demo,poster);
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
