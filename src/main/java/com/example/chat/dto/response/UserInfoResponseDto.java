package com.example.chat.dto.response;

import lombok.Getter;

@Getter
public class UserInfoResponseDto {
	private final Long userId;
	private final String nickname;
	private final Long clubId;
	private final String clubName;
	private final String role;
	// private final String imageUrl;

	public UserInfoResponseDto(Long userId, String nickname, Long clubId, String clubName, String role) {
		this.userId = userId;
		this.nickname = nickname;
		this.clubId = clubId;
		this.clubName = clubName;
		this.role = role;
		// this.imageUrl = imageUrl;
	}

	public static UserInfoResponseDto from(Long userId, String nickname, Long clubId, String clubName, String role) {
		return new UserInfoResponseDto(userId, nickname, clubId, clubName, role);
	}
}
