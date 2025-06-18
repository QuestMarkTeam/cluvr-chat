package com.example.chat.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.example.chat.dto.response.UserInfoResponseDto;

@Component
@ConditionalOnProperty(name = "app.user-dummy-external", havingValue = "true")
public class DummyInfoExternal implements GetInfoFromExternal {
	/**
	 * Returns dummy user information for the specified user ID.
	 *
	 * @param userId the ID of the user to retrieve information for
	 * @return a UserInfoResponseDto containing the user ID, a generated username, a fixed role, and a fixed profile image URL
	 */
	@Override
	public UserInfoResponseDto getUserInfo(Long userId) {
		String role = userId % 3 == 0 ? "LEADER" : (userId % 2 == 0 ? "MANAGER" : "MEMBER");
		return new UserInfoResponseDto(userId, "테스트유저" + userId, role,
			"http://example.com/profile" + userId + ".png");
	}
}
