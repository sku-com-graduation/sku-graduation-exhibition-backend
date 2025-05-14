package com.ghostHoliday.graduationExhibitions.controller;

import com.ghostHoliday.graduationExhibitions.dto.post.UpdatePostInfoTestRequest;
import com.ghostHoliday.graduationExhibitions.dto.post.UpdatePostInfoTestResponse;
import com.ghostHoliday.graduationExhibitions.service.PostService;
import com.ghostHoliday.graduationExhibitions.utility.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3")
public class S3UploadController {

    private final PostService postService;

    @PostMapping("/presigned-url/post")
    public ResponseEntity<List<UpdatePostInfoTestResponse>> getPreSignedUrl(
            @RequestBody UpdatePostInfoTestRequest request
    ) {
        List<UpdatePostInfoTestResponse> responses = postService.updatePostInfoTest(request);

        return ResponseEntity.ok(responses);
    }
}