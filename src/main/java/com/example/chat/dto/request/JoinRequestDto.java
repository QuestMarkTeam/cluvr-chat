package com.example.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class JoinRequestDto {
	@NotNull(message = "CLUB ID는 필수입니다")
	private Long clubId;

	@NotNull(message = "사용자 ID는 필수입니다")
	private Long userId;

	public JoinRequestDto(Long userId, Long clubId) {
		this.userId = userId;
		this.clubId = clubId;
	}

	public static JoinRequestDto from(Long userId, Long clubId) {
		return new JoinRequestDto(userId, clubId);
	}
}
