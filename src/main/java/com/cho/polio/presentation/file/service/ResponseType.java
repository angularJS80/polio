package com.cho.polio.presentation.file.service;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파일 업로드 응답 타입")
public enum ResponseType {
    @Schema(description = "S3 key만 반환 (비공개 파일, presigned URL API를 통해 접근)")
    KEY,

    @Schema(description = "공개 URL 반환 (공개 버킷의 파일, img src에 직접 사용 가능)")
    PUBLIC_URL
}