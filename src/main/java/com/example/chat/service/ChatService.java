package com.example.chat.service;

import java.util.List;

import com.example.chat.dto.request.ChatMessageRequestDto;
import com.example.chat.dto.request.ChatRoomRequestDto;
import com.example.chat.dto.request.CreateChatRoomRequestDto;
import com.example.chat.dto.request.JoinRequestDto;
import com.example.chat.dto.response.ChatRoomResponseDto;
import com.example.chat.entity.ChatLog;
import com.example.chat.entity.ChatRoomUser;

public interface ChatService {
	/****
	 * Creates a new chat room using the provided request data.
	 *
	 * @param request the details required to create the chat room
	 */
	void createChatRoom(CreateChatRoomRequestDto request);

	/****
	 * Retrieves chat rooms for a given club filtered by user role criteria.
	 *
	 * @param request the criteria specifying user role and other filters
	 * @return a list of chat rooms matching the specified club and role criteria
	 */
	List<ChatRoomResponseDto> findChatRoomByClubAndRole(ChatRoomRequestDto request);

	/**
	 * Sends a chat message to all participants in the specified chat room.
	 *
	 * @param request the message details and target chat room information
	 */
	void broadcastMessage(ChatMessageRequestDto request);

	/****
	 * Retrieves the list of chat messages for the specified chat room.
	 *
	 * @param roomId the unique identifier of the chat room
	 * @return a list of chat logs associated with the given room
	 */
	List<ChatLog> getMessages(Long roomId);

	/**
	 * Adds a user to the chat room associated with the specified club.
	 *
	 * @param request the user information required to join the chat room
	 */
	void join(JoinRequestDto request);

	/****
	 * Removes a user from the chat room associated with the specified club.
	 *
	 * @param clubId the identifier of the club whose chat room the user will leave
	 * @param userId the identifier of the user to be removed from the chat room
	 */
	void leave(Long clubId, Long userId);

	/****
	 * Retrieves the list of users currently present in the specified chat room.
	 *
	 * @param roomId the unique identifier of the chat room
	 * @return a list of users in the chat room
	 */
	List<ChatRoomUser> getUserInRoom(Long roomId);
}
