package com.example.chat.service;

import com.example.chat.dto.response.UserInfoResponseDto;

public interface GetInfoFromExternal {
	/****
	 * Retrieves user information for the specified user ID from an external source.
	 *
	 * @param userId the unique identifier of the user
	 * @return a UserInfoResponseDto containing the user's information
	 */
	UserInfoResponseDto getUserInfo(Long userId);
}
