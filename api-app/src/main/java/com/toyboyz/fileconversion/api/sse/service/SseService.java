package com.toyboyz.fileconversion.api.sse.service;


import com.toyboyz.fileconversion.api.history.entitiy.History;
import com.toyboyz.fileconversion.api.history.service.HistoryService;
import com.toyboyz.fileconversion.infra.redis.dto.SubDTO;
import com.toyboyz.fileconversion.api.sse.config.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

    private final SseEmitterRegistry sseEmitterRegistry;
    private final HistoryService historyService;
    private final StringRedisTemplate redisTemplate;

    private static final String STREAM_KEY_PREFIX = "progress:";

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleHistoryEvent(History history) { //[Ref]엔티티를 직접 주지말고 dto 에 담아서 주자
        notify(history);
    }


    //ApplicationEventPublisher을 통해 커밋이 완료된 데이터를 대상으로 업데이트된 값을 프론트로 전송
    public void notify(History history) {
        String uuid = history.getUuid();
        SseEmitter emitter = sseEmitterRegistry.getEmitter(uuid);

        if (emitter == null) {
            log.warn("SSE 연결이 존재하지 않습니다. 사용자 ID : {}", uuid);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("history-update")
                    .data(Map.of(
                            "id", history.getHistoryId(),
                            "status",history.getStatus(),
                            "message", history.getStatus()
                            )));
            log.info("Notify 메서드 실행됨! ID: {}, Status: {}", history.getHistoryId(), history.getStatus());
        } catch (IOException e) {
            log.info("SSE 연결이 끊어졌습니다. 사용자 ID : {}",uuid);
            emitter.completeWithError(e);
            sseEmitterRegistry.removeEmitter(uuid);
        }
    }


    //worker 서버에서 작업하며 발생한 이벤트를 받아 프론트로 전송
    //변환 중(status : 2)
    //변환 완료(status : 3)
    //변환 실패(status : 9)
    public void notifyRedis(SubDTO subDTO) {
        String uuid = subDTO.getUuid();

        // SSE 연결 여부와 무관하게 Stream에 이벤트 저장 (재연결 시 재전송 용도)
        Map<String, String> streamFields = new HashMap<>();
        streamFields.put("uuid", uuid);
        streamFields.put("fileName", subDTO.getFileName() != null ? subDTO.getFileName() : "");
        streamFields.put("percent", String.valueOf(subDTO.getPercent()));
        streamFields.put("convertedFile", subDTO.getConvertedFile() != null ? subDTO.getConvertedFile() : "");
        streamFields.put("status", subDTO.getStatus());
        RecordId recordId = redisTemplate.opsForStream().add(STREAM_KEY_PREFIX + uuid, streamFields);
        String streamId = recordId.getValue();

        SseEmitter emitter = sseEmitterRegistry.getEmitter(uuid);
        if (emitter == null) return;

        try {
            //null safe 를 위한 HashMap 자료구조로 리팩토링
            Map<String, Object> data = new HashMap<>();
            data.put("uuid", uuid);
            data.put("fileName", subDTO.getFileName());
            data.put("percent", subDTO.getPercent());
            data.put("convertedFile", subDTO.getConvertedFile());
            data.put("status", subDTO.getStatus());
            emitter.send(SseEmitter.event()
                    .id(streamId)  // Stream ID를 SSE event id로 사용 (Last-Event-ID 기반 재연결용)
                    .name("redis-caching-update")
                    .data(data));
            log.info("sse 전송 완료");

            //history 업데이트 메서드 호출
            //history pk 필요함
            historyService.updateHistoryEvent(subDTO);
        } catch (IOException e) {
            sseEmitterRegistry.removeEmitter(uuid); //[Ref] 변환 진행 중일 때 예외 처리의 경우 sse 가 끊어지면 안됨
        }
    }

    // SSE 재연결 시 못 받은 이벤트 재전송
    public void replayMissedEvents(String uuid, String lastEventId, SseEmitter emitter) {
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                .range(STREAM_KEY_PREFIX + uuid,
                        Range.from(Range.Bound.exclusive(lastEventId)).to(Range.Bound.unbounded()));

        if (records == null || records.isEmpty()) return;

        for (MapRecord<String, Object, Object> record : records) {
            Map<Object, Object> fields = record.getValue();
            Map<String, Object> data = new HashMap<>();
            data.put("uuid", fields.get("uuid"));
            data.put("fileName", fields.get("fileName"));
            data.put("percent", Integer.parseInt((String) fields.get("percent")));
            data.put("convertedFile", fields.get("convertedFile"));
            data.put("status", fields.get("status"));

            try {
                emitter.send(SseEmitter.event()
                        .id(record.getId().getValue())
                        .name("redis-caching-update")
                        .data(data));
            } catch (IOException e) {
                sseEmitterRegistry.removeEmitter(uuid);
                return;
            }
        }
        log.info("[SSE] 미전송 이벤트 {}개 재전송 완료 - uuid: {}", records.size(), uuid);
    }
}

