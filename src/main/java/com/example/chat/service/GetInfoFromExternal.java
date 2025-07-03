package com.example.chat.service;

import com.example.chat.dto.response.UserInfoResponseDto;

public interface GetInfoFromExternal {
	/****
	 * Retrieves user information from JWT token for the specified club.
	 *
	 * @param clubId the ID of the club
	 * @return a UserInfoResponseDto containing the user's information
	 */
	UserInfoResponseDto getUserInfo(Long clubId);

	Long getUserIdFromSub(String sub);
}
