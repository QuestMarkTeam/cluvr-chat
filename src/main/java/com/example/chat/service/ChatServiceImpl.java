package com.example.chat.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.chat.dto.request.ChatMessageRequestDto;
import com.example.chat.dto.request.CreateChatRoomRequestDto;
import com.example.chat.dto.response.ChatRoomListResponseDto;
import com.example.chat.dto.response.ChatRoomResponseDto;
import com.example.chat.dto.response.UserInfoResponseDto;
import com.example.chat.entity.ChatLog;
import com.example.chat.entity.ChatRoom;
import com.example.chat.entity.ChatRoomUser;
import com.example.chat.enums.ClubRole;
import com.example.chat.enums.MessageType;
import com.example.chat.enums.RoomType;
import com.example.chat.pubsub.RedisPublisher;
import com.example.chat.repository.ChatLogRepository;
import com.example.chat.repository.ChatRoomRepository;
import com.example.chat.repository.ChatRoomUserRepository;
import com.example.notification.event.ChatNotificationEvent;
import com.example.notification.event.ChatNotificationProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
	private final ChatRoomRepository chatRoomRepository;
	private final ChatLogRepository chatLogRepository;
	private final ChatRoomUserRepository userRepository;
	private final GetInfoFromExternal getInfoFromExternal;
	// private final DummyInfoExternal dummyInfoExternal; // 무시해라 레빗아... 더미다
	private final ChatNotificationProducer notificationProducer;
	private final RedisPublisher redisPublisher;
	private final ObjectMapper objectMapper;

	/**
	 * Creates and persists a new chat room using the provided request data.
	 *
	 * @param clubId the ID of the club
	 * @param request contains club ID, room name, creator user ID, image URL, and room type for the new chat room
	 * @author Tcimel
	 */
	@Transactional
	@Override
	public void createChatRoom(Long clubId, CreateChatRoomRequestDto request) {
		UserInfoResponseDto userInfo = getInfoFromExternal.getUserInfo(clubId);
		ClubRole userRole = ClubRole.valueOf(userInfo.getRole().toUpperCase());
		List<ChatRoomUser> users = userRepository.findByClubIdAndUserId(clubId, userInfo.getUserId());
		for (ChatRoomUser u : users) {
			if (u.getClubRole() != userRole) {
				u.updateClubRole(userRole);
			}
		}
		userRepository.saveAll(users);

		if (userRole == ClubRole.MEMBER) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
		}

		ChatRoom room = new ChatRoom(
			clubId,
			request.getName(),
			userInfo.getUserId(),
			request.getImageUrl(),
			request.getType());
		chatRoomRepository.save(room);
	}

	/**
	 * 설명: 채팅방 리스트 조회
	 * <p>
	 * 클럽 Id와 유저의 클럽내에서의 Role 기반으로 채팅방 리스트를 가져옴
	 * 외부 API 요청(도메인쪽으로)하여 Role 정보를 불러와서 갱신 해주고 불러옴
	 *
	 * @param clubId the ID of the club
	 * @return 채팅방 리스트 반환
	 * @author Tcimel
	 */
	@Override
	@Transactional
	public ChatRoomListResponseDto findChatRoomByClubAndRole(Long clubId) {
		UserInfoResponseDto userInfo = getInfoFromExternal.getUserInfo(clubId);
		ClubRole userRole = ClubRole.valueOf(userInfo.getRole().toUpperCase());
		List<ChatRoomUser> users = userRepository.findByClubIdAndUserId(clubId, userInfo.getUserId());
		for (ChatRoomUser u : users) {
			if (u.getClubRole() != userRole) {
				u.updateClubRole(userRole);
			}
		}
		userRepository.saveAll(users);

		List<ChatRoom> chatRoomEntities;
		if (userRole == ClubRole.ADMIN || userRole == ClubRole.OWNER) {
			chatRoomEntities = chatRoomRepository.findByClubId(clubId);
		} else {
			chatRoomEntities = chatRoomRepository.findByClubIdAndType(clubId, RoomType.MEMBER);
		}
		
		List<ChatRoomResponseDto> chatRooms = chatRoomEntities.stream()
			.map(ChatRoomResponseDto::from)
			.collect(Collectors.toList());

		return new ChatRoomListResponseDto(userInfo.getClubName(), chatRooms, userRole);
	}

	/**
	 * 채팅방에 속한 모든 구독자에게 채팅 메시지를 브로드캐스트(전송)하고, 메시지를 영속 저장소에 저장합니다.
	 * <p>
	 * 사용자가 해당 채팅방의 멤버일 때만 메시지가 전송됩니다. 전송 전 발신자의 닉네임을 설정합니다.
	 *
	 * @param request 채팅 메시지 요청 객체 (roomId, userId, message, type 등 포함)
	 * @author Tcimel
	 */
	@Override
	@Transactional
	public void broadcastMessage(ChatMessageRequestDto request) {
		// userId 검증 강화
		if (request.getUserId() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용자 ID가 필요합니다.");
		}
		
		if (request.getRoomId() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "채팅방 ID가 필요합니다.");
		}
		
		if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "메시지 내용이 필요합니다.");
		}

		// 사용자가 해당 채팅방의 멤버인지 확인
		if (!userRepository.existsByRoomIdAndUserId(request.getRoomId(), request.getUserId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 채팅방의 멤버가 아닙니다.");
		}

		if (request.getMessageId() == null || request.getMessageId().isEmpty()) {
			request.setMessageId(UUID.randomUUID().toString());
		}
		/*저장 실패 후 이미 브로커로 전송된 메시지를 되돌릴 방법이 없습니다.
		트랜잭션 또는 try-catch 후 롤백 전략, 혹은 먼저 저장 후 전송하는 방식을 고려하세요.
		권한 미충족 시 return; 으로 무음 처리 → 클라이언트는 성공으로 오인할 수 있습니다. 명시적 에러 응답·에러 메시지 전송이 필요합니다.*/
		ChatRoomUser user = userRepository.findByRoomIdAndUserId(request.getRoomId(), request.getUserId())
			.orElseThrow(() -> new ResponseStatusException(
				HttpStatus.BAD_REQUEST, "사용자 정보를 찾을 수 없습니다."));
		request.setNickname(user.getNickname());

		List<ChatRoomUser> roomUsers = userRepository.findByRoomId(request.getRoomId());
		for (ChatRoomUser receiver : roomUsers) {
			if (!receiver.getUserId().equals(request.getUserId())) {
				ChatNotificationEvent event = new ChatNotificationEvent(
					receiver.getUserId(),
					request.getRoomId(),
					request.getNickname() + "님의 새 메시지가 도착하였습니다."
				);
				notificationProducer.send(event);
			}
		}

		try {
			System.out.println("🥕🥕🥕 Redis publish 실행");
			String json = objectMapper.writeValueAsString(request);
			redisPublisher.publish("room:" + request.getRoomId(), json);
		} catch (JsonProcessingException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "메세지 직렬화 실패");
		}
	}

	/**
	 * Retrieves all chat messages for the specified chat room, ordered by creation time in ascending order.
	 *
	 * @param roomId the ID of the chat room
	 * @return a list of chat logs for the room, sorted by creation time
	 */
	@Override
	@Transactional(readOnly = true)
	public List<ChatLog> getMessages(Long clubId, Long roomId) {
		System.out.println("🥕🥕🥕 getMessages 실행~~~~~~~~~~");
		// return chatLogRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
		UserInfoResponseDto userInfo = getInfoFromExternal.getUserInfo(clubId);
		ChatRoomUser user = userRepository.findByRoomIdAndUserId(roomId, userInfo.getUserId())
			.orElseThrow(() -> new ResponseStatusException(
				HttpStatus.BAD_REQUEST, "사용자 정보를 찾을 수 없습니다."));
		LocalDateTime joinTime = user.getJoinedAt();

		return chatLogRepository.findByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(roomId, joinTime);
	}

	/**
	 * Adds a user to all accessible chat rooms in a club based on their role, creating membership records and broadcasting entry messages.
	 * <p>
	 * For each chat room the user is eligible to join and not already a member of, creates a new `ChatRoomUser` entry and sends an "ENTER" message to the room. After joining, refreshes the user's chat room list.
	 *
	 * @param clubId the ID of the club
	 * @param roomId the ID of the chat room
	 * @author Tcimel
	 */
	@Override
	@Transactional
	public void join(Long clubId, Long roomId) {
		UserInfoResponseDto userInfo = getInfoFromExternal.getUserInfo(clubId);
		ClubRole userRole = ClubRole.valueOf(userInfo.getRole().toUpperCase());
		boolean alreadyJoined = userRepository.existsByRoomIdAndUserId(roomId, userInfo.getUserId());
		if (!alreadyJoined) {
			ChatRoomUser join = new ChatRoomUser(
				clubId,
				roomId,
				userInfo.getUserId(),
				userInfo.getNickname(),
				userRole,
				LocalDateTime.now()
			);
			userRepository.save(join);
			ChatMessageRequestDto enterMessage = ChatMessageRequestDto.from(MessageType.ENTER, roomId, userInfo.getUserId(),
				userInfo.getNickname(),
				userInfo.getNickname() + "님이 입장하셨습니다.",
				LocalDateTime.now(),
				UUID.randomUUID().toString());
			broadcastMessage(enterMessage);
		}
	}

	/**
	 * Removes a user from all chat rooms in a club that they have access to based on their role, and broadcasts a leave message to each room.
	 *
	 * @param clubId the ID of the club from which the user is leaving chat rooms
	 * @author Tcimel
	 */
	@Override
	@Transactional
	public void leave(Long clubId) {
		List<ChatRoom> allRooms = chatRoomRepository.findByClubId(clubId);
		UserInfoResponseDto userInfo = getInfoFromExternal.getUserInfo(clubId);
		ClubRole userRole = ClubRole.valueOf(userInfo.getRole().toUpperCase());
		List<ChatRoom> accessibleRooms = allRooms.stream()
			.filter(room -> {
				if (room.getType() == RoomType.MANAGER) {
					return userRole == ClubRole.OWNER || userRole == ClubRole.ADMIN;
				}
				return true;
			}).toList();
		for (ChatRoom room : accessibleRooms) {
			userRepository.deleteByRoomIdAndUserId(room.getId(), userInfo.getUserId());
			ChatMessageRequestDto leaveMessage = ChatMessageRequestDto.from(MessageType.LEAVE, room.getId(), userInfo.getUserId(),
				userInfo.getNickname(),
				userInfo.getNickname() + "님이 퇴장하셨습니다.",
				LocalDateTime.now(),
				UUID.randomUUID().toString());
			broadcastMessage(leaveMessage);
		}
	}

	@Override
	public List<ChatRoomUser> getUserInRoom(Long roomId) {
		return userRepository.findByRoomId(roomId);
	}

}
