package com.example.chat.kafka;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.example.chat.dto.request.ChatMessageRequestDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaChatDlqConsumer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaChatConsumer kafkaChatConsumer;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.max-retry-count:3}")
    private int maxRetryCount;

    @Value("${app.kafka.retry-delay-multiplier:2}")
    private int retryDelayMultiplier;

    @KafkaListener(topics = "chat-message.DLQ", groupId = "chat-dlq-group")
    public void consumeDlqMessage(String message) {
        log.warn("🔄 DLQ 메시지 재처리 시작: {}", message);
        
        try {
            JsonNode node = kafkaChatConsumer.extractRetryInfo(message);
            if (node == null) {
                log.error("❌ 메시지 파싱 실패로 재처리 불가: {}", message);
                return;
            }

            int retryCount = node.has("retryCount") ? node.get("retryCount").asInt() : 0;
            String errorType = node.has("errorType") ? node.get("errorType").asText() : "UNKNOWN";
            String errorMessage = node.has("errorMessage") ? node.get("errorMessage").asText() : "";
            
            log.info("📊 재처리 정보: retryCount={}, errorType={}, errorMessage={}", 
                retryCount, errorType, errorMessage);
            
            // 최대 재시도 횟수 확인
            if (kafkaChatConsumer.isMaxRetryExceeded(retryCount)) {
                handleMaxRetryExceeded(message, retryCount, errorType, errorMessage);
                return;
            }
            
            // 에러 타입별 처리
            if ("JSON_PARSE_ERROR".equals(errorType)) {
                handleJsonParseError(message, retryCount, errorMessage);
                return;
            }
            
            // 지수 백오프를 통한 재시도 지연
            int delaySeconds = kafkaChatConsumer.calculateRetryDelay(retryCount);
            log.info("⏰ 재시도 지연: {}초 후 재처리 시작", delaySeconds);
            
            try {
                Thread.sleep(delaySeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("재시도 지연 중 인터럽트 발생");
            }
            
            // retryCount 증가 후 다시 처리
            ((ObjectNode) node).put("retryCount", retryCount + 1);
            ((ObjectNode) node).put("lastRetryAt", LocalDateTime.now().toString());
            
            kafkaChatConsumer.processMessage(node.toString());
            
            log.info("✅ DLQ 메시지 재처리 성공: retryCount={}, message={}", retryCount + 1, message);
            
        } catch (Exception e) {
            log.error("❌ DLQ 메시지 재처리 실패: {}", message, e);
            handleRetryFailure(message, e);
        }
    }

    /**
     * 최대 재시도 횟수 초과 처리
     */
    private void handleMaxRetryExceeded(String message, int retryCount, String errorType, String errorMessage) {
        log.error("🚨 DLQ 메시지 최대 재시도 횟수 초과: retryCount={}, errorType={}, errorMessage={}", 
            retryCount, errorType, errorMessage);
        
        // TODO: 영구 보관 처리
        // saveToPermanentStorage(message, retryCount, errorType, errorMessage);
        
        // TODO: 알림 서비스 호출
        // sendNotification(message, retryCount, errorType, errorMessage);
        
        log.error("📦 메시지 영구 보관 필요: {}", message);
    }

    /**
     * JSON 파싱 에러 처리 (재시도 의미없음)
     */
    private void handleJsonParseError(String message, int retryCount, String errorMessage) {
        log.error("🚫 JSON 파싱 에러는 재시도 의미없음: retryCount={}, errorMessage={}", 
            retryCount, errorMessage);
        
        // JSON 파싱 에러는 재시도해도 해결되지 않으므로 즉시 영구 보관
        handleMaxRetryExceeded(message, retryCount, "JSON_PARSE_ERROR", errorMessage);
    }

    /**
     * 재시도 실패 처리
     */
    private void handleRetryFailure(String message, Exception e) {
        try {
            JsonNode node = kafkaChatConsumer.extractRetryInfo(message);
            if (node == null) {
                log.error("❌ 재시도 실패 메시지 파싱 불가: {}", message);
                return;
            }
            
            int retryCount = node.has("retryCount") ? node.get("retryCount").asInt() : 0;
            String errorType = node.has("errorType") ? node.get("errorType").asText() : "RETRY_FAILURE";
            String errorMessage = e.getMessage();
            
            // 증가된 retryCount로 다시 DLQ로 전송
            ((ObjectNode) node).put("retryCount", retryCount + 1);
            ((ObjectNode) node).put("errorType", errorType);
            ((ObjectNode) node).put("errorMessage", errorMessage);
            ((ObjectNode) node).put("lastRetryAt", LocalDateTime.now().toString());
            
            kafkaTemplate.send("chat-message.DLQ", node.toString());
            log.error("🔄 재시도 실패로 DLQ 토픽으로 메시지 재전송: retryCount={}, errorType={}", 
                retryCount + 1, errorType);
                
        } catch (Exception e2) {
            log.error("❌ DLQ 토픽으로 메시지 재전송 실패: {}", message, e2);
        }
    }

    /**
     * 영구 보관 처리 (향후 구현)
     */
    private void saveToPermanentStorage(String message, int retryCount, String errorType, String errorMessage) {
        // TODO: FailedMessage 엔티티 생성 및 저장
        log.info("💾 메시지 영구 보관: retryCount={}, errorType={}", retryCount, errorType);
    }

    /**
     * 알림 서비스 호출 (향후 구현)
     */
    private void sendNotification(String message, int retryCount, String errorType, String errorMessage) {
        // TODO: Slack, 이메일 등 알림 서비스 호출
        log.info("📢 알림 전송: retryCount={}, errorType={}", retryCount, errorType);
    }
}
