package com.cho.polio.presentation.file.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UploadPresignedUrlResponse {
    private String presignedUrl;
    private String key;
}