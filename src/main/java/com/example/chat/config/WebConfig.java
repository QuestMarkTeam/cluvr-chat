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
	// 내 서버(백엔드)에 접근을 허용할 프론트엔드 주소
	// 하드코딩된 localhost URL 대신 환경변수나 프로파일별 설정을 고려
	@Value("${app.cors.allowed-origins}")
	private String allowedOrigins;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**") // 내 서버의 모든 API 경로에 대해 CORS 정책을 적용
			.allowedOrigins(allowedOrigins.split(",")) // 허용할 출처(origin) 목록. 쉼표로 구분된 문자열을 배열로 변환
			.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용할 HTTP 메서드
			.allowedHeaders("Content-Type", "Authorization", "X-Requested-With") // 허용할 헤더 목록. 헤더는 클라이언트가 서버에 요청할 때 전달하는 데이터의 형식과 내용을 설명하는 키-값 쌍
			.allowCredentials(true); // 쿠키 허용 여부
	}
}
