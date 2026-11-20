# #8 blockgate-core DB 기반 — 설계 문서

| 항목 | 값 |
|---|---|
| 이슈 | #8 |
| 영역 | plugin |
| 상태 | **승인** (2026-11-20, 개발자 채팅 승인 — 이슈 #8 코멘트) |
| 선택된 방안 | 인프라 Phase라 G1 없음 — `docs/09-setup-roadmap.md` Phase 4 5~7번 |

## 1. 배경과 목적

이후 모든 기능이 쓸 DB 기반을 `blockgate-core`에 만든다. 운영 CD의 헬스 체크(`docs/06-ci-cd.md`)가 이 플러그인의 활성화·DB 연결 로그를 본다.

## 2. 범위

| 포함 | 제외 |
|---|---|
| DB 설정(config.yml + 환경변수 우선), HikariCP 풀, Flyway `V1__baseline.sql`, 비동기 실행기, 활성화·DB 결과 로그, 종료 처리 | 게임 기능, 명령어, 다른 모듈, 재연결·재시도 정책 |

## 3. 완료 조건 (AC)

| AC | WHEN (상황·행동) | THEN (기대 결과) |
|---|---|---|
| AC-1 | DB 접속 정보를 읽을 때 | 환경변수 `BLOCKGATE_DB_URL` / `BLOCKGATE_DB_USER` / `BLOCKGATE_DB_PASSWORD`가 비어 있지 않으면 그 값, 아니면 `config.yml`의 `database.url` / `database.user` / `database.password`를 쓴다. 풀 크기는 `database.pool-size`(기본 **4**) |
| AC-2 | URL이 어디에도 없거나 풀 크기가 1 미만일 때 | `IllegalStateException` — 메시지 `DB URL이 설정되지 않았습니다 (BLOCKGATE_DB_URL 또는 config.yml database.url)` / `database.pool-size는 1 이상이어야 합니다 (현재: <값>)` |
| AC-3 | 설정으로 풀을 만들 때 | HikariCP 풀 이름 `BlockGateCore-DB`, 최대 크기 = 풀 크기, PostgreSQL에 연결되어 `SELECT 1`이 1 |
| AC-4 | 마이그레이션을 실행할 때 | 플러그인 안의 `db/migration`을 적용. 빈 DB → 스키마 버전 `1`, 테이블 `bg_meta(key text PK, value text NOT NULL)` 존재. 다시 실행하면 적용 0건 |
| AC-5 | DB 작업을 맡길 때 | 전용 실행기의 스레드(이름 `BlockGateCore-DB-<n>`)에서 실행되고 `CompletableFuture`로 결과를 돌려준다. 작업 예외는 같은 원인을 가진 실패 future가 된다. 닫힌 뒤 맡기면 `IllegalStateException`으로 실패한 future |
| AC-6 | 플러그인이 활성화될 때 | `onEnable`은 DB를 기다리지 않고 바로 끝난다. 비동기 초기화(풀 → 마이그레이션) 성공 시 INFO `DB 연결 성공 (스키마 버전 1)`. 실패 시 SEVERE `DB 연결 실패: <원인 메시지>` 후 메인 스레드에서 플러그인 비활성화 |
| AC-7 | 플러그인이 비활성화될 때 | 풀을 닫고 실행기를 종료한다 |
| AC-8 | 로컬 서버에 `deployDev` 했을 때 | 서버 로그에 `[BlockGateCore] Enabling BlockGateCore v0.1.0`과 `DB 연결 성공 (스키마 버전 1)`, 서버 예외 0건 |

## 4. 테스트 리스트

| T | 시나리오 | AC | 종류 | 레벨 | 완료 |
|---|---|---|---|---|---|
| T1 | 환경변수가 없으면 config.yml 값(url·user·password·pool-size)을 쓴다 | AC-1 | 정상 | 단위(JUnit) | ☑ |
| T2 | 환경변수가 있으면 config.yml보다 우선한다 (빈 문자열 환경변수는 무시) | AC-1 | 정상 | 단위(JUnit) | ☑ |
| T3 | pool-size 미설정 → 4 | AC-1 | 경계 | 단위(JUnit) | ☑ |
| T4 | URL이 어디에도 없으면 AC-2 메시지로 실패 | AC-2 | 실패·예외 | 단위(JUnit) | ☑ |
| T5 | pool-size 0 → AC-2 메시지로 실패 (1은 허용) | AC-2 | 경계 | 단위(JUnit) | ☑ |
| T6 | 풀 이름·최대 크기, PostgreSQL 17에 `SELECT 1` = 1 | AC-3 | 정상 | 통합(Testcontainers) | ☑ |
| T7 | 빈 DB 마이그레이션 → 버전 1, `bg_meta` 존재 | AC-4 | 정상 | 통합(Testcontainers) | ☑ |
| T8 | 마이그레이션 두 번째 실행 → 적용 0건 (재시작 후 유지) | AC-4 | MC 특수 | 통합(Testcontainers) | ☑ |
| T9 | 맡긴 작업이 `BlockGateCore-DB-` 스레드에서 실행되고 결과를 돌려준다 | AC-5 | 정상 | 단위(JUnit) | ☑ |
| T10 | 작업 예외 → 같은 원인의 실패 future | AC-5 | 실패·예외 | 단위(JUnit) | ☑ |
| T11 | 닫힌 실행기에 맡기면 `IllegalStateException` 실패 future | AC-5 | 경계 | 단위(JUnit) | ☑ |
| T12 | 연결할 수 없는 DB 설정으로 활성화 → `onEnable` 예외 없음, SEVERE `DB 연결 실패:` 로그, 플러그인 비활성화 | AC-6 | 실패·예외 | 단위(MockBukkit) | ☑ |
| T13 | 실제 DB로 활성화 → INFO `DB 연결 성공 (스키마 버전 1)`, 플러그인 활성 유지 | AC-6 | 정상 | 통합(MockBukkit + Testcontainers) | ☑ |
| T14 | 비활성화 → 풀 닫힘 | AC-7 | 정상 | 통합(MockBukkit + Testcontainers) | ☑ |
| T15 | `deployDev` → 서버 로그 2줄 확인, `exceptions_recent` 0건 (3회 연속) | AC-8 | 정상 | E2E(VitaminMCP, 로그 확인) | ☐ |
| T16 (추가) | 배포 JAR(shadowJar)의 Flyway 플러그인 서비스 목록이 flyway-core·flyway-database-postgresql 항목을 relocate된 이름으로 모두 포함 | AC-8 | 실패·예외 | 통합(빌드 산출물) | ☑ |
| T17 (추가) | 작업이 `Error`(예: `LinkageError`)를 던져도 같은 원인의 실패 future가 된다 | AC-5 | 실패·예외 | 단위(JUnit) | ☑ |
| T18 (추가) | 초기화 도중(풀 생성 직후, 마이그레이션 전) 비활성화 → 비활성화가 끝나면 그 풀은 닫혀 있고, 실행기는 종료되고, `dataSource()`는 null | AC-7 | MC 특수 | 통합(MockBukkit + Testcontainers) | ☑ |
| T19 (추가) | URL 미설정으로 활성화 → SEVERE `DB 연결 실패: DB URL이 설정되지 않았습니다 (…)`, 플러그인 비활성화. 설정 해석은 메인 스레드(onEnable)에서 하므로 DB 실행기를 만들지 않는다 | AC-2, AC-6 | 실패·예외 | 단위(MockBukkit) | ☑ |
| T20 (추가) | `DatabaseSettings.toString()`에 비밀번호가 드러나지 않는다 (`***`) | AC-1 | 보안 | 단위(JUnit) | ☐ |

> T16은 T15 첫 실행에서 발견: Shadow 9의 기본 `duplicatesStrategy = EXCLUDE` 때문에 서비스 파일이 병합되지 않아, 실서버에서 Flyway가 `NullPointerException`(PluginRegister)으로 실패했다. 서버 없이 CI에서 잡도록 추가.
> T17~T20은 리뷰(quality)에서 발견: `Error`가 나면 future가 영원히 미완료, 초기화 중 비활성화 시 풀 누수, DB 스레드에서 `getConfig()` 호출, record `toString()`의 비밀번호 노출. AC는 그대로이고 기존 AC를 더 엄밀하게 검증하는 항목이다.
> T15는 봇 시나리오가 아니라 서버 로그와 `exceptions_recent`로 확인하는 항목이다 (플러그인에 플레이어가 겪는 흐름이 아직 없음). 3회 실행 증거는 `.claude/state/verify-*.md`와 PR "검증 결과"에 남긴다.
> T4·T2의 "비어 있음"은 공백만 있는 값도 포함한다 (`isBlank`).

## 5. 설계 메모

| 항목 | 내용 |
|---|---|
| 구조 | `DatabaseSettings`(순수, record) · `DataSourceFactory` · `Migrator` · `DbExecutor` · `BlockGateCore`(JavaPlugin). 설정 해석은 `Map<String,String>` 환경변수와 `ConfigurationSection`을 받아 테스트 가능하게 |
| 명령어·권한 | 없음 |
| 데이터·DB | Flyway `V1__baseline.sql`: `bg_meta` 테이블. Flyway는 플러그인 클래스로더로 설정 (`Flyway.configure(classLoader)`) |
| 설정 | `config.yml`: `database.url`(기본 빈 값), `database.user`(`blockgate`), `database.password`(빈 값), `database.pool-size`(4). 로컬·운영 compose가 `BLOCKGATE_DB_*`를 주입 |
| 로그 | 위 AC-6 문구. 비밀번호는 로그에 남기지 않는다 |

## 6. 리스크·고려사항

| 리스크 | 대응 |
|---|---|
| Shadow relocate 뒤 Flyway·pgJDBC의 ServiceLoader가 깨질 수 있음 | 실제로 깨졌음(T15 첫 실행) → 서비스 파일만 `duplicatesStrategy = INCLUDE` + `mergeServiceFiles()`, T16이 CI에서 검사 |
| 초기화 중 비활성화(서버 종료 등) | 비활성화는 실행기 종료를 제한 시간(10초) 기다린 뒤 풀을 닫는다. 초기화는 종료 중이면 만든 풀을 닫고 중단 (T18) |
| T12 단위 테스트가 1초를 넘음 (약 3.5초) | 대부분 MockBukkit 첫 기동 비용(같은 기동 뒤 T19는 빠름). 거부된 TCP 연결은 즉시 실패하므로 허용 |
| MockBukkit이 비동기·스케줄러를 다르게 다룸 | 초기화 future를 테스트용으로 노출하고, 메인 스레드 작업은 `performTicks`로 진행 |

## 7. 플레이테스트 포인트 (G3)

해당 없음 (인프라 Phase).
