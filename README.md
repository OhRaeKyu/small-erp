# small-erp (나의 작은 ERP)

직접 만들어 파는 소상공인(케이크·빵·수제 식품·공예품 등)을 위한 웹 ERP.
주문 → 생산 → 원재료 재고 → 발주를 연결하고, 인스타그램·카카오 문의를 한 곳에서 처리하는 것이 목표입니다.

## 구성

| 폴더 | 설명 | 기술 | 로컬 포트 |
|---|---|---|---|
| `backend/` | API 서버 1개. `/api/app/**` 사용자 서비스, `/api/admin/**` 관리자 서비스, `/api/public/**` 비로그인 | Java 21, Spring Boot 3.5, JPA, Spring Security (JWT), Flyway | 8080 |
| `app-web/` | 사용자 서비스 (사업자) | Next.js 16, TypeScript, Tailwind CSS 4 | 3000 |
| `admin-web/` | 관리자 서비스 (플랫폼 운영자) | Next.js 16, TypeScript, Tailwind CSS 4 | 3001 |
| `infra/` | 로컬 DB 초기화 스크립트 | PostgreSQL 16 | 5432 |

프론트는 브라우저에서 같은 도메인의 `/api/*`만 호출하고, Next 서버가 백엔드로 넘깁니다(rewrites).
로그인하면 액세스 토큰(30분)과 리프레시 토큰(14일)을 httpOnly 쿠키로 받습니다
(`APP_TOKEN`/`APP_REFRESH`, `ADMIN_TOKEN`/`ADMIN_REFRESH`). 액세스 토큰이 만료되면 프론트가 `/auth/refresh`로 자동 갱신합니다.
토큰의 `aud`가 서비스별로 달라 사업자 토큰으로 관리자 API를, 관리자 토큰으로 사용자 API를 호출할 수 없습니다.

## 로컬 실행

```bash
# 1) DB
docker compose up -d postgres

# 2) 백엔드 (local 프로필: 관리자·샘플 신청 데이터 자동 생성)
cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'

# 3) 프론트
cd app-web && npm install && npm run dev      # http://localhost:3000
cd admin-web && npm install && npm run dev    # http://localhost:3001
```

전부 컨테이너로 띄우려면 `docker compose up --build`.

### 샘플 계정 (local 프로필)

| 서비스 | 아이디 | 비밀번호 | 비고 |
|---|---|---|---|
| 관리자 | `admin@smallerp.local` | `admin1234!` | |
| 사용자 | `123-45-67891` | `test1234!` | 승인된 사업장 "달콤케이크 공방" |
| 사용자 | `214-87-00124` | `test1234!` | 회원가입 승인 대기 → 로그인 시 안내 문구 |

### 메일

SMTP 주소(`spring.mail.host`)가 없으면 메일을 보내지 않고 백엔드 로그에 내용만 남깁니다. 로컬 개발은 이 상태로 충분합니다.
Gmail로 실제 발송하려면 Google 계정에서 2단계 인증을 켜고 앱 비밀번호(16자리)를 만든 뒤 아래 중 하나로 넣습니다.
포트 587과 STARTTLS는 `application.yml`에 기본값으로 들어 있습니다.

- 로컬: `backend/config/application-local.yml` (git에 올라가지 않음, local 프로필일 때 Spring이 자동으로 읽음)

  ```yaml
  spring:
    mail:
      host: smtp.gmail.com
      username: 보내는주소@gmail.com
      password: 앱비밀번호16자리
  app:
    mail:
      from: 보내는주소@gmail.com
  ```

- 서버: 환경변수 `SPRING_MAIL_HOST=smtp.gmail.com`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `MAIL_FROM`

## 테스트

```bash
cd backend && ./gradlew test   # localhost:5432/erp_test 필요 (docker compose의 postgres가 만들어 줌)
cd app-web && npm run lint && npm run build
```

## API (1단계 일부)

전체 명세는 백엔드 실행 후 Swagger UI에서 볼 수 있습니다: http://localhost:8080/swagger-ui.html

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/public/industry-packs` | 회원가입 화면의 업종 목록 |
| POST | `/api/public/signup-requests` | 회원가입 요청 (사업자번호 검증번호 확인, 중복 요청 차단). JSON, 또는 등록증 첨부 시 multipart(`request` JSON + `certificate` 파일: jpg·png·pdf, 5MB 이하) |
| POST | `/api/public/signup-requests/status` | 사업자번호 + 비밀번호로 최신 요청 상태 조회 (틀리면 404, 존재 여부 비노출) |
| POST | `/api/app/auth/login` | 사용자 로그인. 아이디 = 사업자번호(하이픈 무관). 사업장당 계정 1개. 5번 연속 틀리면 10분 잠금(429 LOGIN_LOCKED, 관리자 로그인도 동일) |
| POST | `/api/app/auth/refresh` | 리프레시 쿠키로 토큰 갱신 (계정 잠금·사업장 정지 시 거부) |
| GET | `/api/app/auth/me` | 로그인 사용자·사업장·역할 |
| POST | `/api/app/auth/logout` | 로그아웃 |
| GET·PUT | `/api/app/tenant` | 설정 > 사업장 정보 (상호·주소·연락처·요일별 영업시간·기본 하루 생산 가능량·주문 마감 N일). 사업장은 토큰에서만 정함 |
| POST | `/api/admin/auth/login` | 관리자 로그인 |
| POST | `/api/admin/auth/refresh` | 리프레시 쿠키로 토큰 갱신 |
| GET | `/api/admin/auth/me` | 로그인 관리자 |
| POST | `/api/admin/auth/logout` | 로그아웃 |
| GET | `/api/admin/signup-requests` | 회원가입 요청 목록. `status`, `q`(사업자번호·상호·대표자), `sort`(OLDEST 기본/LATEST), `page`, `size` |
| GET | `/api/admin/signup-requests/counts` | 상태별 건수 |
| GET | `/api/admin/signup-requests/{id}` | 상세 (같은 사업자번호의 다른 요청, 작업 이력 포함) |
| GET | `/api/admin/signup-requests/{id}/certificate` | 사업자등록증 보기 (관리자만, 서버 비공개 디스크 `STORAGE_DIR`에 저장) |
| POST | `/api/admin/signup-requests/{id}/approve` | 승인: 사업장·계정·소속 생성, 업종 팩 적용, 작업 이력, 안내 메일 (한 트랜잭션, 메일은 커밋 후 비동기) |
| POST | `/api/admin/signup-requests/{id}/reject` | 반려 (사유 5자 이상), 작업 이력, 안내 메일 |

## 브랜치 전략

| 브랜치 | 용도 |
|---|---|
| `main` | 테스트까지 끝난 소스만. dev에서 CI가 통과한 뒤 합친다 |
| `dev` | 개발 통합 브랜치 |
| `feature/<기능>` | dev에서 따서 기능별로 개발하고 dev에 합친다 |

혼자 개발하므로 PR 없이 직접 합칩니다. CI(GitHub Actions)는 세 종류 브랜치 모두에 push할 때 돕니다.

## 설계 문서

요구사항·화면·ERD는 프로젝트 공유 폴더의 `planning/`, `design/` 문서를 따릅니다.
