package com.example.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// WebSocketMessageBrokerConfigurer :
// WebSocket의 엔드포인트, 메시지 브로커, 라우팅 방식 설정 제공 클래스
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// 클라이언트가 연결할 엔드포인트 (SockJS 지원)
		registry.addEndpoint("/cluvr-chat")
			.setAllowedOriginPatterns(getAllowedOrigins())
			// 나중에 특정 url 요청만 접근하도록 할 때 이부분 변경 : setAllowedOriginPatterns("https://www.cluvr.com")
			// 또는 .setAllowedOriginPatterns("https://cluvr.com", "http://localhost:3000")
			.withSockJS();
	}

	private String[] getAllowedOrigins() {
		String profile = System.getProperty("spring.profiles.active", "dev");
		if ("prod".equals(profile)) {
			return new String[] {"http://www.cluvr.com", "https://cluvr.com"};
		}
		return new String[] {"http://localhost:*", "http://127.0.0.1:*"};
	}

	// 메시지가 어디로 전송되고(subscribe), 어디에서 수신되고(send) 할지를 설정하는 메서드
	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		// pub은 클라이언트 -> 서버 전송 prefix
		registry.setApplicationDestinationPrefixes("/chat");

		// sub은 서버 -> 클라이언트 브로드캐스트 prefix
		registry.enableSimpleBroker("/sub");
	}
	// Redis Pub/Sub, RabbitMQ, Kafka 같은 외부 브로커 설정 가능
	// RabbitMQ 브로커 사용 예제 : RabbitMQ (STOMP 지원 브로커)
	/*@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.setApplicationDestinationPrefixes("/chat");

		registry.enableStompBrokerRelay("/sub")
			.setRelayHost("localhost")
			.setRelayPort(61613)
			.setClientLogin("guest")
			.setClientPasscode("guest");
	}*/

	// STOMP를 지원하는 외부 브로커(RabbitMQ, ActiveMQ 등)와 직접 연결하는 방식
	// enableStompBrokerRelay() 는 “브로커를 내장하지 말고 외부한테 맡겨”라는 뜻

	// Redis Pub/Sub or Kafka 사용 예제
	// 컨트롤러에서 설정 필요
	// WebSocket 브로커가 직접 지원하지 않기 때문에 직접 메시지를 중간 처리해야 함.
	// 즉, configureMessageBroker()보다는 메시지 처리 로직 내부에서 Redis/Kafka를 연동 필요
	/*@MessageMapping("/room/{roomId}")
	public void handleMessage(@Payload ChatMessage message) {
		// 1. 메시지 Redis로 publish
		redisPublisher.publish("room:" + message.getRoomId(), message);

		// 2. 또는 Kafka로 전송
		kafkaTemplate.send("chat-room-" + message.getRoomId(), message);
	}*/

	// 그리고 Redis/Kafka 리스너는 그 메시지를 받아서 다시 WebSocket으로 전달:
	// messagingTemplate.convertAndSend("/sub/room/" + roomId, message);
}
