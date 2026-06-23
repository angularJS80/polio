package com.cho.polio.presentation.file.controller;

import com.cho.polio.presentation.file.dto.UploadFileListResponse;
import com.cho.polio.presentation.file.dto.UploadPresignedUrlResponse;
import com.cho.polio.presentation.file.entity.UploadFile;
import com.cho.polio.presentation.file.service.FileStorageService;
import com.cho.polio.presentation.file.service.ResponseType;
import com.cho.polio.presentation.file.service.UploadFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "File", description = "파일 업로드 및 관리 API")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final UploadFileService uploadFileService;

    @Operation(
        summary = "파일 업로드",
        description = "S3에 파일을 업로드하고 ResponseType에 따라 KEY 또는 PUBLIC_URL을 반환합니다.",
        parameters = {
            @Parameter(name = "type", in = ParameterIn.PATH, description = "응답 타입 (KEY: S3 key만 반환, PUBLIC_URL: 공개 URL 반환)", required = true, schema = @Schema(implementation = ResponseType.class)),
            @Parameter(name = "file", in = ParameterIn.DEFAULT, description = "업로드할 파일", required = true, content = @Content(mediaType = "multipart/form-data"))
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "업로드 성공", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", description = "S3 key 또는 공개 URL")))
        }
    )
    @PostMapping("/upload/{type}")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file, @PathVariable ResponseType type) {
        String result = fileStorageService.uploadFile(file, type);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(result);
    }

    @Operation(
        summary = "Presigned URL 생성 및 리다이렉트",
        description = "S3 key를 받아 presigned URL을 생성하고 302 리다이렉트합니다.",
        parameters = {
            @Parameter(name = "filePath", in = ParameterIn.QUERY, description = "S3 key 또는 파일 경로", required = true, schema = @Schema(type = "string"))
        },
        responses = {
            @ApiResponse(responseCode = "302", description = "S3 presigned URL로 리다이렉트")
        }
    )
    @GetMapping("/presigned-url")
    public ResponseEntity<String> getPreSignedUrl(@RequestParam("filePath") String filePath) {
        String preSignedUrl = fileStorageService.getPreSignedUrl(filePath);
        // 리다이렉트 대신 URL 문자열만 반환 (200 OK)
        return ResponseEntity.ok(preSignedUrl);
    }

    @Operation(
        summary = "업로드용 Presigned URL 생성",
        description = "파일명과 Content-Type을 받아 S3 업로드용 presigned URL을 생성합니다. (10분 유효)",
        parameters = {
            @Parameter(name = "fileName", in = ParameterIn.QUERY, description = "업로드할 파일명 (확장자 포함)", required = true, schema = @Schema(type = "string")),
            @Parameter(name = "contentType", in = ParameterIn.QUERY, description = "파일 MIME 타입 (예: image/jpeg)", required = true, schema = @Schema(type = "string"))
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "업로드용 presigned URL 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UploadPresignedUrlResponse.class)))
        }
    )
    @GetMapping("/upload-presigned-url")
    public ResponseEntity<UploadPresignedUrlResponse> getUploadPresignedUrl(@RequestParam String fileName,
            @RequestParam String contentType, @RequestParam String visibility, Authentication authentication) {
        // JWT 토큰에서 sub(사용자 ID) 추출
        String uploadBy = authentication.getName();

        UploadPresignedUrlResponse response = fileStorageService.generateUploadPresignedUrl(fileName, contentType,
                uploadBy,visibility);

        // DB에 PENDING 상태로 저장
        uploadFileService.createPendingUpload(response.getKey(), fileName, contentType, null, uploadBy,visibility);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
    @PutMapping("/{fileId}/visibility")
    public ResponseEntity<Void> updateVisibility(
            @PathVariable Long fileId,
            @RequestParam String visibility,
            Authentication authentication) {

        String userId = authentication.getName();

        // 파일을 찾지 못하거나 권한이 없으면 404를 반환
        uploadFileService.findByIdAndUploadBy(fileId, userId)
                .map(UploadFile::getKey)
                .map(key-> fileStorageService.changeFileVisibility(key,visibility)) // 인자 추가 필요시
                .ifPresentOrElse(
                        newKey -> uploadFileService.updateVisibility(fileId, newKey, visibility),
                        () -> { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."); }
                );

        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "업로드 완료 처리",
        description = "S3 업로드 완료 후 상태를 COMPLETED로 업데이트합니다.",
        parameters = {
            @Parameter(name = "key", in = ParameterIn.QUERY, description = "S3 파일 key", required = true, schema = @Schema(type = "string"))
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "완료 처리 성공")
        }
    )
    @PostMapping("/complete-upload")
    public ResponseEntity<Void> completeUpload(@RequestParam String key) {
        uploadFileService.completeUpload(key);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "사용자별 업로드 파일 목록 조회",
        description = "JWT 토큰의 사용자 ID로 업로드한 파일 목록을 조회합니다. 각 파일의 presigned URL을 함께 반환합니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UploadFileListResponse.class)))
        }
    )
    @GetMapping("/my-uploads")
    public ResponseEntity<UploadFileListResponse> getMyUploads(Authentication authentication) {
        String uploadBy = authentication.getName();
        List<UploadFile> uploadFiles = uploadFileService.findByUploadBy(uploadBy);

        List<UploadFileListResponse.UploadFileInfo> fileInfos = uploadFiles.stream()
                .map(file -> {
                    String presignedUrl = fileStorageService.getPreSignedUrl(file.getKey());
                    return new UploadFileListResponse.UploadFileInfo(
                            file.getId(),
                            file.getKey(),
                            file.getFileName(),
                            file.getContentType(),
                            file.getFileSize(),
                            file.getStatus().name(),
                            file.getUploadBy(),
                            file.getVisibility(),
                            presignedUrl
                    );
                })
                .toList();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UploadFileListResponse(fileInfos));
    }
}