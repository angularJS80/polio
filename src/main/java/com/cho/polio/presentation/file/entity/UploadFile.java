package com.cho.polio.presentation.file.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "upload_file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UploadFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_key", nullable = false, unique = true)
    private String key;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    @Column
    private Long fileSize;

    @Column(nullable = false)
    private String uploadBy;

    @Column(nullable = false)
    private String visibility;


    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UploadStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    @Builder
    public UploadFile(String key, String fileName, String contentType, Long fileSize, String uploadBy,String visibility) {
        this.key = key;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadBy = uploadBy;
        this.visibility = visibility;
        this.status = UploadStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = UploadStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = UploadStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }

    public void updateKey(String newKey) {
        this.key = newKey;
    }

    public void updateVisibility(String visibility) {
        this.visibility = visibility;
    }
}