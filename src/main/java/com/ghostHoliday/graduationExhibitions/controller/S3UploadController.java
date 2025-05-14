package com.ghostHoliday.graduationExhibitions.controller;

import com.ghostHoliday.graduationExhibitions.utility.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3")
public class S3UploadController {

    private final S3Uploader s3Uploader;

    @GetMapping("/presigned-url")
    public ResponseEntity<Map<String, String>> getPreSignedUrl(
            @RequestParam String folder,
            @RequestParam String extension // 예: jpg, png, mp4 등
    ) {
        String result = s3Uploader.generatePreSignedUploadUrl(folder, extension);
        String[] split = result.split("\\|");

        Map<String, String> response = new HashMap<>();
        response.put("accessUrl", split[0]);       // CloudFront 접근용
        response.put("uploadUrl", split[1]);       // S3 직접 업로드용

        return ResponseEntity.ok(response);
    }
}