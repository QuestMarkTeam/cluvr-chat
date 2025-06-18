package com.example.chat.repository;

import java.util.List;
import java.util.Optional;

import com.example.chat.entity.ChatRoomUser;
import com.example.common.repository.BaseRepository;

public interface ChatRoomUserRepository extends BaseRepository<ChatRoomUser, Long> {
	/**
	 * Checks if a chat room user exists for the specified room ID and user ID.
	 *
	 * @param roomId the ID of the chat room
	 * @param userId the ID of the user
	 * @return true if a ChatRoomUser entity exists for the given room and user IDs, false otherwise
	 */
	boolean existsByRoomIdAndUserId(Long roomId, Long userId);

	/**
	 * Retrieves all ChatRoomUser entities associated with the specified chat room ID.
	 *
	 * @param roomId the unique identifier of the chat room
	 * @return a list of ChatRoomUser entities belonging to the given room, or an empty list if none are found
	 */
	List<ChatRoomUser> findByRoomId(Long roomId);

	/****
	 * Retrieves the ChatRoomUser entity matching the specified room ID and user ID.
	 *
	 * @param roomId the ID of the chat room
	 * @param userId the ID of the user
	 * @return the ChatRoomUser entity for the given room and user, or null if not found
	 */
	Optional<ChatRoomUser> findByRoomIdAndUserId(Long roomId, Long userId);

	/****
	 * Deletes the association between a user and a chat room based on the specified room ID and user ID.
	 *
	 * @param roomId the ID of the chat room
	 * @param userId the ID of the user to remove from the chat room
	 */
	void deleteByRoomIdAndUserId(Long roomId, Long userId);

	/**
	 * Retrieves all chat room user entities associated with the specified user ID.
	 *
	 * @param userId the ID of the user whose chat room associations are to be retrieved
	 * @return a list of ChatRoomUser entities linked to the given user ID
	 */
	List<ChatRoomUser> findByUserId(Long userId);
}
