# MSA Service Template

MSA(Service 단위) Spring Boot 기본 템플릿 레포지토리입니다.
모든 신규 서비스는 이 템플릿을 기반으로 빠르고 일관성 있게 생성할 수 있습니다.

## 목차

- [프로젝트 개요](#프로젝트-개요)
- [기술 스택](#기술-스택)
- [시작하기](#시작하기)
- [템플릿 사용 방법](#템플릿-사용-방법)
- [로컬 개발 환경](#로컬-개발-환경)
- [프로젝트 구조](#프로젝트-구조)
- [배포](#배포)
- [API 문서](#api-문서)
- [개발 규칙](#개발-규칙)
- [IntelliJ IDEA 추천 설정](#intellij-idea-추천-설정)

## 프로젝트 개요

이 템플릿은 MSA 기반 Spring Boot 서비스를 빠르게 시작할 수 있도록 다음을 제공합니다:

- Spring Boot 3.x 기반 프로젝트 구조
- Docker & Docker Compose 설정
- Kubernetes 매니페스트 및 Helm Chart
- GitHub Actions CI/CD 파이프라인
- 공통 API 응답 형식 및 예외 처리
- Health Check 엔드포인트

## 기술 스택

### Backend

- **Java**: 17
- **Spring Boot**: 3.2.x
- **Spring Data JPA**: 데이터베이스 접근
- **Spring Validation**: 요청 검증
- **Lombok**: 보일러플레이트 코드 감소

### Database

- **PostgreSQL**: 16

### DevOps

- **Docker**: 컨테이너화
- **Kubernetes**: 오케스트레이션
- **Helm**: 패키지 관리
- **GitHub Actions**: CI/CD

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
| kubectl        | 1.27+                     | [Kubernetes](https://kubernetes.io)                      |
| kind           | 최신                      | [kind](https://kind.sigs.k8s.io)                         |
| Helm           | 최신                      | [Helm](https://helm.sh)                                  |
| Git            | 최신                      | [Git](https://git-scm.com)                               |

## 템플릿 사용 방법

### 1. 템플릿에서 새 리포지토리 생성

1. GitHub에서 이 리포지토리 페이지로 이동
2. 우측 상단 **Use this template** 버튼 클릭
3. 새 리포지토리 이름 입력 (예: `service-account`, `service-ticket`)
4. **Create repository** 클릭

### 2. 로컬로 클론

```bash
git clone https://github.com/your-org/your-service-name.git
cd your-service-name
```

### 3. 프로젝트 설정 변경

다음 파일들에서 `template`을 실제 서비스 이름으로 변경하세요:

#### 필수 변경 파일:

- `settings.gradle`: `rootProject.name`
- `src/main/resources/application.yml`: `spring.application.name`
- `src/main/java/com/company/template/`: 패키지명 변경
- `helm/Chart.yaml`: `name`, `description`
- `helm/values.yaml`: 이미지 이름 등

#### 자동 변경 스크립트 (선택):

```bash
./scripts/rename-service.sh your-service-name
```

## 로컬 개발 환경

### 방법 1: Docker Compose 사용 (권장)

#### 1) 데이터베이스 시작

```bash
docker-compose up -d postgres
```

#### 2) 애플리케이션 실행

```bash
./gradlew bootRun
```

#### 3) 전체 스택 실행 (앱 + DB)

```bash
docker-compose up -d
```

#### 4) 종료

```bash
docker-compose down
```

### 방법 2: IntelliJ IDEA 사용 (권장)

#### 1) 프로젝트 열기

1. IntelliJ IDEA 실행
2. `File > Open` 선택
3. 프로젝트 루트 디렉토리 선택 (`build.gradle` 파일이 있는 위치)
4. `Open as Project` 클릭
5. Gradle 프로젝트가 자동으로 import 됨

#### 2) JDK 설정 확인

1. `File > Project Structure` (`Cmd+;` 또는 `Ctrl+Alt+Shift+S`)
2. `Project` 섹션에서 SDK를 `17` 이상으로 설정
3. Language level도 `17`로 설정

#### 3) 애플리케이션 실행

1. `src/main/java/com/company/template/TemplateApplication.java` 파일 열기
2. 파일 내 `main` 메서드 왼쪽의 ▶️ 버튼 클릭
3. `Run 'TemplateApplication'` 선택

또는 상단 메뉴:

- `Run > Run...` 선택 후 `TemplateApplication` 선택

#### 4) 빌드

- **전체 프로젝트 빌드**: `Build > Build Project` (`Cmd+F9` 또는 `Ctrl+F9`)
- **Gradle 빌드**: 우측 Gradle 패널 > `Tasks > build > build` 더블클릭

#### 5) 테스트 실행

- **전체 테스트**: `src/test/java` 우클릭 > `Run 'Tests in...'`
- **개별 테스트**: 테스트 파일 내에서 ▶️ 버튼 클릭
- **Gradle 테스트**: Gradle 패널 > `Tasks > verification > test`

#### 6) Run Configuration 설정 (선택)

1. 상단 Run Configuration 드롭다운 클릭 > `Edit Configurations...`
2. Environment Variables에 추가:
   ```
   SPRING_PROFILES_ACTIVE=local
   ```
3. VM Options에 추가 (필요시):
   ```
   -Xmx512m -Xms256m
   ```

### 방법 3: Gradle로 직접 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun

# 테스트
./gradlew test
```

### Health Check 확인

```bash
curl http://localhost:8080/api/health
```

응답 예시:

```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "template"
  },
  "error": null
}
```

### 패키지 구조 상세

```
com.company.{service-name}
 ├── controller/          # Presentation Layer
 │   ├── api/            # API 버전별 컨트롤러
 │   └── request/        # 요청 DTO
 │
 ├── service/            # Business Layer
 │   ├── impl/           # 서비스 구현체
 │   └── mapper/         # Entity ↔ DTO 변환
 │
 ├── repository/         # Persistence Layer
 │   ├── custom/         # 커스텀 Repository
 │   └── specification/  # JPA Specification
 │
 ├── domain/             # Domain Layer
 │   ├── entity/         # JPA 엔티티
 │   ├── vo/             # Value Objects
 │   └── enums/          # 열거형
 │
 ├── dto/                # Data Transfer Objects
 │   ├── request/        # API 요청 DTO
 │   └── response/       # API 응답 DTO
 │
 ├── config/             # Infrastructure
 │   ├── security/       # 보안 설정
 │   ├── database/       # DB 설정
 │   └── web/            # Web 설정
 │
 ├── exception/          # Exception Handling
 │   ├── custom/         # 커스텀 예외
 │   └── handler/        # 전역 예외 핸들러
 │
 └── util/               # Utilities
     ├── validation/     # 커스텀 Validator
     └── constant/       # 상수
```

## 프로젝트 구조

```
msa-service-template/
├── .github/
│   ├── workflows/          # GitHub Actions 워크플로우
│   └── PULL_REQUEST_TEMPLATE.md
├── helm/                   # Helm Chart
│   ├── templates/
│   ├── Chart.yaml
│   └── values.yaml
├── k8s/                    # Kubernetes 매니페스트
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── configmap.yaml
│   └── secret.yaml
├── scripts/                # 유틸리티 스크립트
├── src/
│   ├── main/
│   │   ├── java/com/company/template/
│   │   │   ├── controller/     # REST 컨트롤러
│   │   │   ├── service/        # 비즈니스 로직
│   │   │   ├── repository/     # 데이터 접근
│   │   │   ├── entity/         # JPA 엔티티
│   │   │   ├── dto/            # 데이터 전송 객체
│   │   │   ├── config/         # 설정 클래스
│   │   │   ├── exception/      # 예외 처리
│   │   │   └── TemplateApplication.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
├── build.gradle            # Gradle 빌드 설정
├── settings.gradle
├── Dockerfile             # Docker 이미지 빌드
├── docker-compose.yml     # 로컬 개발 환경
└── README.md
```

## 배포

### Kubernetes (kind 로컬 클러스터)

#### 1) 클러스터 생성

```bash
kind create cluster --name msa-local
```

#### 2) 이미지 빌드 & 로드

```bash
docker build -t template-service:latest .
kind load docker-image template-service:latest --name msa-local
```

#### 3) 배포

```bash
kubectl apply -f k8s/
```

#### 4) 확인

```bash
kubectl get pods
kubectl get services
kubectl logs -f <pod-name>
```

#### 5) 포트 포워딩으로 접근

```bash
kubectl port-forward svc/template-service 8080:80
```

### Helm

#### 설치

```bash
helm install my-service ./helm
```

#### 업그레이드

```bash
helm upgrade my-service ./helm
```

#### 삭제

```bash
helm uninstall my-service
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
  "error": null
}
```

#### 오류 응답

```json
{
  "success": false,
  "data": null,
  "error": "Error message"
}
```

### 주요 엔드포인트

- `GET /api/health`: 헬스 체크
- `GET /actuator/health`: Spring Boot Actuator 헬스 체크
- `GET /actuator/info`: 애플리케이션 정보

## 개발 규칙

### 패키지 구조 규칙

```
com.company.{service-name}
 ├── controller      # REST API 엔드포인트
 ├── service         # 비즈니스 로직
 ├── repository      # 데이터베이스 접근
 ├── entity          # JPA 엔티티
 ├── dto             # 요청/응답 DTO
 ├── config          # 설정 클래스
 └── exception       # 커스텀 예외 및 핸들러
```

### 브랜치 전략

- `main`: 운영 환경 배포 브랜치
- `develop`: 개발 환경 통합 브랜치
- `feature/{기능명}`: 기능 개발 브랜치
- `bugfix/{버그명}`: 버그 수정 브랜치

### Pull Request 규칙

1. PR 생성 시 템플릿 작성
2. 최소 1명 이상 리뷰어 지정
3. CI 성공 필수
4. 승인 후 Squash Merge

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

### 코드 스타일

- Java Code Convention 준수
- Lombok 적극 활용
- Layer 간 의존성 방향: Controller → Service → Repository
- DTO 사용으로 Entity 노출 방지

## IntelliJ IDEA 추천 설정

### 필수 플러그인

권장 플러그인 설치 방법: `File > Settings > Plugins`

- **Lombok**: Lombok 어노테이션 지원 (필수)
- **String Manipulation**: 문자열 조작 단축키
- **Rainbow Brackets**: 괄호 가독성 향상
- **SonarLint**: 코드 품질 검사

### Lombok 설정

Lombok 사용을 위한 필수 설정:

1. `File > Settings > Build, Execution, Deployment > Compiler > Annotation Processors`
2. `Enable annotation processing` 체크

### 코드 스타일 설정

1. `File > Settings > Editor > Code Style > Java`
2. 권장 설정:
   - Indent: 4 spaces
   - Tab size: 4
   - Continuation indent: 8

### Live Templates 추천

자주 사용하는 코드 스니펫 등록: `File > Settings > Editor > Live Templates`

```java
// psvm - public static void main
public static void main(String[] args) {
    $END$
}

// sout - System.out.println
System.out.println($END$);

// rest - REST Controller 생성
@RestController
@RequestMapping("/api/$PATH$")
@RequiredArgsConstructor
public class $CLASS$Controller {
    $END$
}
```

## CI/CD

### GitHub Actions 워크플로우

1. **CI** (`.github/workflows/ci.yml`)

   - PR 생성 시 자동 빌드 및 테스트
   - 테스트 결과 리포트 생성

2. **Docker Build** (`.github/workflows/docker-build.yml`)

   - main 브랜치 푸시 시 Docker 이미지 빌드
   - GitHub Container Registry에 푸시

3. **Code Quality** (`.github/workflows/code-quality.yml`)
   - 코드 스타일 검사
   - 정적 분석

## 환경 변수

### 로컬 개발

`src/main/resources/application.yml`에서 설정

### Docker

`docker-compose.yml`의 환경 변수 섹션

### Kubernetes

`k8s/configmap.yaml` 및 `k8s/secret.yaml`

### 주요 환경 변수

| 변수          | 설명                  | 기본값      |
| ------------- | --------------------- | ----------- |
| `DB_HOST`     | 데이터베이스 호스트   | localhost   |
| `DB_PORT`     | 데이터베이스 포트     | 5432        |
| `DB_NAME`     | 데이터베이스 이름     | template_db |
| `DB_USER`     | 데이터베이스 사용자   | postgres    |
| `DB_PASSWORD` | 데이터베이스 비밀번호 | postgres    |

## 트러블슈팅

### 빌드 실패

```bash
# Gradle 캐시 삭제
./gradlew clean

# Gradle Wrapper 재설치
./gradlew wrapper
```

### Docker 컨테이너 시작 실패

```bash
# 로그 확인
docker-compose logs

# 컨테이너 재시작
docker-compose restart
```

### Kubernetes Pod 시작 실패

```bash
# Pod 상태 확인
kubectl describe pod <pod-name>

# 로그 확인
kubectl logs <pod-name>
```

## 라이선스

MIT License

## 문의

프로젝트 관련 문의사항은 팀 PM에게 연락하세요.
