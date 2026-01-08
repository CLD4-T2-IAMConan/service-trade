# Service Trade

Passit 프로젝트의 거래 및 결제 관리 마이크로서비스입니다.

## 목차

- [프로젝트 개요](#프로젝트-개요)
- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [시작하기](#시작하기)
- [API 문서](#api-문서)
- [환경 변수](#환경-변수)
- [개발 규칙](#개발-규칙)

## 프로젝트 개요

Service Trade는 Passit 플랫폼의 거래 생성, 상태 관리, 결제 처리를 담당하는 독립적인 마이크로서비스입니다.

### 주요 책임

- 거래 생성 및 관리
- 거래 상태 관리 (요청, 승인, 결제, 완료, 취소)
- 결제 처리 및 검증
- 거래 히스토리 관리
- SNS 이벤트 발행
- 티켓 서비스 연동

## 주요 기능

### 거래 관리

- **거래 생성**: 티켓 구매 거래 생성
- **거래 승인**: 판매자의 거래 승인/거절
- **거래 취소**: 구매자/판매자의 거래 취소
- **거래 완료**: 거래 완료 처리
- **거래 조회**: 거래 상세 정보 조회
- **거래 목록**: 사용자별 거래 목록 (구매/판매)

### 거래 상태 관리

거래는 다음과 같은 상태를 가집니다:

1. **REQUESTED**: 구매자가 거래 요청
2. **APPROVED**: 판매자가 거래 승인
3. **PAID**: 구매자가 결제 완료
4. **COMPLETED**: 거래 완료
5. **CANCELLED**: 거래 취소
6. **REJECTED**: 판매자가 거래 거절

### 결제 관리

- **결제 생성**: 거래에 대한 결제 정보 생성
- **결제 완료**: 결제 완료 처리
- **결제 검증**: 결제 정보 검증
- **결제 취소**: 결제 취소 및 환불 처리
- **결제 내역**: 결제 히스토리 조회

### 이벤트 처리

- **거래 이벤트 발행**: SNS를 통한 거래 상태 변경 이벤트 발행
- **티켓 상태 동기화**: 거래 상태에 따른 티켓 상태 업데이트 이벤트 발행
- **알림 이벤트**: 거래 관련 알림 이벤트 발행

### 서비스 간 통신

- **티켓 서비스 조회**: 티켓 정보 조회 및 검증
- **사용자 정보 확인**: Account 서비스를 통한 사용자 검증
- **채팅방 생성**: Chat 서비스 연동

## 기술 스택

### Backend

- **Java**: 17
- **Spring Boot**: 3.2.0
- **Spring Data JPA**: 데이터베이스 접근
- **Spring Security**: JWT 토큰 검증
- **Spring Validation**: 요청 검증
- **Spring WebFlux**: 비동기 HTTP 클라이언트
- **AWS SDK**: SNS 연동
- **Lombok**: 보일러플레이트 코드 감소

### Database

- **MySQL**: 8.0+

### AWS Services

- **SNS**: 이벤트 발행
- **SQS**: 이벤트 수신 (선택)

### Testing

- **JUnit 5**: 단위 테스트
- **Mockito**: Mock 객체 생성
- **WebTestClient**: API 통합 테스트

### Build Tool

- **Gradle**: 8.5

## 시작하기

### 사전 요구사항

다음 도구들이 설치되어 있어야 합니다:

| 도구           | 버전                      | 설치 링크                                                |
| -------------- | ------------------------- | -------------------------------------------------------- |
| IntelliJ IDEA  | 최신 (Community/Ultimate) | [JetBrains](https://www.jetbrains.com/idea/)             |
| JDK            | 17                        | [Adoptium](https://adoptium.net)                         |
| Docker Desktop | 최신                      | [Docker](https://www.docker.com/products/docker-desktop) |
| Git            | 최신                      | [Git](https://git-scm.com)                               |

### 로컬 개발 환경 설정

#### 1. 리포지토리 클론

```bash
git clone https://github.com/your-org/service-trade.git
cd service-trade
```

#### 2. 데이터베이스 시작 (Docker Compose)

```bash
docker-compose up -d
```

MySQL 컨테이너가 시작됩니다.

#### 3. 환경 변수 설정

필요한 경우 환경 변수를 설정하세요:

```bash
# 데이터베이스
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=passit_db
export DB_USER=passit_user
export DB_PASSWORD=passit_password

# 서비스 통신
export TICKET_SERVICE_URL=http://localhost:8082
export ACCOUNT_SERVICE_URL=http://localhost:8081

# AWS (선택)
export AWS_REGION=ap-northeast-2
export AWS_SNS_TOPIC_ARN=arn:aws:sns:...
```

#### 4. 애플리케이션 실행

**방법 1: Gradle로 실행**

```bash
./gradlew bootRun
```

**방법 2: IntelliJ IDEA**

1. 프로젝트를 IntelliJ IDEA로 열기
2. `src/main/java/com/company/trade/TradeApplication.java` 실행
3. Run Configuration에서 환경 변수 설정 가능

애플리케이션은 `http://localhost:8085`에서 실행됩니다.

#### 5. Health Check 확인

```bash
curl http://localhost:8085/api/health
```

응답 예시:

```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "service-trade"
  },
  "error": null
}
```

### 빌드 및 테스트

#### 전체 빌드

```bash
./gradlew clean build
```

#### 단위 테스트 실행

```bash
./gradlew test
```

#### 테스트 리포트 확인

```bash
# 리포트 확인: build/reports/tests/test/index.html
open build/reports/tests/test/index.html
```

## API 문서

### 공통 응답 형식

모든 API는 다음 형식으로 응답합니다:

#### 성공 응답

```json
{
  "success": true,
  "data": {
    /* 실제 데이터 */
  },
  "message": "성공 메시지",
  "error": null
}
```

#### 오류 응답

```json
{
  "success": false,
  "data": null,
  "message": null,
  "error": "에러 메시지"
}
```

### 주요 엔드포인트

#### 거래 API (`/api/deals`)

| Method | Endpoint                | 설명                  | 인증 필요 |
| ------ | ----------------------- | --------------------- | --------- |
| POST   | `/`                     | 거래 생성             | ✅        |
| GET    | `/{dealId}`             | 거래 상세 조회        | ✅        |
| GET    | `/my/buying`            | 내 구매 목록          | ✅        |
| GET    | `/my/selling`           | 내 판매 목록          | ✅        |
| POST   | `/{dealId}/approve`     | 거래 승인             | ✅        |
| POST   | `/{dealId}/reject`      | 거래 거절             | ✅        |
| POST   | `/{dealId}/cancel`      | 거래 취소             | ✅        |
| POST   | `/{dealId}/complete`    | 거래 완료             | ✅        |
| GET    | `/ticket/{ticketId}`    | 티켓별 거래 목록      | ✅        |

#### 결제 API (`/api/payments`)

| Method | Endpoint                | 설명                  | 인증 필요 |
| ------ | ----------------------- | --------------------- | --------- |
| POST   | `/`                     | 결제 생성             | ✅        |
| GET    | `/{paymentId}`          | 결제 상세 조회        | ✅        |
| POST   | `/{paymentId}/complete` | 결제 완료             | ✅        |
| POST   | `/{paymentId}/cancel`   | 결제 취소             | ✅        |
| GET    | `/deal/{dealId}`        | 거래별 결제 정보      | ✅        |
| GET    | `/my`                   | 내 결제 내역          | ✅        |

#### 티켓 정보 API (`/api/tickets`)

| Method | Endpoint                | 설명                  | 인증 필요 |
| ------ | ----------------------- | --------------------- | --------- |
| GET    | `/{ticketId}`           | 티켓 정보 조회        | ✅        |
| GET    | `/{ticketId}/available` | 티켓 구매 가능 여부   | ✅        |

#### Health Check

| Method | Endpoint           | 설명                     |
| ------ | ------------------ | ------------------------ |
| GET    | `/api/health`      | 서비스 헬스 체크         |
| GET    | `/actuator/health` | Spring Actuator 헬스 체크 |

### 요청/응답 예시

#### 거래 생성

**Request:**

```json
POST /api/deals
Authorization: Bearer {token}
Content-Type: application/json

{
  "ticketId": 123,
  "buyerId": 456,
  "price": 150000,
  "message": "빠른 거래 원합니다"
}
```

**Response:**

```json
{
  "success": true,
  "data": {
    "dealId": 789,
    "ticketId": 123,
    "buyerId": 456,
    "sellerId": 111,
    "status": "REQUESTED",
    "price": 150000,
    "createdAt": "2024-01-08T10:00:00"
  },
  "message": "거래가 요청되었습니다.",
  "error": null
}
```

#### 거래 승인

**Request:**

```json
POST /api/deals/789/approve
Authorization: Bearer {token}
```

**Response:**

```json
{
  "success": true,
  "data": {
    "dealId": 789,
    "status": "APPROVED",
    "updatedAt": "2024-01-08T10:05:00"
  },
  "message": "거래가 승인되었습니다.",
  "error": null
}
```

## 환경 변수

### 데이터베이스

| 변수          | 설명              | 기본값          |
| ------------- | ----------------- | --------------- |
| `DB_HOST`     | MySQL 호스트      | localhost       |
| `DB_PORT`     | MySQL 포트        | 3306            |
| `DB_NAME`     | 데이터베이스 이름 | passit_db       |
| `DB_USER`     | DB 사용자         | passit_user     |
| `DB_PASSWORD` | DB 비밀번호       | passit_password |

### 서비스 통신

| 변수                   | 설명                 | 기본값                 |
| ---------------------- | -------------------- | ---------------------- |
| `TICKET_SERVICE_URL`   | Ticket 서비스 URL    | http://localhost:8082  |
| `ACCOUNT_SERVICE_URL`  | Account 서비스 URL   | http://localhost:8081  |
| `CHAT_SERVICE_URL`     | Chat 서비스 URL      | http://localhost:8083  |

### AWS 설정 (선택)

| 변수                   | 설명                | 기본값           |
| ---------------------- | ------------------- | ---------------- |
| `AWS_REGION`           | AWS 리전            | ap-northeast-2   |
| `AWS_SNS_TOPIC_ARN`    | SNS Topic ARN       | -                |
| `AWS_ACCESS_KEY_ID`    | AWS Access Key      | -                |
| `AWS_SECRET_ACCESS_KEY`| AWS Secret Key      | -                |

## 프로젝트 구조

```
service-trade/
├── src/
│   ├── main/
│   │   ├── java/com/company/trade/
│   │   │   ├── config/              # 설정 클래스 (Security, AWS, WebClient 등)
│   │   │   ├── controller/          # REST API 컨트롤러
│   │   │   ├── dto/                 # 요청/응답 DTO
│   │   │   ├── entity/              # JPA 엔티티
│   │   │   ├── exception/           # 커스텀 예외 및 핸들러
│   │   │   ├── repository/          # JPA Repository
│   │   │   ├── service/             # 비즈니스 로직
│   │   │   └── TradeApplication.java
│   │   └── resources/
│   │       ├── application.yml      # 애플리케이션 설정
│   │       ├── application-dev.yml  # Dev 환경 설정
│   │       └── application-prod.yml # Prod 환경 설정
│   └── test/
│       ├── java/                    # 테스트 코드
│       │   ├── controller/         # 컨트롤러 테스트
│       │   └── service/            # 서비스 테스트
│       └── resources/
│           ├── application-test.yml
│           └── data-test.sql
├── build.gradle                     # Gradle 빌드 설정
├── docker-compose.yml               # Docker Compose 설정
├── helm/                            # Helm 차트
├── k8s/                             # Kubernetes 매니페스트
├── scripts/                         # 유틸리티 스크립트
└── README.md
```

## 개발 규칙

### 코드 스타일

- Java Code Convention 준수
- Lombok 적극 활용
- Layer 간 의존성 방향: Controller → Service → Repository
- DTO 사용으로 Entity 노출 방지
- 모든 API 응답은 `ApiResponse` 래퍼 사용

### 거래 상태 전이 규칙

```
REQUESTED → APPROVED → PAID → COMPLETED
    ↓           ↓         ↓
CANCELLED   REJECTED  CANCELLED
```

- `REQUESTED`: 초기 상태, 구매자가 생성
- `APPROVED`: 판매자 승인 후
- `REJECTED`: 판매자 거절 시 (종료)
- `PAID`: 결제 완료 후
- `COMPLETED`: 거래 완료 (종료)
- `CANCELLED`: 언제든지 취소 가능 (종료)

### 브랜치 전략

- `main`: 운영 환경 배포 브랜치
- `develop`: 개발 환경 통합 브랜치
- `feature/{기능명}`: 기능 개발 브랜치
- `bugfix/{버그명}`: 버그 수정 브랜치

### 커밋 메시지 규칙

```
feat: 새로운 기능 추가
fix: 버그 수정
docs: 문서 수정
style: 코드 포맷팅
refactor: 코드 리팩토링
test: 테스트 코드
chore: 빌드, 설정 파일 수정
```

### Pull Request 규칙

1. PR 생성 시 템플릿 작성
2. 최소 1명 이상 리뷰어 지정
3. CI 성공 필수
4. 승인 후 Squash Merge

## 트러블슈팅

### 빌드 실패

```bash
# Gradle 캐시 삭제
./gradlew clean

# Gradle Wrapper 재설치
./gradlew wrapper
```

### 데이터베이스 연결 실패

```bash
# Docker 컨테이너 상태 확인
docker-compose ps

# MySQL 로그 확인
docker-compose logs mysql

# 컨테이너 재시작
docker-compose restart mysql
```

### 서비스 간 통신 실패

- 서비스 URL 확인
- 네트워크 연결 확인
- JWT 토큰 전파 확인
- 방화벽 설정 확인

### SNS 이벤트 발행 실패

- AWS 자격증명 확인
- IAM 권한 확인 (SNS Publish)
- Topic ARN 확인
- 네트워크 연결 확인

## 배포

### Docker 빌드

```bash
docker build -t trade-service:latest .
```

### Kubernetes 배포

```bash
kubectl apply -f k8s/
```

### Helm 배포

```bash
helm install trade-service ./helm
```

## 라이선스

MIT License

## 문의

프로젝트 관련 문의사항은 팀 PM에게 연락하세요.
