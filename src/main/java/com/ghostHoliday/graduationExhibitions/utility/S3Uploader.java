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
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest;
import software.amazon.awssdk.services.cloudfront.model.InvalidationBatch;
import software.amazon.awssdk.services.cloudfront.model.Paths;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.DeleteObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedDeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public void cleanupOldVersions(String prefix) {
        S3Client s3 = getS3Client();

        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix) // 예: poster_
                .build();

        ListObjectsV2Response listResponse = s3.listObjectsV2(listRequest);
        List<S3Object> allFiles = listResponse.contents();

        System.out.println("전체 파일 수: " + allFiles.size());
        allFiles.forEach(file -> System.out.println(" - " + file.key()));

        if (allFiles.size() <= 1) return;

        List<S3Object> sorted = allFiles.stream()
                .sorted(Comparator.comparing(S3Object::key).reversed()) // 파일명 기준 정렬
                .collect(Collectors.toList());

        List<S3Object> toDelete = sorted.subList(1, sorted.size());

        for (S3Object obj : toDelete) {
            System.out.println("삭제 대상: " + obj.key());
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(obj.key())
                    .build());
        }
    }



    public UploadUrlDTO generatePreSignedUploadUrl(String path, String contentType, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String key = path + "_" + timestamp + "." + extension;
        S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType) // 혹은 필요한 타입
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

//    public void invalidateCloudFront(String path) {
//        CloudFrontClient cloudFrontClient = CloudFrontClient.builder()
//                .region(Region.AP_NORTHEAST_2)
//                .credentialsProvider(StaticCredentialsProvider.create(
//                        AwsBasicCredentials.create(accessKey, secretKey)))
//                .build();
//
//        CreateInvalidationRequest invalidationRequest = CreateInvalidationRequest.builder()
//                .distributionId("E2BX69LH0CHB4A")
//                .invalidationBatch(InvalidationBatch.builder()
//                        .callerReference(String.valueOf(System.currentTimeMillis()))
//                        .paths(Paths.builder()
//                                .quantity(1)
//                                .items(path) // 예: "/teamPost/uuid/poster"
//                                .build())
//                        .build())
//                .build();
//
//        cloudFrontClient.createInvalidation(invalidationRequest);
//        cloudFrontClient.close();
//    }


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
        return uploadToS3(file, folder + "." + extension);
    }


    private String uploadToS3(MultipartFile file, String key) throws IOException {

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
