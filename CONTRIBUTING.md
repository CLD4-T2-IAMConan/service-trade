# Contributing Guide

이 프로젝트에 기여해주셔서 감사합니다!

## 개발 환경 설정

1. 이 리포지토리를 fork 하세요
2. 로컬에 클론하세요
3. 새 브랜치를 생성하세요: `git checkout -b feature/my-feature`
4. 변경사항을 커밋하세요: `git commit -m 'feat: add some feature'`
5. 브랜치에 푸시하세요: `git push origin feature/my-feature`
6. Pull Request를 생성하세요

## 코드 스타일

- Java Code Convention 준수
- 들여쓰기: 4칸 (스페이스)
- 줄바꿈: Unix (LF)
- 인코딩: UTF-8

## 커밋 메시지 규칙

커밋 메시지는 다음 형식을 따릅니다:

```
<type>: <subject>

<body>
```

### Type
- `feat`: 새로운 기능 추가
- `fix`: 버그 수정
- `docs`: 문서 수정
- `style`: 코드 포맷팅, 세미콜론 누락 등
- `refactor`: 코드 리팩토링
- `test`: 테스트 코드 추가
- `chore`: 빌드 업무 수정, 패키지 매니저 수정 등

### 예시
```
feat: add user authentication

- Implement JWT token generation
- Add login/logout endpoints
- Create user repository
```

## Pull Request 규칙

1. PR 템플릿을 모두 작성해주세요
2. 관련된 이슈가 있다면 링크해주세요
3. 스크린샷이 필요한 경우 첨부해주세요
4. 최소 1명의 리뷰어를 지정해주세요
5. CI 테스트가 통과해야 합니다

## 테스트

새로운 기능을 추가할 때는 반드시 테스트 코드를 작성해주세요.

```bash
# 모든 테스트 실행
./gradlew test

# 특정 테스트 실행
./gradlew test --tests "com.company.template.*"
```

## 문의

궁금한 사항이 있으시면 이슈를 생성하거나 팀 PM에게 연락해주세요.
