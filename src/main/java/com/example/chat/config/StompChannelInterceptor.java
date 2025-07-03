package com.example.chat.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StompChannelInterceptor implements ChannelInterceptor {
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && accessor.getSessionAttributes() != null) {
            Authentication authentication = (Authentication) accessor.getSessionAttributes().get("SPRING.AUTHENTICATION");
            if (authentication != null) {
                log.info("[WebSocket] 세션에서 인증 객체 복원 성공: {}", authentication);
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);

                accessor.setUser(authentication);
            } else {
                log.warn("[WebSocket] 세션에서 인증 객체 복원 실패");
            }
        }
        return message;
    }
}
