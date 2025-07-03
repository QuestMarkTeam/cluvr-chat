package com.example.chat.dto.request;

import java.time.LocalDateTime;

import com.example.chat.enums.MessageType;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatMessageRequestDto {
	private MessageType type;
	private Long roomId;
	private Long userId;
	private String nickname;
	private String message;
	private LocalDateTime createdAt;
	private String messageId;


	public ChatMessageRequestDto(MessageType type, Long roomId, Long userId, String nickname, String message, LocalDateTime createdAt, String messageId) {
		this.type = type;
		this.roomId = roomId;
		this.userId = userId;
		this.nickname = nickname;
		this.message = message;
		this.createdAt = createdAt;
		this.messageId = messageId;
	}

	public static ChatMessageRequestDto from(MessageType type, Long roomId, Long userId, String nickname,
		String message, LocalDateTime createdAt, String messageId) {
		return new ChatMessageRequestDto(type, roomId, userId, nickname, message, createdAt, messageId);
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public void setRoomId(Long roomId) {
		this.roomId = roomId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}
}
