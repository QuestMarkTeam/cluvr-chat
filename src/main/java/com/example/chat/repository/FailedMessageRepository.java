package com.example.chat.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.chat.entity.FailedMessage;

/**
 * 실패한 메시지 저장소
 */
@Repository
public interface FailedMessageRepository extends MongoRepository<FailedMessage, String> {
    
    /**
     * 특정 에러 타입으로 실패한 메시지 조회
     */
    List<FailedMessage> findByErrorType(String errorType);
    
    /**
     * 특정 상태의 메시지 조회
     */
    List<FailedMessage> findByStatus(String status);
    
    /**
     * 특정 기간 동안 실패한 메시지 조회
     */
    List<FailedMessage> findByFailedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * 특정 채팅방의 실패한 메시지 조회
     */
    List<FailedMessage> findByRoomId(String roomId);
    
    /**
     * 특정 사용자의 실패한 메시지 조회
     */
    List<FailedMessage> findByUserId(String userId);
    
    /**
     * 재시도 횟수가 특정 값 이상인 메시지 조회
     */
    List<FailedMessage> findByRetryCountGreaterThanEqual(int retryCount);
    
    /**
     * 복합 조건으로 실패한 메시지 조회
     */
    @Query("{'errorType': ?0, 'status': ?1, 'failedAt': {$gte: ?2, $lte: ?3}}")
    List<FailedMessage> findByErrorTypeAndStatusAndFailedAtBetween(
        String errorType, String status, LocalDateTime start, LocalDateTime end);
    
    /**
     * 처리되지 않은 메시지 수 조회
     */
    long countByStatus(String status);
} 