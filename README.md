# cluvr-chat

## 프로젝트 개요

**cluvr-chat**은 실시간 채팅, 알림, 외부 API 연동, 인증/인가, 메시지 브로커(Kafka, RabbitMQ), Redis Pub/Sub, MySQL, MongoDB 등 다양한 기술을 활용한 대규모 채팅/알림 백엔드 서버입니다.

## 전체 구조 다이어그램
사진 넣을 것이다~~~~~~~~~~~~~

---

### 시스템 메시지 플로우

1. **클라이언트**는 WebSocket(ChatWSController)로만 채팅 메시지를 전송할 수 있습니다.
2. ChatWSController는 ChatService의 **broadcastMessage()**를 호출합니다.
3. ChatService는
   - (1) 채팅방 멤버에게 RabbitMQ로 알림을 발송하고,
   - (2) **RedisPublisher**를 통해 메시지를 Redis Pub/Sub에 발행합니다.
4. RedisSubscriber가 Redis에서 메시지를 구독하여
   - (1) WebSocket 브로드캐스트로 클라이언트에 실시간 전달,
   - (2) Kafka Producer로 메시지를 전달합니다.
5. Kafka Consumer는 Kafka에서 메시지를 소비하여 **성공 메시지는 ChatLog(MongoDB)에 저장**합니다.
6. Kafka Consumer가 메시지 처리에 실패하면, 해당 메시지는 **DLQ(Dead Letter Queue)**로 이동합니다.
7. Kafka DLQ Consumer가 DLQ 메시지를 재처리하며,
   - 재처리 성공 시 원래 플로우(ChatLog 저장)로 복귀
   - 최대 재시도 초과/파싱 에러 등은 **FailedMessage(MongoDB)에 영구 보관**합니다.
8. **REST API(ChatController)**는 채팅 메시지 전송이 아닌, 채팅방 생성/조회/입장/멤버조회/메시지조회 등 관리성 엔드포인트만 담당합니다.

---

## 주요 기술 스택

- **Java 17, Spring Boot 3.5**
- **JPA (MySQL), Spring Data MongoDB**
- **WebSocket + STOMP**
- **Kafka, RabbitMQ, Redis Pub/Sub**
- **AWS Cognito (OAuth2/JWT 기반 인증)**
- **Docker, Docker Compose**
- **Lombok, Checkstyle**

---

## 폴더 구조 및 주요 컴포넌트

```plaintext
src/main/java/com/example/
  ├── Main.java                # SpringBoot 진입점
  ├── chat/
  │   ├── controller/          # REST, WebSocket 컨트롤러
  │   ├── service/             # 비즈니스 로직
  │   ├── entity/              # JPA/MongoDB 엔티티
  │   ├── repository/          # JPA/MongoDB 레포지토리
  │   ├── kafka/               # Kafka Producer/Consumer
  │   ├── pubsub/              # Redis Pub/Sub Publisher/Subscriber
  │   ├── config/              # WebSocket, Redis 등 설정
  │   ├── dto/                 # 요청/응답 DTO
  │   ├── enums/               # 도메인 Enum
  │   └── notification/        # 알림 이벤트/프로듀서
  ├── common/                  # 공통 엔티티/레포지토리
  └── global/                  # 글로벌 설정, 예외, 응답, 인터셉터
```

---

## 주요 기능

- **실시간 채팅**: WebSocket + STOMP, Redis Pub/Sub, Kafka 기반 메시지 송수신
- **채팅방 관리**: 채팅방 생성/목록/입장/퇴장/멤버 관리
- **알림 시스템**: RabbitMQ를 통한 실시간 알림
- **외부 API 연동**: 클럽/유저 정보 외부 API 호출
- **JWT, OAuth2 인증**: AWS Cognito 기반 인증/인가
- **DB 이중화**: MySQL(JPA) + MongoDB 동시 사용
- **장애/실패 메시지 관리**: Kafka DLQ, 실패 메시지 저장/재처리

---

## 환경 변수 및 설정

`.env` 파일 및 `application.yml`에서 주요 환경변수를 관리합니다.

- **DB**: MYSQL_HOST, MYSQL_PORT, MYSQL_DATABASE, MYSQL_USERNAME, MYSQL_PASSWORD
- **MongoDB**: MONGODB_HOST, MONGODB_PORT, MONGODB_DATABASE
- **Redis**: REDIS_HOST, REDIS_PORT
- **Kafka**: KAFKA_BOOTSTRAP_SERVERS, KAFKA_CONSUMER_GROUP_ID 등
- **JWT/Cognito**: JWT_SECRET_KEY, USER_POOL_ID, CLIENT_ID, CLIENT_SECRET
- **외부 API**: EXTERNAL_CLUB_API_BASE_URL
- **AWS**: ACCESS_AWS, SECRET_AWS, AWS_REGION
- **기타**: APP_CORS_ALLOWED_ORIGINS, LOGGING_LEVEL_ROOT 등

---

## 실행 방법

### 로컬 실행 (Docker Compose)

```bash
# 도커 컴포즈로 전체 서비스 실행
docker-compose -f docker-compose-local.yml up -d --build
```

- spring(백엔드), mysql, mongo, redis, kafka, zookeeper, kafka-ui 등 모든 서비스가 자동으로 실행됩니다.
- 기본 포트: 8082
- API 서버도 실행 중이여야 합니다.
---

## API/엔드포인트 예시

- **REST API**: `/api/chat/rooms`, `/api/chat/message` 등
- **WebSocket**: `/ws/chat` 엔드포인트, STOMP 프로토콜 사용
- **Redis/Kafka**: 내부 메시지 브로커 연동

---

## 개발 및 테스트
- **환경별 설정**: `application.yml`,`application-local.yml`, `.env`로 분리 관리

---

## 기타

- **Kafka UI**: http://localhost:8085
- **DB, Redis, Kafka 등 모두 Docker로 손쉽게 실행 가능**
- **AWS Cognito 연동 필요시, 관련 환경변수 필수 세팅**

---