package com.cho.polio.presentation.file.repository;

import com.cho.polio.presentation.file.entity.UploadFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UploadFileRepository extends JpaRepository<UploadFile, Long> {
    Optional<UploadFile> findByKey(String key);
    boolean existsByKey(String key);
    List<UploadFile> findByUploadBy(String uploadBy);

    Optional<UploadFile> findByIdAndUploadBy(Long fileId, String userId);
}