package com.example.chat.dto.request;

import com.example.chat.enums.MessageType;

import lombok.Getter;

@Getter
public class ChatMessageRequestDto {
	private MessageType type;
	private Long roomId;
	private Long userId;
	private String nickname;
	private String message;

	public ChatMessageRequestDto(MessageType type, Long roomId, Long userId, String nickname, String message) {
		this.type = type;
		this.roomId = roomId;
		this.userId = userId;
		this.nickname = nickname;
		this.message = message;
	}

	public static ChatMessageRequestDto from(MessageType type, Long roomId, Long userId, String nickname,
		String message) {
		return new ChatMessageRequestDto(type, roomId, userId, nickname, message);
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
}
