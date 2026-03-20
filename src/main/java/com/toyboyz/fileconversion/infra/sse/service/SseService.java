package com.toyboyz.fileconversion.infra.sse.service;

import com.toyboyz.fileconversion.api.history.entity.History;
import com.toyboyz.fileconversion.infra.redis.dto.SubDTO;
import com.toyboyz.fileconversion.infra.redis.service.RedisService;
import com.toyboyz.fileconversion.infra.sse.config.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

    private final SseEmitterRegistry sseEmitterRegistry;


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleHistoryEvent(History history) { //엔티티를 직접 주지말고 dto 에 담아서 주자
        notify(history);
    }


    //rdb 갱신 시 실행
    //id 별 emitter 보관
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


    public void notifyRedis(SubDTO subDTO) {
        String uuid = subDTO.getUuid();
        SseEmitter emitter = sseEmitterRegistry.getEmitter(uuid);

        if (emitter == null) return;

        try {
            //null safe 를 위한 HashMap 자료구조로 리팩토링
            Map<String, Object> data = new HashMap<>();
            data.put("uuid", uuid);
            data.put("filename", subDTO.getFilename());
            data.put("percent", subDTO.getPercent());
            data.put("convertedFile", subDTO.getConvertedFile());
            data.put("status", subDTO.getStatus());

            System.out.println(data.get("convertedFile"));
            emitter.send(SseEmitter.event()
                    .name("redis-caching-update")
                    .data(data));

            log.info("sse 전송 완료");

        } catch (IOException e) {
            sseEmitterRegistry.removeEmitter(uuid); //[수정 예정] 변환 진행 중일 때 예외 처리의 경우 sse 가 끊어지면 안됨
        }
    }
}

