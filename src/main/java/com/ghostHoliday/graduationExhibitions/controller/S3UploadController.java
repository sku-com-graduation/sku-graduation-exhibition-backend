package com.ghostHoliday.graduationExhibitions.controller;

import com.ghostHoliday.graduationExhibitions.dto.post.MultipartCompleteRequestDTO;
import com.ghostHoliday.graduationExhibitions.dto.post.UpdatePostInfoV2Request;
import com.ghostHoliday.graduationExhibitions.dto.post.S3UrlDTO;
import com.ghostHoliday.graduationExhibitions.dto.post.UpdatePostInfoV2Response;
import com.ghostHoliday.graduationExhibitions.dto.professor.UpdateProfessorInfoV2;
import com.ghostHoliday.graduationExhibitions.dto.studentProfile.UpdateStudentInfoV2Request;
import com.ghostHoliday.graduationExhibitions.service.PostService;
import com.ghostHoliday.graduationExhibitions.service.ProfessorService;
import com.ghostHoliday.graduationExhibitions.service.StudentService;
import com.ghostHoliday.graduationExhibitions.utility.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user/s3")
public class S3UploadController {

    private final PostService postService;
    private final ProfessorService professorService;
    private final StudentService studentService;
    private final S3Uploader s3Uploader;

    @PostMapping("/presigned-url/post")
    public ResponseEntity<UpdatePostInfoV2Response> getPreSignedUrlByPost(
            @RequestBody UpdatePostInfoV2Request request
    ) {
        UpdatePostInfoV2Response response = postService.UpdatePostInfoV2(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/presigned-url/professor")
    public ResponseEntity<S3UrlDTO> getPreSignedUrlByProfessor(
            @RequestBody UpdateProfessorInfoV2 request
    ) throws Exception {
        S3UrlDTO response = professorService.updateProfessorInfoV2(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/presigned-url/student")
    public ResponseEntity<S3UrlDTO> getPreSignedUrlByStudent(
            @RequestBody UpdateStudentInfoV2Request request
    ) throws Exception {
        S3UrlDTO response = studentService.updateStudentInfoV2(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/multipart/complete")
    public ResponseEntity<String> completeMultipartUpload(@RequestBody MultipartCompleteRequestDTO dto) {
        s3Uploader.completeMultipartUpload(dto.getKey(), dto.getUploadId(), dto.getParts());
        return ResponseEntity.ok("비디오 업로드에 성공했습니다.");
    }

}