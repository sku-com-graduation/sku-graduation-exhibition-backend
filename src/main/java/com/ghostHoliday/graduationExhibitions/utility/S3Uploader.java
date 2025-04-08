package com.ghostHoliday.graduationExhibitions.utility;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3Uploader {

    @Value("${AWS_ACCESS_KEY}")
    private String accessKey;

    @Value("${AWS_SECRET_KEY}")
    private String secretKey;

    @Value("${AWS_REGION}")
    private String region;

    @Value("${BUCKET_NAME}")
    private String bucket;

    @Value("${CLOUDFRONT_URL}")
    private String cloudFrontUrl;

    private final FileUtility fileUtility;

    private S3Client getS3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    public String upload(MultipartFile file, String folder) throws IOException {
        String extension = fileUtility.getImageFileExtension(file.getOriginalFilename());
        if (extension == null || (!extension.equalsIgnoreCase("jpg") && !extension.equalsIgnoreCase("png"))) {
            throw new RuntimeException("지원되지 않는 파일 형식입니다.");
        }

        String key = folder + "/" + UUID.randomUUID() + "." + extension;

        getS3Client().putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return cloudFrontUrl + "/" + key;
    }

    public void delete(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;
        String key = imageUrl.replace(cloudFrontUrl + "/", "");

        getS3Client().deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }
}
