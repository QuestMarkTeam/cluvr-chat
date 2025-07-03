package com.example.chat.kafka;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.example.chat.dto.request.ChatMessageRequestDto;
import com.example.chat.entity.ChatLog;
import com.example.chat.repository.ChatLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaChatConsumer {
	private final ChatLogRepository chatLogRepository;
	private final ObjectMapper objectMapper;
	private final SimpMessagingTemplate messagingTemplate;
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final StringRedisTemplate redisTemplate;

	@Value("${app.kafka.max-retry-count:3}")
	private int maxRetryCount;

	@Value("${app.kafka.retry-delay-multiplier:2}")
	private int retryDelayMultiplier;

	// Kafka 토픽에서 메세지를 읽기
	// 이 메세지를 MongoDB로 저장
	// @KafkaListener 스프링이 자동으로 kafka에서 메세지가 push 해 줄 때 실행 시켜줌
	@KafkaListener(topics = "chat-message", groupId = "chat-group")
	public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
		log.warn("🔥🔥🔥 consume() called with message: {}", record.value());
		String message = record.value();
		log.info("🥕 Kafka 메시지 수신: {}", message);

		try {
			processMessage(message);
			ack.acknowledge();
		} catch (JsonProcessingException e) {
			log.warn("❗역직렬화 실패 → DLQ로 이동: {}", message, e);
			// JSON 파싱 실패는 재시도해도 의미없으므로 즉시 DLQ로 이동
			handleDlq(message, 0, "JSON_PARSE_ERROR", e.getMessage());
			ack.acknowledge();
		} catch (Exception e) {
			log.error("❌ 메시지 처리 중 오류 → DLQ로 이동: {}", message, e);
			// 기타 에러는 재시도 가능하므로 retryCount를 0으로 시작
			handleDlq(message, 0, "PROCESSING_ERROR", e.getMessage());
			ack.acknowledge();
		}
	}

	private void handleDlq(String message, int retryCount, String errorType, String errorMessage) {
		try {
			String dlqMessage = addRetryInfoToMessage(message, retryCount, errorType, errorMessage);
			kafkaTemplate.send("chat-message.DLQ", dlqMessage);
			log.info("📦 DLQ 토픽 전송 성공: retryCount={}, errorType={}, message={}", 
				retryCount, errorType, dlqMessage);
		} catch (Exception e) {
			log.error("❌ DLQ 전송 실패 (원본 메시지 유지): {}", message, e);
		}
	}

	// 실제 메시지 처리 로직 분리
	public void processMessage(String message) throws Exception {
		ChatMessageRequestDto dto = objectMapper.readValue(message, ChatMessageRequestDto.class);
		String idempotencyKey = "chat:processed:" + dto.getMessageId();
		String processedStatus = redisTemplate.opsForValue().get(idempotencyKey);
		Boolean alreadyProcessed = processedStatus != null;

		// 1.	처리 전에 Redis에 "이 메시지 ID는 이미 처리했는지" 확인함.
		// 2.	만약 처리된 적이 있다면 return으로 무시.
		// 3.	아니라면 Redis에 5분짜리 임시 키를 먼저 저장 → 중복 방지.
		// 4.	메시지 처리 완료 후엔 다시 TTL을 1일로 갱신해서 이 메시지를 다시 처리하지 않도록 함.
		if (alreadyProcessed) {
			log.debug("이미 처리된 메시지 건너뜁니다. messageId={}", dto.getMessageId());
			return;
		}
		try {
			// 처리 중 상태로 마킹 (10분)
			redisTemplate.opsForValue().set(idempotencyKey, "processing", 10, TimeUnit.MINUTES);
			// 채팅 로그 저장
			ChatLog chatLog = new ChatLog(dto.getRoomId(), dto.getUserId(), dto.getNickname(), dto.getMessage(),
					dto.getType(), LocalDateTime.now());
			log.info("[KafkaConsumer] DB 저장 시도: {}", chatLog);
			chatLogRepository.save(chatLog);
			log.info("[KafkaConsumer] DB 저장 성공: {}", chatLog);

			// 처리 완료 상태로 업데이트
			redisTemplate.opsForValue().set(idempotencyKey, "completed", Duration.ofDays(1));

			// 메시지 저장 완료 (WebSocket 브로드캐스트는 RedisSubscriber에서 처리)
			log.info("[KafkaConsumer] 메시지 저장 완료: roomId={}, messageId={}", dto.getRoomId(), dto.getMessageId());
		} catch (Exception e) {
			// 처리 실패 시 Redis 키 삭제하여 재처리 가능하도록 함
			redisTemplate.delete(idempotencyKey);
			log.error("채팅 로그 저장 및 브로드캐스트 실패 – roomId={}, userId={}", dto.getRoomId(), dto.getUserId(), e);
			throw e;
		}
	}

	/**
	 * 메시지에 재시도 정보를 추가하는 헬퍼 메서드
	 * @param message 원본 메시지
	 * @param retryCount 재시도 횟수
	 * @param errorType 에러 타입
	 * @param errorMessage 에러 메시지
	 * @return 재시도 정보가 추가된 메시지
	 */
	private String addRetryInfoToMessage(String message, int retryCount, String errorType, String errorMessage) {
		try {
			JsonNode node = objectMapper.readTree(message);
			ObjectNode objectNode = (ObjectNode) node;
			objectNode.put("retryCount", retryCount);
			objectNode.put("errorType", errorType);
			objectNode.put("errorMessage", errorMessage);
			objectNode.put("lastRetryAt", LocalDateTime.now().toString());
			return objectNode.toString();
		} catch (Exception e) {
			log.error("메시지에 재시도 정보 추가 실패: {}", message, e);
			// JSON 파싱 실패 시 원본 메시지에 재시도 정보 추가
			return String.format(
				"{\"originalMessage\":\"%s\",\"retryCount\":%d,\"errorType\":\"%s\",\"errorMessage\":\"%s\",\"lastRetryAt\":\"%s\"}",
				message.replace("\"", "\\\""), 
				retryCount, 
				errorType, 
				errorMessage.replace("\"", "\\\""), 
				LocalDateTime.now()
			);
		}
	}

	/**
	 * 메시지에서 재시도 정보를 추출하는 헬퍼 메서드
	 * @param message 메시지
	 * @return 재시도 정보가 포함된 JsonNode
	 */
	public JsonNode extractRetryInfo(String message) {
		try {
			return objectMapper.readTree(message);
		} catch (Exception e) {
			log.error("메시지에서 재시도 정보 추출 실패: {}", message, e);
			return null;
		}
	}

	/**
	 * 최대 재시도 횟수 확인
	 * @param retryCount 현재 재시도 횟수
	 * @return 최대 재시도 횟수 초과 여부
	 */
	public boolean isMaxRetryExceeded(int retryCount) {
		return retryCount >= maxRetryCount;
	}

	/**
	 * 지수 백오프를 통한 재시도 지연 시간 계산
	 * @param retryCount 현재 재시도 횟수
	 * @return 지연 시간 (초)
	 */
	public int calculateRetryDelay(int retryCount) {
		return (int) Math.pow(retryDelayMultiplier, retryCount);
	}
}
