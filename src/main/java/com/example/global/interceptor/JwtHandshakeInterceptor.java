package com.example.global.interceptor;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.example.chat.service.GetInfoFromExternal;
import com.example.global.exception.BusinessException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

	private final JwtDecoder jwtDecoder;
	private final GetInfoFromExternal getInfoFromExternal;

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
		WebSocketHandler wsHandler, Map<String, Object> attributes) {

		if (request instanceof ServletServerHttpRequest servletRequest) {
			HttpServletRequest httpServletRequest = servletRequest.getServletRequest();

			String origin = httpServletRequest.getHeader("Origin");
			log.info("[WebSocket] Origin~~~~~~~~~~: {}", origin);

			// 1. Authorization 헤더에서 토큰 확인
			String authHeader = httpServletRequest.getHeader("Authorization");
			String token = null;

			if (authHeader != null && authHeader.startsWith("Bearer ")) {
				token = authHeader.substring(7);
				log.info("[WebSocket] Authorization 헤더에서 토큰 추출~~~~~~~~~~: {}", token);
			} else {
				// 2. 쿼리 파라미터에서 토큰 확인
				token = httpServletRequest.getParameter("token");
				log.info("[WebSocket] 쿼리 파라미터에서 토큰 추출~~~~~~~~~~: {}", token);
			}

			if (token != null && !token.isEmpty()) {
				try {
					Jwt jwt = jwtDecoder.decode(token);
					log.info("[WebSocket] JWT 파싱 성공~~~~~~~~~~: sub={}, exp={}", jwt.getSubject(), jwt.getExpiresAt());
					if (jwt.getExpiresAt() != null && jwt.getExpiresAt().isBefore(java.time.Instant.now())) {
						log.error("[WebSocket] JWT가 만료되었습니다~~~~~~~~~~ exp={}, now={}", jwt.getExpiresAt(), java.time.Instant.now());
						return false;
					}
					Authentication authentication = new JwtAuthenticationToken(jwt);
					authentication.setAuthenticated(true); // ★ 반드시 true로!
					SecurityContextHolder.getContext().setAuthentication(authentication);
					log.info("[WebSocket] JWT 인증 성공~~~~~~~~~~");
					log.info("[WebSocket] 인증 객체 세션에 저장: {}", authentication);
					attributes.put("SPRING.AUTHENTICATION", authentication);
				} catch (Exception e) {
					log.error("[WebSocket] JWT 인증 실패~~~~~~~~~~: {}", e.getMessage(), e);
					return false; // JWT 파싱 실패 시 연결 거부
				}
			} else {
				log.warn("[WebSocket] 연결에 토큰이 없습니다~~~~~~~~~~");
				return false; // 토큰이 없으면 연결 거부
			}
		}
		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
		WebSocketHandler wsHandler, Exception exception) {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Jwt jwt =(Jwt) auth.getPrincipal();
		String sub = jwt.getSubject();
		try {
			Long userId = getInfoFromExternal.getUserIdFromSub(sub);
			log.info("WebSocket 연결 완료 - userId: {}", userId);
		} catch (BusinessException e) {
			log.error("WebSocket 핸드셰이크 중 userId 조회 실패: {}", e.getMessage());
		}
	}
}
