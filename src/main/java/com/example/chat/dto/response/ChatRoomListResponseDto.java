package com.example.chat.dto.response;

import java.util.List;

import com.example.chat.enums.ClubRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomListResponseDto {
	private String clubName;
	private List<ChatRoomResponseDto> chatRooms;
	@JsonProperty("role")
	private ClubRole role;

	public ChatRoomListResponseDto(String clubName, List<ChatRoomResponseDto> chatRooms, ClubRole role) {
		this.clubName = clubName;
		this.chatRooms = chatRooms;
		this.role = role;
	}
}
