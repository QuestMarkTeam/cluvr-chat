package com.example.chat.repository;

import java.util.List;

import com.example.chat.entity.ChatRoom;
import com.example.chat.enums.RoomType;
import com.example.common.repository.BaseRepository;

public interface ChatRoomRepository extends BaseRepository<ChatRoom, Long> {
	/****
	 * Retrieves all chat rooms associated with the specified club ID.
	 *
	 * @param clubId the unique identifier of the club
	 * @return a list of chat rooms belonging to the given club
	 */
	List<ChatRoom> findByClubId(Long clubId);

	/****
	 * Retrieves a list of chat rooms for the specified club and room type.
	 *
	 * @param clubId the unique identifier of the club
	 * @param type the type of chat room to filter by
	 * @return a list of chat rooms matching the given club ID and room type
	 */
	List<ChatRoom> findByClubIdAndType(Long clubId, RoomType type);
}
