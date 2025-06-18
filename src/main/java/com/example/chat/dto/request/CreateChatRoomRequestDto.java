package com.example.chat.dto.request;

import com.example.chat.enums.ClubRole;
import com.example.chat.enums.RoomType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter // Jackson 역직렬화를 위한 생성자나 setter 메서드가 필요
@NoArgsConstructor
public class CreateChatRoomRequestDto {
	@NotNull
	private Long clubId; // 클럽 ID (도메인 서버가 전달)
	@NotNull
	private RoomType type; // 채팅방 타입(MANAGER, MEMBER)
	@NotNull
	private Long userId; // 생성자 ID (도메인 서버가 전달)
	@NotNull
	private ClubRole role; // 해당 유저의 클럽에서의 role 정보 (LEADER, MANAGER, MEMBER)
	@NotBlank
	private String name; // 채팅방 명
	private String imageUrl; // 선택적 필드
}
