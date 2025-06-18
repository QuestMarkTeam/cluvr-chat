package com.example.chat.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.chat.dto.request.ChatMessageRequestDto;
import com.example.chat.dto.request.ChatRoomRequestDto;
import com.example.chat.dto.request.CreateChatRoomRequestDto;
import com.example.chat.dto.request.JoinRequestDto;
import com.example.chat.dto.response.ChatRoomResponseDto;
import com.example.chat.entity.ChatLog;
import com.example.chat.entity.ChatRoomUser;
import com.example.chat.service.ChatService;
import com.example.global.response.response.BaseResponse;
import com.example.global.response.response.ResponseCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;

	/**
	 * 설명: 채팅방 만들기
	 * <p>
	 * 생성할 때, 클럽 Id, 만든 user Id, 생성을 요청한 user의 클럽 role 필요
	 * 유저는 클럽장, 매니저, MEMBER 역할이 있고 클럽장과 매니저 등급만 채팅방 생성 가능
	 *
	 * @param request {설명: 채팅방 생성에 필요한 dto}
	 * @return {생성 완료 시 ResponseCode.CREATED 반환}
	 * @throws {생성 권한이 없을 경우 권한이 없다는 안내 메세지 반환}
	 * @author Tcimel
	 */
	@PostMapping("/chat/create")
	public ResponseEntity<BaseResponse<Void>> createChatRoom(
		@RequestBody CreateChatRoomRequestDto request) {
		log.info("방 생성 요청: {}", request);
		chatService.createChatRoom(request);
		return ResponseEntity.ok(BaseResponse.success(ResponseCode.CREATED));
	}

	/**
	 * 설명: {채팅방 조회}
	 * 채팅방 조회 시 클럽원 역할에 따라 불러오는 리스트가 다름.
	 * 채팅방 타입은 MANAGER, MEMBER 타입이 있는데
	 * 클럽장과 매니저 역할은 모든 채팅방 리스트가 보이고
	 * 유저에게는 MANAGER 타입의 채팅방이 보이면 안됨.
	 * <p>
	 * [고도화 때 접근하는 IP 설정 필요]
	 * 방법 1 : IP 화이트리스트 설정 (필터 설정하기)
	 * 방법 2 : X-INTERNAL_KEY 설정 추가 해당 키값이 없거나 같지 않을 경우 돌려보내기
	 * 방법 3 : Spring Security 사용
	 * <p>
	 * 조회인데 Post를 한 이유
	 * 외부에서 직접 URL 조작을 막고 싶은 경우, Body 안에 정보를 넣고 POST로 전달하는 방식이 일반적이라고 함.
	 *
	 * @param request {설명: 유저 Role에 해당하는 채팅방 리스트를 불러와야 하기 때문에, requestbody로 userId와 user의 role정보 전달 필요}
	 * @return {clubId와 user의 role에 따른 채팅방 리스트 반환}
	 * @author Tcimel
	 */
	@GetMapping("/chat/list")
	public ResponseEntity<BaseResponse<List<ChatRoomResponseDto>>> getChatRooms(
		@RequestBody ChatRoomRequestDto request
	) {
		log.info("🥕🥕🥕 request = {}", request);
		List<ChatRoomResponseDto> chatRooms = chatService.findChatRoomByClubAndRole(request);
		return ResponseEntity.ok(BaseResponse.success(chatRooms, ResponseCode.OK));
	}

	@PostMapping("/chat/join")
	public ResponseEntity<BaseResponse<Void>> joinChatRoom(
		@RequestBody JoinRequestDto request
	) {
		chatService.join(request);
		return ResponseEntity.ok(BaseResponse.success(ResponseCode.OK));
	}

	/**
	 * 설명: {채팅 메세지 불러오는 메서드}
	 * <p>
	 * [고도화 할 때 입장한 후 부터의 메세지 캐싱 필요]
	 *
	 * @param roomId
	 * @return roomId에 해당하는 채팅 메세지
	 * @author Tcimel
	 */

	@GetMapping("/chat/{roomId}")
	public ResponseEntity<BaseResponse<List<ChatLog>>> getMessages(@PathVariable Long roomId) {
		List<ChatLog> messages = chatService.getMessages(roomId);
		return ResponseEntity.ok(BaseResponse.success(messages, ResponseCode.OK));
	}

	// 추후 개선 진행 : 컨트롤러 계층에서 DTO 로 변환해 최소·안전 정보만 반환.
	@GetMapping("/room/{roomId}/users")
	public ResponseEntity<BaseResponse<List<ChatRoomUser>>> getUserInRoom(@PathVariable Long roomId) {
		List<ChatRoomUser> users = chatService.getUserInRoom(roomId);
		return ResponseEntity.ok(BaseResponse.success(users, ResponseCode.OK));
	}

	@MessageMapping("/message") // /pub/chat/message
	public void sendMessage(@Payload ChatMessageRequestDto request) {
		chatService.broadcastMessage(request); // 채팅 로그 몽고디비에 저장
	}
}
