package com.ghostHoliday.graduationExhibitions.utility;

import com.ghostHoliday.graduationExhibitions.dto.post.UploadUrlDTO;
import lombok.Data;
import lombok.Getter;
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
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.DeleteObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedDeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Data
@Component
@RequiredArgsConstructor
public class S3Uploader {

    @Value("${AWS_ACCESS_KEY}")
    private String accessKey;

    @Value("${AWS_SECRET_KEY}")
    private String secretKey;

    @Value("${AWS_REGION}")
    private String region;

    @Getter
    @Value("${BUCKET_NAME}")
    private String bucket;

    @Getter
    @Value("${CLOUDFRONT_URL}")
    private String cloudFrontUrl;

    private final FileUtility fileUtility;


    public String generatePreSignedDeleteUrl(String key) {
        S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        PresignedDeleteObjectRequest presignedRequest = presigner.presignDeleteObject(
                DeleteObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))
                        .deleteObjectRequest(deleteRequest)
                        .build()
        );

        presigner.close();
        return presignedRequest.url().toString(); // 이 URL로 DELETE 요청하면 파일 삭제됨
    }


    public UploadUrlDTO generatePreSignedUploadUrl(String key) {

        S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("application/octet-stream") // 혹은 필요한 타입
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

        presigner.close();
        return new UploadUrlDTO(
                cloudFrontUrl + "/" + key,
                presignedRequest.url().toString());
    }


    public S3Client getS3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    public String upload(MultipartFile file, String folder) throws IOException {
        String extension = fileUtility.getImageFileExtension(file.getOriginalFilename());
        if (extension == null) {
            throw new RuntimeException("지원되지 않는 이미지 파일 형식입니다.");
        }
        return uploadToS3(file, folder, extension);
    }


    private String uploadToS3(MultipartFile file, String folder, String extension) throws IOException {
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

    public void createFolder(String folderPath) {
        getS3Client().putObject(PutObjectRequest.builder()
                .bucket(bucket)
                .key(folderPath.endsWith("/") ? folderPath : folderPath + "/")
                .build(), RequestBody.empty());
    }

    public void deleteFolder(String folderPath) {
        S3Client s3 = getS3Client();
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(folderPath.endsWith("/") ? folderPath : folderPath + "/")
                .build();

        ListObjectsV2Response listResponse = s3.listObjectsV2(listRequest);

        for (S3Object s3Object : listResponse.contents()) {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Object.key())
                    .build());
        }
    }
}
