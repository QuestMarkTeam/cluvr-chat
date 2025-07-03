package com.example.notification.event;

import lombok.Getter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
public class ChatNotificationEvent {
	private final Long receiverId;
	private final Long roomId;
	private final String content;

	@JsonCreator
	public ChatNotificationEvent(
		@JsonProperty("receiverId") Long receiverId,
		@JsonProperty("roomId") Long roomId,
		@JsonProperty("content") String content
	) {
		this.receiverId = receiverId;
		this.roomId = roomId;
		this.content = content;
	}
}
