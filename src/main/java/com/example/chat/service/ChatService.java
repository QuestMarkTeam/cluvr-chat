package com.example.chat.service;

import java.util.List;

import com.example.chat.dto.request.ChatMessageRequestDto;
import com.example.chat.dto.request.CreateChatRoomRequestDto;
import com.example.chat.dto.response.ChatRoomListResponseDto;
import com.example.chat.entity.ChatLog;
import com.example.chat.entity.ChatRoomUser;

public interface ChatService {
	/****
	 * Creates a new chat room using the provided request data.
	 *
	 * @param clubId the identifier of the club
	 * @param request the details required to create the chat room
	 * @author Tcimel
	 */
	void createChatRoom(Long clubId, CreateChatRoomRequestDto request);

	/****
	 * Retrieves chat rooms for a given club filtered by user role criteria.
	 *
	 * @param clubId the identifier of the club
	 * @return a list of chat rooms matching the specified club and role criteria
	 * @author Tcimel
	 */
	ChatRoomListResponseDto findChatRoomByClubAndRole(Long clubId);

	/**
	 * Sends a chat message to all participants in the specified chat room.
	 *
	 * @param request the message details and target chat room information
	 * @author Tcimel
	 */
	void broadcastMessage(ChatMessageRequestDto request);

	/****
	 * Retrieves the list of chat messages for the specified chat room.
	 *
	 * @param roomId the unique identifier of the chat room
	 * @return a list of chat logs associated with the given room
	 * @author Tcimel
	 */
	List<ChatLog> getMessages(Long clubId, Long roomId);

	/**
	 * Adds a user to the chat room associated with the specified club.
	 *
	 * @param clubId the identifier of the club
	 * @param roomId the identifier of the chat room
	 * @author Tcimel
	 */
	void join(Long clubId, Long roomId);

	/****
	 * Removes a user from the chat room associated with the specified club.
	 *
	 * @param clubId the identifier of the club whose chat room the user will leave
	 * @author Tcimel
	 */
	void leave(Long clubId);

	/****
	 * Retrieves the list of users currently present in the specified chat room.
	 *
	 * @param roomId the unique identifier of the chat room
	 * @return a list of users in the chat room
	 * @author Tcimel
	 */
	List<ChatRoomUser> getUserInRoom(Long roomId);
}
