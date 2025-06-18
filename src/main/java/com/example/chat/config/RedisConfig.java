package com.example.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.example.chat.pubsub.RedisSubscriber;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {
	// Publisher <-> Subscriber 연결 역할
	// RedisMessageListenerContainer
	// -- Redis Pub/Sub 메세지를 비동기적으로 수신하고, 등록된 MessageListener에게 메세지를 전달하는 리스너 컨테이너
	// RedisConnectionFactory
	// -- Redis 서버와의 실제 연결을 생성해주는 커넥션 팩토리 (연결 관리자)
	// setConnectionFactory(RedisConnectionFactory factory)
	// -- 위에서 만든 Redis 연결(팩토리)을 리스너 컨테이너에 주입해주는 메서드
	// -- “RedisMessageListenerContainer야, 이 연결로 Redis랑 대화해!”
	// addMessageListener(MessageListener listener, ChannelTopic topic)
	// -- 특정 Redis 채널(topic)을 구독하고, 메시지를 받으면 listener의 onMessage()를 자동 호출해줌.
	@Bean
	public RedisMessageListenerContainer container(
		RedisConnectionFactory factory, RedisSubscriber subscriber) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(factory);

		// 단일 채널
		// container.addMessageListener(subscriber, new ChannelTopic("room:123"));

		// 패턴 구독
		container.addMessageListener(subscriber, new PatternTopic("room:*"));
		return container;
	}
}
