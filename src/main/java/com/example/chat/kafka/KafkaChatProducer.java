package com.example.chat.kafka;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaChatProducer {
	// KafkaTemplate<K, V>는 Spring Kafka가 제공하는 클래스,
	// Kafka 브로커로 메시지를 보내기 위한 핵심 클래스.
	private final KafkaTemplate<String, String> kafkaTemplate;

	public void sendMessage(String topic, String message, String roomId) {
		// "chat-log"라는 Kafka 토픽(topic)에
		// → message라는 문자열 메시지를 전송(Publish)
		// System.out.println("🥕🥕🥕 Kafka 서버에서 메세지 받음");
		try {
			log.info("[KafkaProducer] 메시지 전송 시도: topic={}, key={}, message={}", topic, roomId, message);
			CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, roomId, message);
			SendResult<String, String> result = future.get(10, TimeUnit.SECONDS);
			log.info("[KafkaProducer] 메시지 전송 성공: offset={}, partition={}", result.getRecordMetadata().offset(), result.getRecordMetadata().partition());
		} catch (TimeoutException e) {
			log.error("[KafkaProducer] Publish failed: TimeoutException: {}", e.getMessage(), e);
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			log.error("[KafkaProducer] Publish failed: InterruptedException: {}", e.getMessage(), e);
			Thread.currentThread().interrupt(); // 인터럽트 플래그 복구
			throw new RuntimeException(e);
		} catch (Exception e) {
			log.error("[KafkaProducer] Publish failed: {}", e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}
}
