package com.cho.polio.presentation.file.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UploadFileListResponse {
    private List<UploadFileInfo> files;

    @Getter
    @AllArgsConstructor
    public static class UploadFileInfo {
        private Long id;
        private String key;
        private String fileName;
        private String contentType;
        private Long fileSize;
        private String status;
        private String uploadBy;
        private String visibility;
        private String presignedUrl;
    }
}