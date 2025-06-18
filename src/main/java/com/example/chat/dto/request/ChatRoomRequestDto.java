package com.example.chat.dto.request;

import com.example.chat.enums.ClubRole;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomRequestDto {
	@NotNull(message = "CLUB ID는 필수입니다")
	private Long clubId;
	@NotNull(message = "사용자 ID는 필수입니다")
	private Long userId;
	@NotNull(message = "역할 정보는 필수입니다")
	private ClubRole role;

	public ChatRoomRequestDto(Long userId, Long clubId, ClubRole role) {
		this.clubId = clubId;
		this.userId = userId;
		this.role = role;
	}

	public static ChatRoomRequestDto from(Long userId, Long clubId, ClubRole role) {
		return new ChatRoomRequestDto(userId, clubId, role);
	}
}
