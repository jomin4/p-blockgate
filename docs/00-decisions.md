# 00. 결정 사항 최종본

> 기초 인프라 세팅 기준의 결정이다. 기능이 추가·변경되면 도구는 유연하게 추가·변경한다.
> 바꿀 때는 이 문서와 관련 문서를 같은 PR에서 갱신한다.

## 1. 프로젝트 개요

| 항목 | 내용 |
|---|---|
| 프로젝트명 | BlockGate |
| 목적 | 친구들이 Claude Code로 함께 개발하면서 같은 서버에서 게임 |
| 현재 범위 | 기초 인프라·개발 환경·협업 하네스 세팅 (게임 주제는 이후 결정) |
| 개발자 역할 | 기능 제안, 피드백, 검수, 인게임 플레이 |
| Claude Code 역할 | 조사, 이슈, 설계, 구현, 테스트, 리뷰, PR, 머지, 배포 확인, 정리 |

## 2. 기술 스택

| 구분 | 항목 | 버전·선택 |
|---|---|---|
| 게임 | Minecraft Java Edition | **26.1.2** (클라이언트도 26.1.2) |
| | 서버 | Paper 26.1.2, stable 빌드 번호 고정 |
| 언어·빌드 | Java | Temurin 25 |
| | 빌드 | Gradle Kotlin DSL + Wrapper(9.x) + Shadow |
| | API | paper-api `26.1.2.build.N-stable`(서버와 같은 빌드), Adventure, Brigadier |
| DB | DB | PostgreSQL 17 |
| | 연결·마이그레이션 | pgJDBC + HikariCP(shaded) + Flyway, DB 작업은 모두 비동기 |
| 테스트 | 단위(순수 로직) | JUnit 5 + AssertJ |
| | 단위(Bukkit API) | MockBukkit `mockbukkit-v26.1.2` |
| | 대체 객체 | Mockito 5 (`-javaagent` 설정) |
| | 통합(DB) | Testcontainers + PostgreSQL |
| | E2E 인수 | VitaminMCP (봇 클라이언트, 로컬 전용) |
| | 커버리지 | JaCoCo (+ CI에서 변경분 커버리지) |
| | 이후 | PIT mutation testing |

## 3. 운영 인프라

| 항목 | 내용 |
|---|---|
| 클라우드 | Oracle Cloud Always Free, 홈 리전 Japan Central (Osaka, `ap-osaka-1`) |
| VM | Ampere A1 1대, 2 OCPU / 12GB, Ubuntu 24.04 ARM64 |
| 스토리지 | 부트 50GB + 블록 100GB (무료 한도 200GB 이내), Object Storage 20GB 이내 |
| 구성 | 단일 서버(개발/운영 분리 없음), Docker Compose |
| 컨테이너 | itzg/minecraft-server(Paper 26.1.2), PostgreSQL(외부 비공개) |
| 네트워크 | public subnet, 외부 공개는 TCP 25565만 |
| 접속 보안 | online-mode + whitelist |
| 관리 접속 | Tailscale (SSH, 배포). RCON은 컨테이너 내부 `rcon-cli`로만 사용 |
| IaC | Terraform `oracle/oci` provider + OCI native backend(state locking) |
| 초기화 | cloud-init (Docker, Tailscale, iptables, 블록 볼륨 마운트) |
| 보호 | VM·볼륨 `prevent_destroy`, CI 무료 한도 가드 |
| 백업·롤백 | 이후 단계 |

## 4. 개발 환경 (각 PC)

| 항목 | 내용 |
|---|---|
| 로컬 서버 | `infra/dev` Docker Compose: Paper 26.1.2 + PostgreSQL + VitaminMCP 플러그인 |
| DB 접근 | `docker compose exec postgres psql` (Postgres MCP 없음) |
| 필수 설치 | Git, gh, JDK 25, Docker, Node.js, Claude Code (인프라 담당은 Terraform, OCI CLI 추가) |
| 공유 설정 | CLAUDE.md, `.claude/`, `.githooks/`, `.mcp.json` (레포 포함) |
| 개인 설정 | `.env`, `.claude/settings.local.json`, `*.tfvars` (커밋 금지) |

## 5. 방법론

| 층 | 채택 |
|---|---|
| 프로세스 | Kanban |
| 브랜치·배포 | GitHub Flow + Continuous Deployment |
| 개발 기법 | TDD (Kent Beck Canon TDD) |

## 6. 워크플로우 / 게이트

| 게이트 | 시점 | 의미 |
|---|---|---|
| G1 | 조사 후 | 구현 방안 선택 |
| G2 | 설계 문서 작성 후 | AC·기대값·테스트 리스트 승인 |
| G3 | 배포 후 플레이 | 재미·사용감 승인 (검증은 Claude 테스트가 담당) |

## 7. 남은 확인 사항

| 항목 | 상태 |
|---|---|
| Oracle 가입 | ✅ 완료 (OCID 등은 세팅 시 Claude Code에 제공) |
| Osaka A1 용량 | 인스턴스 생성 시 확인 (실패 시 재시도) |
| Paper 26.1.2 stable 빌드 번호 | Phase 2에서 최신 stable로 고정 |
| VitaminMCP 26.1.2 실동작 | Phase 3에서 확인 후 버전 고정 |
