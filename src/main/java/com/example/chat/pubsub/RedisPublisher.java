package com.example.chat.pubsub;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisPublisher {
	private final StringRedisTemplate redisTemplate;

	// Redis 채널(topic)에 메세지를 보내는 역할
	public void publish(String topic, String message) {
		// Redis 서버의 topic 채널로 메세지를 보냄
		// Redis Pub/Sub 구조에 의해 Redis 내부로 전파됨
		// RedisConfig 설정에서 topic 채널을 RedisSubscriber가 구독하도록 미리 설정해 둠
		// 그래서 메세지가 도달하면 스프링이 자동으로 redisSubscriber.onMessage()호출
		System.out.println("🥕🥕🥕 Redis publish 실행 2");
		redisTemplate.convertAndSend(topic, message);
	}

}
