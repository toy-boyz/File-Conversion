package com.toyboyz.fileconversion.api.redis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyboyz.fileconversion.api.stats.service.StatsQueryService;
import com.toyboyz.fileconversion.infra.redis.dto.SubDTO;
import com.toyboyz.fileconversion.api.sse.service.SseService;
import com.toyboyz.fileconversion.api.sse.service.StatsSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMessageHandler {

    private final SseService sseService;
    private final StatsSseService statsSseService;
    private final StatsQueryService statsQueryService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper om;

    private static final String GLOBAL_KEY = "stats:global";

    public void subProg(String message) {
        log.info("레디스 수신");
        try {
            SubDTO subDTO = om.readValue(message, SubDTO.class); //null로 들어오면 매칭 불가 -> " "
            sseService.notifyRedis(subDTO);

            // 완료 상태면 전역 통계 Redis 값 증가 및 SSE 전송
            if (isCompleted(subDTO)) {
                statsQueryService.ensureGlobalStatsKey();
                redisTemplate.opsForHash().increment(GLOBAL_KEY, "completedCount", 1);
                redisTemplate.opsForHash().increment(GLOBAL_KEY, "completedBytes", subDTO.getSize());
                statsSseService.broadcastLatestSummary();
                // 변환 완료 후 Stream 키 1시간 뒤 자동 삭제
                redisTemplate.expire("progress:" + subDTO.getUuid(), Duration.ofHours(1));
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isCompleted(SubDTO subDTO) {
        return subDTO.getPercent() == 100
                && "3".equals(subDTO.getStatus());
    }
}
