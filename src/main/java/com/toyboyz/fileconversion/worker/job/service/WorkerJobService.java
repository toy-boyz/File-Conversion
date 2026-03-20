package com.toyboyz.fileconversion.worker.job.service;

import com.toyboyz.fileconversion.worker.job.entity.WorkerJob;
import com.toyboyz.fileconversion.worker.job.repository.WorkerJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WorkerJobService {

    private final WorkerJobRepository workerJobRepository;

//    @Transactional("secondaryTransactionManager")
//    public WorkerJob createUploadingJob(Long historyId, String uuid, String s3Key, String requestFormat) {
//        WorkerJob job = WorkerJob.builder()
//                .historyId(historyId)
//                .uuid(uuid)
//                .s3Key(s3Key)
//                .requestFormat(requestFormat)
//                .status("UPLOADING")
//                .retryCount(0)
//                .createdAt(LocalDateTime.now())
//                .updatedAt(LocalDateTime.now())
//                .build();
//
//        return workerJobRepository.save(job);
//    }
}