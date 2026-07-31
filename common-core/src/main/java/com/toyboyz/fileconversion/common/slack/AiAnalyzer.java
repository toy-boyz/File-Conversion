package com.toyboyz.fileconversion.common.slack;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AiAnalyzer {

    private final WebClient webClient;
    private final String apiKey;

    public AiAnalyzer(@Value("${claude.api.key:}") String apiKey, WebClient.Builder webClientBuilder) {
        this.apiKey = apiKey;
        this.webClient = webClientBuilder.baseUrl("https://api.anthropic.com").build();
    }

    public String analyze(String errorMessage) {
        if (apiKey.isBlank()) {
            return "AI 분석 불가 (API 키 미설정)";
        }

        Map<String, Object> requestBody = Map.of(
                "model", "claude-haiku-4-5-20251001",
                "max_tokens", 512,
                "messages", List.of(
                        Map.of("role", "user", "content",
                                "파일 변환 서비스에서 다음 메시지가 DLQ로 이관됐습니다. 에러 원인과 해결 방법을 간략히 분석해주세요:\n" + errorMessage)
                )
        );

        try {
            Map<?, ?> response = webClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            return (String) content.get(0).get("text");
        } catch (Exception e) {
            log.error("[AI] 분석 실패: {}", e.getMessage());
            return "AI 분석 실패";
        }
    }
}
