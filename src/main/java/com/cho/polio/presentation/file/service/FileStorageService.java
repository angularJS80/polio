package com.cho.polio.presentation.file.service;

import com.cho.polio.presentation.file.dto.UploadPresignedUrlResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String uploadFile(MultipartFile file, String bucketName);
    String uploadFile(MultipartFile file);
    String uploadFile(MultipartFile file, ResponseType type);
    String getPreSignedUrl(String filePath);
    String getPublicUrl(String key);
    UploadPresignedUrlResponse generateUploadPresignedUrl(String fileName, String contentType, String uploadBy,
            String visibility);
    String changeFileVisibility(String currentKey,String visibility);

}