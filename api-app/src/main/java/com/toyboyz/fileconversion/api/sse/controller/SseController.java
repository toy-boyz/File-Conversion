package com.toyboyz.fileconversion.api.sse.controller;

import com.toyboyz.fileconversion.api.history.service.HistoryService;
import com.toyboyz.fileconversion.api.sse.config.SseEmitterRegistry;
import com.toyboyz.fileconversion.api.sse.service.SseService;
import com.toyboyz.fileconversion.api.sse.service.StatsSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final HistoryService historyService;
    private final StatsSseService statsSseService;



    @GetMapping(value = "/api/sse/connect/{uuid}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@PathVariable String uuid,
                              @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        // 1. Emitter 생성 (타임아웃 60분 설정 예시)
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);

        // 2. Registry에 저장 (그래야 나중에 서비스에서 찾아서 쏠 수 있음)
        sseEmitterRegistry.addEmitter(uuid, emitter);

        // 3. 연결 종료/타임아웃 시 처리
        emitter.onCompletion(() -> sseEmitterRegistry.removeEmitter(uuid));
        emitter.onTimeout(() -> sseEmitterRegistry.removeEmitter(uuid));

        // 4. 연결 직후 더미 데이터 전송 (503 에러 방지 및 연결 확인)
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected!"));
        } catch (IOException e) {
            sseEmitterRegistry.removeEmitter(uuid);
        }

        // 5. 재연결 시 못 받은 이벤트 재전송
        if (lastEventId != null) {
            sseService.replayMissedEvents(uuid, lastEventId, emitter);
        }

        return emitter;
    }

    @GetMapping(value = "/api/sse/stats", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectStats() {
        return statsSseService.connect();
    }

}
