package com.example.chat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.example.chat.dto.response.UserInfoResponseDto;
import com.example.global.response.response.BaseResponse;

import lombok.RequiredArgsConstructor;

@Profile("!test")
@Component
@RequiredArgsConstructor
public class GetInfoFromExternalImpl implements GetInfoFromExternal {
	private final RestTemplate restTemplate;

	@Value("${external.club_api.base_url}")
	private String baseUrl;

	/**
	 * 외부 API로부터 사용자 정보를 조회합니다.
	 *
	 * @param userId 조회할 사용자 ID
	 * @return 사용자 정보 DTO
	 */
	@Override
	public UserInfoResponseDto getUserInfo(Long userId) {
		///clubs/{clubId}/members/{userId}/role
		String url = baseUrl + "/api/users/" + userId;
		try {
			ResponseEntity<BaseResponse<UserInfoResponseDto>> response = restTemplate.exchange(
				url,
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<>() {
				}
			);

			if (response.getBody() == null || response.getBody().getData() == null) {
				throw new IllegalStateException("사용자 정보를 찾을 수 없습니다.");
			}

			return response.getBody().getData();
		} catch (Exception e) {
			throw new RuntimeException("외부 API 호출 실패: " + userId, e);
		}
	}
}
