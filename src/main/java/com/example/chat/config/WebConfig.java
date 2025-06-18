package com.example.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 설명:
 * http://localhost:63342에서 실행되는 프론트엔드와 CORS 문제 없이 통신할 수 있도록 설정된 Spring의 CORS 설정 클래스
 * IntelliJ에서 HTML 파일을 실행하면 임의의 포트를 지정해 임시 웹 서버를 띄우는데, 그게 63342
 * <p>
 * CORS(Cross-Origin Resource Sharing)는 서로 다른 출처(origin) 간에 리소스를 주고받을 수 있도록 브라우저가 허용하는 정책
 * 프론트엔드가 http://localhost:64452에서 실행되고, 백엔드(Spring)가 http://localhost:8080이라면, 이 둘은 “다른 origin”
 * <p>
 * Origin = 프로토콜 + 호스트 + 포트
 * 예시 1:
 * 주소: http://localhost:8080
 * 프로토콜: http
 * 호스트: localhost
 * 포트: 8080
 */

@Configuration
public class WebConfig implements WebMvcConfigurer {
	@Value("${app.cors.allowed-origins:http://localhost:63342}") // 하드코딩된 localhost URL 대신 환경변수나 프로파일별 설정을 고려
	private String allowedOrigins;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
			.allowedOrigins(allowedOrigins.split(","))
			.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용할 HTTP 메서드
			.allowedHeaders("Content-Type", "Authorization", "X-Requested-With")
			.allowCredentials(true);
	}
}
