package com.example.chat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.chat.dto.response.UserInfoResponseDto;
import com.example.global.exception.BusinessException;
import com.example.global.response.response.BaseResponse;
import com.example.global.response.response.ResponseCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@Slf4j
@Profile("!test")
@Component
@RequiredArgsConstructor
public class GetInfoFromExternalImpl implements GetInfoFromExternal {
	private final RestTemplate restTemplate;

	@Value("${external.club_api.base_url}")
	private String baseUrl;
	private final ObjectMapper objectMapper;

	@Override
	public UserInfoResponseDto getUserInfo(Long clubId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
			throw new BusinessException(ResponseCode.NO_JWT, "JWT 인증 정보가 없습니다.");
		}
		String sub = jwt.getSubject();
		Long userId = getUserIdFromSub(sub);
		System.out.println("subsubsubsub~~~~~~~~~: " + sub);
		System.out.println("userIduserId~~~~~~~~~: " + userId);

		if (userId == null) {
			throw new BusinessException(ResponseCode.AUTHENTICATION_FAILED, "유저 정보를 찾을 수 없습니다.");
		}
		try {
			String url = baseUrl + "/api/clubs/" + clubId + "/members/" + userId + "/role";
			//JWT 토큰을 헤더에 포함
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(jwt.getTokenValue());
			HttpEntity<String> entity = new HttpEntity<>(headers);

			ResponseEntity<String> response = restTemplate.exchange(
				url,
				HttpMethod.GET,
				entity,
				String.class
			);

			BaseResponse<UserInfoResponseDto> parsed = objectMapper.readValue(
				response.getBody(),
				new TypeReference<BaseResponse<UserInfoResponseDto>>() {}
			);

			log.info("🔁 응답 상태: {}", response.getStatusCode());
			log.info("🔁 응답 바디: {}", response.getBody());

			if (response.getBody() == null) {
				throw new IllegalStateException("사용자 정보를 찾을 수 없습니다.");
			}

			return parsed.getData();
			
		} catch (HttpClientErrorException e) {
			log.error("외부 API 호출 실패 - HTTP 에러: {}", e.getStatusCode());
			throw new RuntimeException("외부 API 호출 실패: userId=" + userId, e);
		} catch (NumberFormatException e) {
			throw new BusinessException(ResponseCode.AUTHENTICATION_FAILED, "유효하지 않은 형식입니다.");
		} catch (Exception e) {
			log.error("외부 API 호출 중 예상치 못한 에러 발생", e);
			throw new RuntimeException("외부 API 호출 실패: userId=" + userId, e);
		}
	}

	public Long getUserIdFromSub(String sub) {
		if (sub == null || sub.trim().isEmpty()) {
			throw new BusinessException(ResponseCode.FAIL_SUB_TO_USERID, "sub 값이 유효하지 않습니다.");
		}

		String url = baseUrl + "/api/users/sub/" + sub + "/user-id";

		try {
			ResponseEntity<String> response = restTemplate.exchange(
				url,
				HttpMethod.GET,
				null,
				String.class
			);

			log.debug("sub -> userId 응답 원문: {}", response.getBody());

			BaseResponse<Long> parsed = objectMapper.readValue(
				response.getBody(),
				new TypeReference<BaseResponse<Long>>() {
				}
			);

			if (parsed.getData() == null) {
				throw new BusinessException(ResponseCode.FAIL_TO_USERID);
			}

			return parsed.getData();
		} catch (BusinessException e) {
			throw e;
		} catch (HttpClientErrorException e) {
			log.error("외부 API 호출 실패 - HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
			throw new BusinessException(ResponseCode.FAIL_SUB_TO_USERID, "외부 서비스 호출 실패");
		} catch (Exception e) {
			log.error("sub -> userId 변환 중 예외 발생", e);
			throw new BusinessException(ResponseCode.FAIL_SUB_TO_USERID, "예상치 못한 오류 발생");
		}
	}
}
