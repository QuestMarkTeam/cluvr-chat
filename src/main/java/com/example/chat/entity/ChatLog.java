package com.example.chat.entity;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.example.chat.enums.MessageType;

import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/* roomId: 1: roomId 필드에 대해 오름차순 정렬 기준으로 인덱스를 생성
createdAt: 1: 그 다음 createdAt 필드도 오름차순 정렬 기준으로 인덱스를 생성
두 필드를 묶어서 하나의 인덱스로 구성 → 복합 인덱스.
채팅 로그 조회 성능 향상을 위해 roomId와 createdAt 필드에 인덱스를 추가 */
@Getter
@Document("chat_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@CompoundIndex(def = "{'roomId': 1, 'createdAt': 1}")
public class ChatLog {
	@Id
	private String id; // MongoDB에서 id는 String 값

	@Indexed
	private Long roomId;

	private Long userId;
	private String nickname;

	private String message;

	private MessageType type; // ENTER, TALK, LEAVE

	private LocalDateTime createdAt;

	public ChatLog(Long roomId, Long userId, String nickname, String message, MessageType type,
		LocalDateTime createdAt) {
		this.roomId = roomId;
		this.userId = userId;
		this.nickname = nickname;
		this.message = message;
		this.type = type;
		this.createdAt = createdAt;
	}
}
