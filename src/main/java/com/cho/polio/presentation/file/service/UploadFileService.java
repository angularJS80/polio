package com.cho.polio.presentation.file.service;

import com.cho.polio.presentation.file.entity.UploadFile;
import com.cho.polio.presentation.file.repository.UploadFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UploadFileService {

    private final UploadFileRepository uploadFileRepository;

    @Transactional
    public UploadFile createPendingUpload(String key, String fileName, String contentType, Long fileSize,
            String uploadBy,String visibility) {
        UploadFile uploadFile = UploadFile.builder()
                .key(key)
                .fileName(fileName)
                .contentType(contentType)
                .fileSize(fileSize)
                .uploadBy(uploadBy)
                .visibility(visibility)

                .build();
        return uploadFileRepository.save(uploadFile);
    }

    @Transactional
    public void completeUpload(String key) {
        UploadFile uploadFile = uploadFileRepository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Upload file not found: " + key));
        uploadFile.complete();
    }


    public Optional<UploadFile> findById(Long key) {
        return uploadFileRepository.findById(key);
    }

    public List<UploadFile> findByUploadBy(String uploadBy) {
        return uploadFileRepository.findByUploadBy(uploadBy);
    }

    public boolean existsById(String key) {
        return uploadFileRepository.existsByKey(key);
    }

    @Transactional
    public void updateVisibility(Long fileId, String newKey, String visibility) {
        UploadFile file = uploadFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다."));

        file.updateKey(newKey);
        file.updateVisibility(visibility);

        // save()는 JpaRepository를 사용 중이라면 @Transactional 안에서
        // Dirty Checking(변경 감지)에 의해 자동으로 처리되므로 생략 가능합니다.
    }

    public Optional<UploadFile> findByIdAndUploadBy(Long fileId, String userId) {
        return uploadFileRepository.findByIdAndUploadBy(fileId,userId);
    }
}