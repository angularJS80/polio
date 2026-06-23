package com.cho.polio.presentation.file.service;

import com.cho.polio.presentation.file.dto.UploadPresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AwsS3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.s3.endpoint}")
    private String endpoint;

    @Override
    public String uploadFile(MultipartFile file, String bucketName) {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String s3FileName = UUID.randomUUID().toString() + extension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3FileName)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 실패", e);
        }

        return s3FileName;
    }

    public String uploadFile(MultipartFile file) {
        return uploadFile(file, this.bucketName);
    }

    @Override
    public String uploadFile(MultipartFile file, ResponseType type) {
        String key = uploadFile(file);

        if (ResponseType.PUBLIC_URL == type) {
            return getPublicUrl(key);
        }

        return key;
    }

    @Override
    public String getPublicUrl(String key) {
        return buildFilePath(key);
    }

    @Override
    public String getPreSignedUrl(String key) {
        String presignedUrl = s3Presigner.presignGetObject(builder -> builder
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build())
                .signatureDuration(Duration.ofHours(1)))
                .url()
                .toString();

        return convertToPathStyle(presignedUrl);
    }

    private String convertToPathStyle(String presignedUrl) {
        URI uri = URI.create(presignedUrl);
        String host = uri.getHost();
        String path = uri.getPath();
        String query = uri.getRawQuery();

        if (host != null && host.startsWith(bucketName + ".")) {
            String newHost = host.substring(bucketName.length() + 1);
            String newPath = path.substring(bucketName.length());
            return URI.create(endpoint).resolve(newPath).toString() + (query != null ? "?" + query : "");
        }

        return presignedUrl;
    }

    @Override
    public UploadPresignedUrlResponse generateUploadPresignedUrl(String fileName, String contentType, String uploadBy
            ,String visibility) {
        // 1. 경로를 포함한 Key 생성: private/{sub}/{uuid}{ext}
        String key = String.format("%s/%s/%s%s",visibility,
                uploadBy,
                UUID.randomUUID().toString(),
                getFileExtension(fileName));

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key) // 여기서 경로가 포함됨
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(putObjectRequest)
                .signatureDuration(Duration.ofMinutes(10))
                .build();

        String presignedUrl = s3Presigner.presignPutObject(presignRequest)
                .url()
                .toString();

        String convertedUrl = convertToPathStyle(presignedUrl);

        // 이제 key에는 'private/{sub}/...' 경로가 포함되어 저장됩니다.
        return new UploadPresignedUrlResponse(convertedUrl, key);
    }

    @Override
    public String changeFileVisibility(String currentKey, String newVisibility) {
        // 1. 키 구조 분리: {visibility}/{userId}/{fileName}
        String[] parts = currentKey.split("/", 3);

        if (parts.length < 3) {
            throw new IllegalArgumentException("올바르지 않은 파일 키 형식입니다: " + currentKey);
        }

        // 2. 앞부분(visibility)만 새로운 값으로 교체하여 조합
        //    parts[1]은 userId, parts[2]는 fileName
        String newKey = newVisibility + "/" + parts[1] + "/" + parts[2];

        // 3. S3 객체 이동 (Copy -> Delete)
        s3Client.copyObject(req -> req
                .sourceBucket(bucketName)
                .sourceKey(currentKey)
                .destinationBucket(bucketName)
                .destinationKey(newKey));

        s3Client.deleteObject(req -> req
                .bucket(bucketName)
                .key(currentKey));

        return newKey;
    }

    private String getFileExtension(String fileName) {
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return fileName.substring(lastIndexOf);
    }

    public String buildFilePath(String fileName) {
        return String.format("%s/%s/%s", endpoint.replaceAll("/$", ""), bucketName, fileName);
    }
}