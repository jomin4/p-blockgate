# plugins — Paper 플러그인 (Gradle 루트)

Phase 4에서 Claude Code가 작성한다. 명세는 `docs/01-tech-stack.md`, `docs/04-tdd-testing.md` 8장.

| 경로 | 내용 |
|---|---|
| `settings.gradle.kts`, `gradlew`, `gradle/` | Gradle 루트, Wrapper, `libs.versions.toml`(모든 버전 고정) |
| `blockgate-core/` | 첫 모듈: DB 풀(HikariCP), Flyway, 비동기 실행기, 활성화·헬스 로그 |
| `blockgate-core/src/test/` | 단위 테스트 (JUnit 5, AssertJ, MockBukkit v26.1.2, Mockito) |
| `blockgate-core/src/integrationTest/` | 통합 테스트 (Testcontainers PostgreSQL 17) |
| `e2e/scenarios/` | VitaminMCP 시나리오 (로컬 전용) |

## 자주 쓰는 명령
| 명령 | 용도 |
|---|---|
| `./gradlew check` | 단위 + 통합 테스트 (skip이 있으면 실패) |
| `./gradlew jacocoTestReport` | 커버리지 리포트 |
| `./gradlew shadowJar` | 배포용 JAR |
| `./gradlew deployDev` | 로컬 개발 서버에 반영 + 재시작 |
