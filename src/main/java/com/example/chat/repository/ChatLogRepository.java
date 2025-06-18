package com.example.chat.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.chat.entity.ChatLog;

public interface ChatLogRepository extends MongoRepository<ChatLog, String> {
	/****
	 * Retrieves all chat logs for the specified room, ordered by creation time in ascending order.
	 *
	 * @param roomId the unique identifier of the chat room
	 * @return a list of chat logs belonging to the room, sorted by creation time from oldest to newest
	 */
	List<ChatLog> findByRoomIdOrderByCreatedAtAsc(Long roomId);
}
