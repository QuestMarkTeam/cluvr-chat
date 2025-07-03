# 개선된 재시도 시스템 (Enhanced Retry System)

## 개요

기존의 단순한 `retryCount` 처리에서 더욱 체계적이고 확장 가능한 재시도 시스템으로 개선했습니다.

## 주요 개선사항

### 1. 에러 타입별 차별화된 처리
- **JSON_PARSE_ERROR**: 재시도 의미없음, 즉시 영구 보관
- **PROCESSING_ERROR**: 재시도 가능한 에러, 지수 백오프 적용
- **RETRY_FAILURE**: 재시도 중 발생한 에러

### 2. 설정 가능한 재시도 정책
```yaml
app:
  kafka:
    max-retry-count: 3                    # 최대 재시도 횟수
    retry-delay-multiplier: 2             # 지수 백오프 배수
```

### 3. 지수 백오프 (Exponential Backoff)
- 1차 재시도: 2초 지연
- 2차 재시도: 4초 지연  
- 3차 재시도: 8초 지연

### 4. 상세한 에러 정보 추적
```json
{
  "roomId": "123",
  "userId": "user1",
  "message": "안녕하세요",
  "retryCount": 2,
  "errorType": "PROCESSING_ERROR",
  "errorMessage": "Database connection failed",
  "lastRetryAt": "2024-01-15T10:30:00"
}
```

## 시스템 아키텍처

### 메시지 흐름
```
1. Kafka Consumer (chat-message)
   ↓ (처리 실패)
2. DLQ (chat-message.DLQ)
   ↓ (재시도)
3. DLQ Consumer
   ↓ (성공/실패)
4. 성공: 정상 처리
   실패: 재시도 또는 영구 보관
```

### 에러 처리 로직
```
에러 발생
    ↓
에러 타입 분류
    ↓
JSON_PARSE_ERROR? → 즉시 영구 보관
    ↓ (아니오)
최대 재시도 횟수 확인
    ↓
초과? → 영구 보관 + 알림
    ↓ (아니오)
지수 백오프 지연
    ↓
재시도
    ↓
성공? → 정상 처리
    ↓ (실패)
retryCount 증가 후 DLQ 재전송
```

## 구현 세부사항

### 1. KafkaChatConsumer 개선
- 에러 타입별 차별화된 처리
- 상세한 재시도 정보 추가
- 헬퍼 메서드 제공

### 2. KafkaChatDlqConsumer 개선
- 지수 백오프 구현
- 에러 타입별 처리 로직
- 영구 보관 및 알림 준비

### 3. FailedMessage 엔티티
- 실패한 메시지 영구 보관
- 상세한 에러 정보 저장
- 처리 상태 추적

## 설정 옵션

### 환경 변수
```bash
# 최대 재시도 횟수 (기본값: 3)
APP_KAFKA_MAX_RETRY_COUNT=5

# 재시도 지연 배수 (기본값: 2)
APP_KAFKA_RETRY_DELAY_MULTIPLIER=3
```

### application.yml
```yaml
app:
  kafka:
    max-retry-count: ${APP_KAFKA_MAX_RETRY_COUNT:3}
    retry-delay-multiplier: ${APP_KAFKA_RETRY_DELAY_MULTIPLIER:2}
```

## 로그 예시

### 초기 처리 실패
```
WARN  - ❗역직렬화 실패 → DLQ로 이동: {"roomId":"123","message":"test"}
INFO  - 📦 DLQ 토픽 전송 성공: retryCount=0, errorType=JSON_PARSE_ERROR, message={...}
```

### DLQ 재처리 시작
```
WARN  - 🔄 DLQ 메시지 재처리 시작: {"roomId":"123","retryCount":1,"errorType":"PROCESSING_ERROR"}
INFO  - 📊 재처리 정보: retryCount=1, errorType=PROCESSING_ERROR, errorMessage=Database connection failed
INFO  - ⏰ 재시도 지연: 2초 후 재처리 시작
```

### 재시도 성공
```
INFO  - ✅ DLQ 메시지 재처리 성공: retryCount=2, message={...}
```

### 최대 재시도 횟수 초과
```
ERROR - 🚨 DLQ 메시지 최대 재시도 횟수 초과: retryCount=3, errorType=PROCESSING_ERROR, errorMessage=Database connection failed
ERROR - 📦 메시지 영구 보관 필요: {"roomId":"123","retryCount":3,...}
```

## 확장 가능한 기능

### 1. 영구 보관 구현
```java
// FailedMessageRepository 사용
FailedMessage failedMessage = FailedMessage.builder()
    .originalMessage(message)
    .retryCount(retryCount)
    .errorType(errorType)
    .errorMessage(errorMessage)
    .failedAt(LocalDateTime.now())
    .status("PENDING")
    .build();
failedMessageRepository.save(failedMessage);
```

### 2. 알림 서비스 구현
```java
// Slack 알림 예시
String slackMessage = String.format(
    "🚨 메시지 처리 최대 재시도 횟수 초과\n" +
    "재시도 횟수: %d\n" +
    "에러 타입: %s\n" +
    "에러 메시지: %s\n" +
    "메시지: %s", 
    retryCount, errorType, errorMessage, message
);
slackService.sendNotification(slackMessage);
```

### 3. 관리자 대시보드
- 실패한 메시지 목록 조회
- 에러 타입별 통계
- 재시도 성공률 분석
- 수동 재처리 기능

## 모니터링 및 알림

### 1. 메트릭 수집
- 재시도 횟수별 분포
- 에러 타입별 발생 빈도
- 재시도 성공률
- 평균 재시도 시간

### 2. 알림 조건
- 최대 재시도 횟수 초과
- 특정 에러 타입의 빈번한 발생
- 재시도 성공률 하락

## 성능 고려사항

### 1. 메시지 크기
- 재시도 정보 추가로 메시지 크기 증가
- 압축 고려 (필요시)

### 2. 처리 지연
- 지수 백오프로 인한 처리 지연
- 비즈니스 요구사항에 맞는 지연 시간 조정

### 3. 저장소 사용량
- FailedMessage 저장으로 인한 MongoDB 사용량 증가
- 정기적인 정리 정책 필요

## 테스트 시나리오

### 1. 정상 처리
```
메시지 전송 → 성공 처리 → 로그 확인
```

### 2. 일시적 실패
```
메시지 전송 → 처리 실패 → DLQ 전송 → 재시도 성공
```

### 3. 지속적 실패
```
메시지 전송 → 처리 실패 → DLQ 전송 → 재시도 실패 → 최대 횟수 초과 → 영구 보관
```

### 4. JSON 파싱 실패
```
잘못된 JSON 전송 → 즉시 영구 보관 (재시도 없음)
```

## 장점

1. **에러 분류**: 에러 타입별 차별화된 처리
2. **설정 가능**: 환경별 재시도 정책 조정
3. **지수 백오프**: 시스템 부하 감소
4. **상세 추적**: 디버깅 및 모니터링 용이
5. **확장성**: 향후 기능 확장 용이
6. **영구 보관**: 데이터 손실 방지

## 주의사항

1. **메시지 순서**: 재시도로 인한 메시지 순서 변경 가능성
2. **중복 처리**: idempotency 키로 중복 방지
3. **리소스 사용**: 재시도로 인한 리소스 사용량 증가
4. **모니터링**: 정기적인 실패 메시지 확인 필요 