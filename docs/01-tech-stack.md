# 01. 기술 스택

## 버전 고정표

| 구분 | 항목 | 버전 | 고정 위치 |
|---|---|---|---|
| 게임 | Minecraft / Paper | 26.1.2, stable 빌드 `N` | `infra/*/compose`의 `VERSION`, `PAPER_BUILD` |
| 런타임 | Java | Temurin 25 | Gradle toolchain, itzg 이미지 태그 |
| 빌드 | Gradle | 9.x (Java 25 지원 버전) | `plugins/gradle/wrapper/gradle-wrapper.properties` |
| API | paper-api | `26.1.2.build.N-stable` (서버 빌드와 동일) | `plugins/gradle/libs.versions.toml` |
| DB | PostgreSQL | 17 | compose 이미지 태그, Testcontainers 이미지 |
| 라이브러리 | pgJDBC, HikariCP, Flyway | 세팅 시점 최신 stable | `libs.versions.toml` |
| 테스트 | JUnit 5, AssertJ | MockBukkit과 호환되는 버전 | `libs.versions.toml` |
| | MockBukkit | `org.mockbukkit.mockbukkit:mockbukkit-v26.1.2` | `libs.versions.toml` |
| | Mockito | 5.x | `libs.versions.toml` |
| | Testcontainers | 세팅 시점 최신 stable | `libs.versions.toml` |
| | JaCoCo | Java 25 지원 버전 | `build.gradle.kts` |
| E2E | VitaminMCP | Phase 3 검증 후 고정 | `.mcp.json`, `infra/dev` 플러그인 |
| 인프라 | Terraform | OCI native backend 지원 버전 (1.12+) | `required_version` |
| | OCI provider | `oracle/oci` 최신 stable | `required_providers` |

> 모든 버전은 **명시적으로 고정**한다. `latest`, `+`, 범위 지정 금지.
> 버전 카탈로그는 `plugins/gradle/libs.versions.toml` 한 곳에서 관리한다.

## 라이브러리 사용 원칙

| 항목 | 원칙 |
|---|---|
| 텍스트 | Adventure Component 사용 (legacy `§` 색 코드 금지) |
| 명령어 | Paper Brigadier API (`LifecycleEvents.COMMANDS`) |
| DB | 메인 스레드에서 DB 호출 금지 → 비동기 실행 후 결과만 메인 스레드로 |
| 의존성 포함 | HikariCP·pgJDBC·Flyway는 Shadow로 포함하고 relocate |
| 설정 | `config.yml`은 기본값 포함, DB 접속 정보는 환경변수 우선 |

## 플러그인 프로젝트 구조

| 경로 | 내용 |
|---|---|
| `plugins/` | Gradle 루트 (`settings.gradle.kts`, `gradlew`) |
| `plugins/blockgate-core/` | 첫 번째 플러그인 모듈 (DB 풀, Flyway, 헬스 로그) |
| `plugins/blockgate-core/src/main/java/dev/blockgate/core/` | 기본 패키지 (`dev.blockgate`, 변경 가능) |
| `plugins/blockgate-core/src/test/java/` | 단위 테스트 (JUnit, MockBukkit) |
| `plugins/blockgate-core/src/integrationTest/java/` | 통합 테스트 (Testcontainers) |
| `plugins/blockgate-core/src/main/resources/db/migration/` | Flyway 마이그레이션 `V<n>__<desc>.sql` |
| `plugins/e2e/scenarios/` | VitaminMCP E2E 시나리오 `<이슈번호>-T<n>-<slug>.json`, 회귀용 `smoke-*.json` |

기능이 커지면 모듈을 추가한다(`plugins/blockgate-<기능>/`). 모듈 추가도 이슈로 진행한다.
