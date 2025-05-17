package com.ghostHoliday.graduationExhibitions.utility;

import com.ghostHoliday.graduationExhibitions.dto.post.CompletedPartDTO;
import com.ghostHoliday.graduationExhibitions.dto.post.MultipartUploadDTO;
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
import software.amazon.awssdk.services.s3.presigner.model.*;

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
                .prefix(prefix)
                .build();

        ListObjectsV2Response listResponse = s3.listObjectsV2(listRequest);
        List<S3Object> allFiles = listResponse.contents();

        System.out.println("전체 파일 수: " + allFiles.size());
        allFiles.forEach(file -> System.out.println(" - " + file.key()));

        if (allFiles.size() <= 1) return;

        List<S3Object> sorted = allFiles.stream()
                .sorted(Comparator.comparing(S3Object::key).reversed())
                .collect(Collectors.toList());

        List<S3Object> toDelete = sorted.subList(1, sorted.size());

        List<ObjectIdentifier> deleteKeys = toDelete.stream()
                .map(obj -> ObjectIdentifier.builder().key(obj.key()).build())
                .collect(Collectors.toList());

        DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(Delete.builder()
                        .objects(deleteKeys)
                        .build())
                .build();

        DeleteObjectsResponse response = s3.deleteObjects(deleteRequest);
        System.out.println("삭제 완료: " + response.deleted().size() + "개 파일");
    }


    public MultipartUploadDTO initiateMultipartUpload(String path, String contentType, String extension, long fileSize, long partSize) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String key = path + "_" + timestamp + "." + extension;

        S3Client s3 = getS3Client();
        S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();

        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        CreateMultipartUploadResponse response = s3.createMultipartUpload(createRequest);
        String uploadId = response.uploadId();

        int totalParts = (int) Math.ceil((double) fileSize / partSize);
        List<MultipartUploadDTO.PartUrl> presignedUrls = new java.util.ArrayList<>();

        for (int partNumber = 1; partNumber <= totalParts; partNumber++) {
            UploadPartRequest partRequest = UploadPartRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .build();

            PresignedUploadPartRequest presignedPart = presigner.presignUploadPart(
                    UploadPartPresignRequest.builder()
                            .uploadPartRequest(partRequest)
                            .signatureDuration(Duration.ofMinutes(15))
                            .build());

            presignedUrls.add(new MultipartUploadDTO.PartUrl(partNumber, presignedPart.url().toString()));
        }

        s3.close();
        presigner.close();

        return new MultipartUploadDTO(key, uploadId, presignedUrls, cloudFrontUrl + "/" + key);
    }

    public void completeMultipartUpload(String key, String uploadId, List<CompletedPartDTO> parts) {
        S3Client s3 = getS3Client();

        List<CompletedPart> completedParts = parts.stream()
                .map(p -> CompletedPart.builder()
                        .partNumber(p.getPartNumber())
                        .eTag(p.getETag())
                        .build())
                .collect(Collectors.toList());

        CompletedMultipartUpload completed = CompletedMultipartUpload.builder()
                .parts(completedParts)
                .build();

        CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(completed)
                .build();

        s3.completeMultipartUpload(request);
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



    public S3Client getS3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
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
