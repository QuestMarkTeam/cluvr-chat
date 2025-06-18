package com.example.chat.dto.response;

import com.example.chat.entity.ChatRoom;
import com.example.chat.enums.RoomType;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomResponseDto {
	private Long id;
	private Long clubId;
	private String name;
	private RoomType type;

	public ChatRoomResponseDto(Long id, Long clubId, String name, RoomType type) {
		this.id = id;
		this.clubId = clubId;
		this.name = name;
		this.type = type;
	}

	public static ChatRoomResponseDto from(ChatRoom room) {
		return new ChatRoomResponseDto(
			room.getId(),
			room.getClubId(),
			room.getName(),
			room.getType()
		);
	}
}
