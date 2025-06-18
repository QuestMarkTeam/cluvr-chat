package com.example.chat.pubsub;

import java.nio.charset.StandardCharsets;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import com.example.chat.dto.request.ChatMessageRequestDto;
import com.example.chat.kafka.KafkaChatProducer;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {
	private final KafkaChatProducer kafkaChatProducer;
	private final ObjectMapper objectMapper;

	// Redis에서 수신한 메세지를 처리
	// 메세지를 받아서 Kafka로 넘기는 역할
	@Override
	public void onMessage(Message message, byte[] pattern) {
		String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
		String msg = new String(message.getBody(), StandardCharsets.UTF_8);
		log.info("📩 채널={}, 메시지={}", channel, msg);
		// System.out.println("🥕🥕🥕 Kafka Producer 실행");
		// kafkaChatProducer.sendMessage("chat-log", msg); // send to kafka
		try {
			ChatMessageRequestDto dto = objectMapper.readValue(msg, ChatMessageRequestDto.class);
			kafkaChatProducer.sendMessage("chat-message", msg, String.valueOf(dto.getRoomId()));
		} catch (Exception e) {
			log.error("Kafka 전송 실패 – channel={}, msg={}", channel, msg, e);
			// TODO: 재시도 또는 장애 전파 정책 적용 필요
		}
	}
}
