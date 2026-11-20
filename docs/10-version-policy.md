# 10. 버전 정책

## 현재 고정 버전

| 항목 | 버전 | 선택 이유 |
|---|---|---|
| Minecraft / Paper | 26.1.2 | VitaminMCP·MockBukkit이 공식 지원하는 최신 버전 |
| Java | Temurin 25 | Paper 26.1+ 요구 |
| MockBukkit | `mockbukkit-v26.1.2` | 서버 버전과 일치 |

## 업그레이드 조건 (모두 충족해야 함)

| # | 조건 |
|---|---|
| 1 | VitaminMCP가 대상 Minecraft 버전을 **공식 지원(🟢)** 으로 표시 |
| 2 | MockBukkit에 대상 버전 아티팩트가 있고 해당 Paper API 기준으로 릴리스됨 |
| 3 | 사용 중인 라이브러리·플러그인이 대상 버전을 지원 |
| 4 | 개발자들이 업그레이드에 동의 (클라이언트도 같이 올려야 함) |

## 업그레이드 절차

| # | 단계 |
|---|---|
| 1 | `chore(server)` 이슈 생성, 영향 범위(API 변경·폐기 목록) 조사 |
| 2 | 브랜치에서 `libs.versions.toml`, compose `VERSION`/`PAPER_BUILD`, MockBukkit 아티팩트 동시 변경 |
| 3 | 로컬 `infra/dev`에서 `./gradlew check` + E2E 전체 통과 |
| 4 | **운영 월드 백업** (월드는 낮은 버전으로 되돌릴 수 없음) |
| 5 | PR → 머지 → server-cd / plugin-cd 배포 → 개발자 접속 확인(G3) |

## 일반 의존성

| 대상 | 정책 |
|---|---|
| 라이브러리 | 고정 버전. 보안 이슈가 있으면 `chore(plugin)` 이슈로 올림 |
| Paper 빌드 번호 | 같은 26.1.2 안의 새 stable 빌드는 `chore(server)` 이슈로 반영 가능 (paper-api 빌드도 함께) |
| GitHub Actions | 메이저 버전 태그 고정 (예: `actions/checkout@v5`) |
| VitaminMCP | npm `vitaminmcp@X`(`.mcp.json`)와 플러그인 `VitaminMCP.jar` X를 **같은 버전으로 함께** 올린다. `docs/01`의 버전·SHA-256과 local-dev 스킬의 받기 명령을 갱신하고, smoke 시나리오 3회 연속 통과를 확인한다 (`chore(repo)` 이슈) |
