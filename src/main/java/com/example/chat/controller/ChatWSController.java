package com.example.chat.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;

import com.example.chat.dto.request.ChatMessageRequestDto;
import com.example.chat.service.ChatService;
import com.example.chat.service.GetInfoFromExternal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.security.Principal;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatWSController {

	private final ChatService chatService;
	private final GetInfoFromExternal getInfoFromExternal;

	// 웹소캣과 일반 API 라우팅 방식이 다르기 때문에 나눠야 한다.
	/**
	 * 설명: {채팅 메세지 전송}
	 * <p>
	 * 채팅 메세지를 전송하는 엔드포인트
	 *
	 * @param request the message details and target chat room information
	 * @author Tcimel
	 */
	@MessageMapping("/message")
	public void sendMessage(@Payload ChatMessageRequestDto request, Principal principal) {
		if (principal instanceof JwtAuthenticationToken jwtAuth) {
			Jwt jwt = (Jwt) jwtAuth.getPrincipal();
			String sub = jwt.getSubject();
			if(sub==null){
				log.error("Jwt sub가 없습니다.");
				return;
			}
			// userId 검증
			Long userId = getInfoFromExternal.getUserIdFromSub(sub);
			if (userId == null) {
				log.error("❌ User ID is null in WebSocket message");
				return;
			}
			request.setUserId(userId);
		} else {
			log.error("WebSocket 메시지 처리 중 JWT 인증 정보가 없습니다.");
			return;
		}

		if (request.getRoomId() == null) {
			log.error("❌ Room ID is null in WebSocket message");
			return;
		}
		
		if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
			log.error("❌ Message is null or empty in WebSocket message");
			return;
		}
		
		try {
			chatService.broadcastMessage(request);
			log.info("✅ Message broadcasted successfully");
		} catch (Exception e) {
			log.error("❌ Error broadcasting message: {}", e.getMessage(), e);
		}
	}
}
