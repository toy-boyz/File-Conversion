package com.toyboyz.fileconversion.api.history.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyboyz.fileconversion.api.file.dto.request.UploadInitRequest;
import com.toyboyz.fileconversion.api.history.entity.History;
import com.toyboyz.fileconversion.api.history.repository.HistoryRepository;
import com.toyboyz.fileconversion.debezium.entity.OutboxEvent;
import com.toyboyz.fileconversion.debezium.repository.OutboxEventRepository;
import com.toyboyz.fileconversion.worker.job.entity.WorkerJob;
import com.toyboyz.fileconversion.worker.job.repository.WorkerJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryService {

    private final HistoryRepository historyRepository;
    private final WorkerJobRepository workerJobRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /** upload-init 단계: status=1, originalFile=S3 key 저장 */
    @Transactional
    public List<History> createPendingHistories(UploadInitRequest req, List<String> s3Keys, List<String> s3FileNames) {
        if (req.files().size() != s3Keys.size() || req.files().size() != s3FileNames.size()) {
            throw new IllegalArgumentException("file size mismatch");
        }

        List<History> saveList = new ArrayList<>();
        for (int i = 0; i < req.files().size(); i++) {
            UploadInitRequest.FileMeta f = req.files().get(i);

            String ext = extractExtension(f.filename());

            History h = History.builder()
                    .uuid(req.uuid())
                    .fileName(f.filename())
                    .requestFormat(req.targetFormat())
                    .s3FileName(s3FileNames.get(i))
                    .originalFile(s3Keys.get(i))
                    .originalFormat(ext)
                    .status("1")                 // 대기/업로드 준비
                    .build();
            saveList.add(h);
        }

        List<History> saved = historyRepository.saveAll(saveList);
        saved.forEach(eventPublisher::publishEvent);
        return saved;
    }

    /** payload JSON 만들어 worker_job 테이블에 저장 + history 상태 변경 */
    @Transactional
    public void markUploadedAndCreateWorker(List<History> histories) {
        List<WorkerJob> workerList = new ArrayList<>();

        for (History h : histories) {
            // 이미 처리된 것이면 skip
            if (!"1".equals(h.getStatus())) {
                continue;
            }

            h.setStatus("2"); // 변환 진행 중
            eventPublisher.publishEvent(h);

            Map<String, Object> payload = new HashMap<>();
            payload.put("historyId", h.getHistoryId());
            payload.put("uuid", h.getUuid());
            payload.put("s3Key", h.getOriginalFile());
            payload.put("originalFormat", h.getOriginalFormat());
            payload.put("requestFormat", h.getRequestFormat());
            payload.put("fileName", h.getS3FileName());

//            outboxList.add(OutboxEvent.of("file", h.getHistoryId(), "fileConvert", toJson(payload)));
            workerList.add(WorkerJob.of(toJson(payload)));
        }

            historyRepository.saveAll(histories);
            workerJobRepository.saveAll(workerList);
//            outboxEventRepository.saveAll(outboxList);

    }

    @Transactional(readOnly = true)
    public List<History> findHistories(String uuid) {
        return historyRepository.findAllByUuidAndStatus(uuid,"1");
    }

    @Transactional
    public void updateStatus(Long id) {
        History history = historyRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("해당 기록이 없습니다."));
        history.setStatus("3");
        historyRepository.save(history);
        eventPublisher.publishEvent(history);
    }

    @Transactional(readOnly = true)
    public List<History> getByIds(List<Long> ids) {
        return historyRepository.findAllById(ids);
    }

    @Transactional(readOnly = true)
    public List<History> findAllByUuidAndStatus(String uuid) {
        return historyRepository.findAllByUuidAndStatus(uuid, "2");
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null) return null;
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return null;
        return filename.substring(idx + 1).toLowerCase();
    }
}
