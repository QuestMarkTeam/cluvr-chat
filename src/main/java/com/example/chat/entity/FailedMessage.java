package com.example.chat.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 실패한 메시지를 영구 보관하기 위한 엔티티
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "failed_messages")
public class FailedMessage {
    
    @Id
    private String id;
    
    /**
     * 원본 메시지 내용
     */
    private String originalMessage;
    
    /**
     * 재시도 횟수
     */
    private int retryCount;
    
    /**
     * 에러 타입
     */
    private String errorType;
    
    /**
     * 에러 메시지
     */
    private String errorMessage;
    
    /**
     * 마지막 재시도 시간
     */
    private LocalDateTime lastRetryAt;
    
    /**
     * 실패 시간
     */
    private LocalDateTime failedAt;
    
    /**
     * 메시지 ID (원본 메시지에서 추출)
     */
    private String messageId;
    
    /**
     * 채팅방 ID (원본 메시지에서 추출)
     */
    private String roomId;
    
    /**
     * 사용자 ID (원본 메시지에서 추출)
     */
    private String userId;
    
    /**
     * 처리 상태 (PENDING, PROCESSED, IGNORED)
     */
    private String status;
    
    /**
     * 처리 노트
     */
    private String notes;
} 