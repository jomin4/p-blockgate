# 04. TDD · 테스트 규칙

## 1. 역할 구분

| 구분 | 담당 | 목적 |
|---|---|---|
| 단위·통합·E2E 테스트 | Claude Code | 완료 조건(AC) 충족 **검증** |
| 인게임 플레이 | 개발자 | 재미·사용감 **피드백** (검증 아님) |

## 2. Canon TDD 사이클

| # | 단계 | 규칙 |
|---|---|---|
| 1 | 테스트 리스트 | 설계 문서의 테스트 리스트(G2 승인본)를 사용. 진행 중 새 케이스가 떠오르면 리스트에 추가하고 PR에 표시 |
| 2 | Red | 리스트에서 **딱 하나**를 실행 가능한 테스트로 작성 → 실행 → **실패 확인**. 실패 이유가 예상과 같은지도 확인 (컴파일 실패도 Red) |
| 3 | Green | 그 테스트와 이전 테스트가 모두 통과하는 **최소 구현** |
| 4 | Refactor | Green 상태에서만. 동작을 바꾸지 않음. 끝나면 전체 테스트 재실행 |
| 5 | 반복 | 리스트가 빌 때까지 2로 |

### 커밋 규칙

| 시점 | 커밋 | 예 |
|---|---|---|
| Red 확인 후 | `test(<영역>): <시나리오> 실패 테스트` | `test(plugin): T3 집 미설정 시 안내 메시지` |
| Green 후 | `feat(<영역>): ...` 또는 `fix(<영역>): ...` | `feat(plugin): 집 미설정 처리` |
| Refactor 후 (있으면) | `refactor(<영역>): ...` | `refactor(plugin): HomeService 분리` |

> PR 생성 훅이 브랜치에 `test(` 커밋이 `feat(`/`fix(` 커밋보다 먼저 있는지 검사한다.

## 3. 테스트 레벨 선택

| 질문 (위에서부터) | 예 → 레벨 | 도구 | 위치 |
|---|---|---|---|
| 인게임에서 플레이어가 보고 겪는 흐름인가? | E2E 인수 | VitaminMCP | 로컬 서버, 결과를 PR에 첨부 |
| DB를 거치는가? | 통합 | JUnit + Testcontainers(PostgreSQL 17) | `src/integrationTest` |
| Bukkit/Paper API에 의존하는가? | 단위 | JUnit + MockBukkit | `src/test` |
| 순수 로직인가? | 단위 | JUnit + AssertJ | `src/test` |

- 하나의 AC가 여러 레벨에 걸치면 **가장 낮은 레벨에서 최대한 검증**하고, E2E는 핵심 흐름만.
- 로직을 Bukkit 의존 코드와 분리하면 순수 단위 테스트가 늘어나 빨라진다 (권장 구조).

## 4. 필수 케이스

| 종류 | 예 |
|---|---|
| 정상 경로 | 명령 성공, 보상 지급 |
| 실패·예외 | 잘못된 입력, 대상 없음, DB 실패 |
| 경계값 | 0, 최대치, 최대치+1, 빈 목록 |
| 권한 | 권한 없음, OP |
| MC 특수 | 작업 중 접속 종료, 인벤토리 가득 참, 서버 재시작 후 유지, 다른 월드 |

## 5. 품질 기준

| 항목 | 기준 |
|---|---|
| 커버리지 | 변경 코드 라인 ≥ 80%, 브랜치 ≥ 70% (JaCoCo, CI에서 변경분 측정) |
| 속도 | 단위 테스트 1개 < 1초, E2E 시나리오 1개 < 60초 |
| 안정성 | E2E는 3회 연속 통과, `Thread.sleep` 금지 (VitaminMCP `wait_for` 사용) |
| 의미 | assertion이 실제 동작을 검증 (호출 여부만 확인하는 테스트 지양) |
| Mock 범위 | 내가 소유하지 않은 외부 경계만 mock. 테스트 대상 자체를 mock하지 않음 |
| **skip** | **skip된 테스트가 1개라도 있으면 빌드 실패** |

## 6. 금지 사항

| 금지 | 대신 |
|---|---|
| assertion 약화·삭제 | 구현을 고친다 |
| `@Disabled`, `assumeTrue`로 회피 | 원인 해결 또는 보고 |
| 구현 결과에 맞춰 기대값 수정 | 기대값은 설계 문서 기준. 설계가 틀렸으면 멈추고 G2 재승인 |
| 통과시키려고 기존(머지된) 테스트 수정 | 수정이 필요하면 사유를 PR에 명시하고 리뷰어가 검증 |
| 예외 삼키기 (`catch {}`) | 로그 + 처리 |
| 테스트 없이 프로덕션 코드 추가 | 먼저 Red |

## 7. 추적

| 대상 | 규칙 |
|---|---|
| 테스트 이름 | `@DisplayName("T3 / AC-2: 집 미설정 상태에서 /home 시 안내 메시지")` |
| 태그 | `@Tag("unit")`, `@Tag("integration")` |
| PR | 테스트 리스트 체크박스 T1~Tn 전부 체크 |

## 8. Gradle 설정 명세 (Phase 4에서 구현)

| 항목 | 구현 |
|---|---|
| 소스셋 | `test`(단위) + `integrationTest`(통합, `check`에 포함) |
| JUnit | `useJUnitPlatform()` |
| skip = 실패 | `afterSuite`에서 루트 suite의 `skippedTestCount > 0`이면 `GradleException` |
| Mockito agent | 아래 스니펫 |
| JaCoCo | `jacocoTestReport`(XML+HTML), test + integrationTest 실행 데이터 병합 |
| 개발 서버 반영 | `deployDev` 태스크: shadowJar → `infra/dev/data/plugins/` → `docker compose restart mc` |

```kotlin
// Mockito inline mock maker를 Java 21+에서 경고/실패 없이 쓰기 위한 agent 설정
val mockitoAgent = configurations.create("mockitoAgent")
dependencies {
    mockitoAgent(libs.mockito.core) { isTransitive = false }
}
tasks.withType<Test>().configureEach {
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
}
```

```kotlin
// skip된 테스트가 있으면 실패
tasks.withType<Test>().configureEach {
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun beforeTest(testDescriptor: TestDescriptor) {}
        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null && result.skippedTestCount > 0) {
                throw GradleException("${result.skippedTestCount}개 테스트가 skip됨 — skip은 실패로 간주 (MockBukkit 미구현 메서드 확인)")
            }
        }
    })
}
```

## 9. E2E (VitaminMCP) 사용 규칙

| 규칙 | 내용 |
|---|---|
| 대상 | **로컬 `blockgate-dev` 서버만**. 운영 서버 금지 |
| 순서 | `deployDev` → `session_start` → `bot_spawn` → 시나리오(`bot_run_scenario` 또는 단계별 도구) → `assert_*` → `exceptions_recent` 확인 → `session_reset` |
| 기다림 | `wait_for`만 사용 (sleep 금지) |
| 판정 | 서버 예외(`exceptions_recent`)가 하나라도 있으면 실패 |
| 저장 | 시나리오는 `plugins/e2e/scenarios/<이슈번호>-T<n>-<slug>.json`에 커밋 (Red 커밋에 포함) → 이후 회귀 테스트로 재사용 |
| 기록 | 시나리오 이름, 단계, 결과를 PR "검증 결과"에 붙인다 |
| 수동 탐색 | AC 외에 봇으로 자유롭게 써보고, 문제를 찾으면 Red 테스트로 만든 뒤 고친다 |
