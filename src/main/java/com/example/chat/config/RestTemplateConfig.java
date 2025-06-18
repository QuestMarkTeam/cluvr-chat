package com.example.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 설명:
 * RestTemplate 이란
 * 1. 다른 서버에 api 요청할 때(권한, 데이터 확인, 사용자 정보 불러오기 등) 사용
 * 2. 채팅 서버 DB에 없는 정보가 필요할 때(ex : userName, profileUrl등)
 *
 * @author Tcimel
 */

@Configuration
public class RestTemplateConfig {
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
