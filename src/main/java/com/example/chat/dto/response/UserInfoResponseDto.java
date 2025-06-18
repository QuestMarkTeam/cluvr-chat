package com.example.chat.dto.response;

import lombok.Getter;

@Getter
public class UserInfoResponseDto {
	private final Long userId;
	private final String nickname;
	private final String role;
	private final String imageUrl;

	public UserInfoResponseDto(Long userId, String nickname, String role, String imageUrl) {
		this.userId = userId;
		this.nickname = nickname;
		this.role = role;
		this.imageUrl = imageUrl;
	}

	public static UserInfoResponseDto from(Long userId, String nickname, String role, String imageUrl) {
		return new UserInfoResponseDto(userId, nickname, role, imageUrl);
	}
}
