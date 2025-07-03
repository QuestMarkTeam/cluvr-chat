# 간단한 재시도 횟수 추적 시스템 (Legacy)

## 개요

메시지 자체에 `retryCount` 필드를 포함시켜 재시도 횟수를 추적하는 간단하고 실용적인 방법으로 구현했습니다.

> **⚠️ 주의**: 이 문서는 이전 버전의 간단한 재시도 시스템을 설명합니다. 
> 최신 개선된 시스템은 `RETRY_SYSTEM_README.md`를 참조하세요.

## 구현 방식

### 1. 메시지 구조
```json
{
  "roomId": "123",
  "userId": "user1",
  "nickname": "사용자1",
  "message": "안녕하세요",
  "type": "TEXT",
  "messageId": "msg-001",
  "retryCount": 2
}
```

### 2. 동작 흐름

#### 초기 메시지 처리 실패 시
```java
// KafkaChatConsumer에서 처리 실패
String dlqMessage = addRetryCountToMessage(message, 0);
kafkaTemplate.send("chat-message.DLQ", dlqMessage);
```

#### DLQ에서 재처리 시
```java
// KafkaChatDlqConsumer에서 재처리
JsonNode node = objectMapper.readTree(message);
int retryCount = node.has("retryCount") ? node.get("retryCount").asInt() : 0;

if (retryCount >= 3) {
    // 3회 이상 실패: 영구 보관/알림
    log.error("DLQ 메시지 3회 이상 재처리 실패, 영구 보관 또는 알림 필요: {}", message);
    return;
}

// retryCount 증가 후 다시 처리
((ObjectNode) node).put("retryCount", retryCount + 1);
kafkaChatConsumer.processMessage(node.toString());
```

#### 재처리 실패 시
```java
// 증가된 retryCount로 다시 DLQ로 전송
JsonNode node = objectMapper.readTree(message);
int retryCount = node.has("retryCount") ? node.get("retryCount").asInt() : 0;
((ObjectNode) node).put("retryCount", retryCount + 1);
kafkaTemplate.send("chat-message.DLQ", node.toString());
```

## 장점

1. **간단함**: Redis나 외부 저장소 없이 메시지 자체에 정보 포함
2. **직관적**: 메시지를 보면 바로 재시도 횟수 확인 가능
3. **확장성**: 메시지와 함께 이동하므로 분산 환경에서도 안전
4. **디버깅 용이**: 로그에서 메시지 내용과 재시도 횟수를 함께 확인 가능

## 로그 예시

### 재시도 시작
```
WARN  - DLQ 메시지 재처리 시작: {"roomId":"123","message":"안녕하세요","retryCount":1}
```

### 재시도 성공
```
INFO  - DLQ 메시지 재처리 성공: retryCount=2, message={"roomId":"123","message":"안녕하세요","retryCount":1}
```

### 최대 재시도 횟수 초과
```
ERROR - DLQ 메시지 3회 이상 재처리 실패, 영구 보관 또는 알림 필요: {"roomId":"123","message":"안녕하세요","retryCount":3}
```

### 재시도 실패 시 재전송
```
ERROR - 재시도 실패로 DLQ 토픽으로 메시지 재전송: retryCount=2
```

## 확장 가능한 기능

### 1. 영구 보관 구현
```java
// FailedMessageRepository 생성
@Entity
public class FailedMessage {
    @Id
    private String id;
    private String originalMessage;
    private int retryCount;
    private LocalDateTime failedAt;
    private String failureReason;
}

// DLQ Consumer에서 사용
if (retryCount >= 3) {
    FailedMessage failedMessage = new FailedMessage();
    failedMessage.setOriginalMessage(message);
    failedMessage.setRetryCount(retryCount);
    failedMessage.setFailedAt(LocalDateTime.now());
    failedMessage.setFailureReason("최대 재시도 횟수 초과");
    failedMessageRepository.save(failedMessage);
    return;
}
```

### 2. 알림 서비스 구현
```java
// Slack 알림 예시
if (retryCount >= 3) {
    String slackMessage = String.format(
        "🚨 DLQ 메시지 최대 재시도 횟수 초과\n" +
        "재시도 횟수: %d\n" +
        "메시지: %s", 
        retryCount, message
    );
    slackService.sendNotification(slackMessage);
}
```

### 3. 설정 가능한 최대 재시도 횟수
```java
@Value("${app.dlq.max-retry-count:3}")
private int maxRetryCount;

if (retryCount >= maxRetryCount) {
    // 최대 재시도 횟수 초과 처리
}
```

## 주의사항

1. **메시지 크기**: `retryCount` 필드가 추가되므로 메시지 크기가 약간 증가합니다.
2. **JSON 파싱**: 메시지가 유효한 JSON이어야 하므로 파싱 실패 시 대체 로직이 필요합니다.
3. **메시지 무결성**: 재시도 과정에서 메시지 내용이 변경되지 않도록 주의해야 합니다.

## 테스트

### 메시지 흐름 테스트
1. 정상 메시지 전송 → 성공
2. 처리 실패 메시지 → DLQ로 전송 (retryCount: 0)
3. DLQ 재처리 성공 → 정상 처리
4. DLQ 재처리 실패 → DLQ 재전송 (retryCount: 1)
5. 3회 실패 → 영구 보관/알림

### JSON 파싱 테스트
```java
// 유효한 JSON
{"roomId":"123","message":"test","retryCount":0}

// 유효하지 않은 JSON (대체 로직 사용)
"invalid json" → {"originalMessage":"invalid json","retryCount":0}
```

이 방식은 복잡한 인프라 없이도 효과적인 재시도 횟수 추적이 가능하며, 메시지와 함께 이동하는 특성상 분산 환경에서도 안정적으로 동작합니다. 