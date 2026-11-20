---
name: verify
description: TDD로 테스트 리스트를 모두 끝낸 뒤 E2E 인수 테스트와 전체 검증(단위·통합·커버리지·빌드)을 할 때 사용. "검증해줘", "E2E 돌려줘"에도 발동한다.
---

# 검증 (워크플로우 7~8단계)

## 1. 자동 테스트 (plugins/ 에서)

```bash
./gradlew clean check jacocoTestReport
```

| 확인 | 기준 |
|---|---|
| 단위 + 통합 | 전부 통과 |
| skip | 0 (있으면 빌드 실패 — MockBukkit 미구현 메서드면 테스트를 순수 로직으로 분리하거나 다른 방식으로) |
| 커버리지 | 변경한 클래스의 라인 ≥ 80%, 브랜치 ≥ 70% (JaCoCo HTML 리포트로 확인, CI가 변경분을 최종 판정) |

## 2. E2E 인수 테스트 (로컬 서버만)

1. `./gradlew deployDev` → 로그에서 플러그인 활성화 확인
2. VitaminMCP: `session_start` → 이 이슈의 `plugins/e2e/scenarios/<이슈번호>-*.json` 실행
3. 각 시나리오 **3회 연속 통과** 확인, `exceptions_recent`가 비어 있어야 함
4. `plugins/e2e/scenarios/smoke-*.json`(있으면) 1회 실행 — 기존 기능 회귀 확인
5. `session_reset`

> 운영 서버에는 절대 연결하지 않는다.

## 3. 에이전트 수동 탐색 (5~10분)
봇으로 AC 밖의 사용 방식을 시도한다: 이상한 입력, 연속 실행, 작업 중 접속 종료, 권한 없는 계정.
문제를 찾으면 설계 문서 테스트 리스트에 `T<n> (추가)`로 넣고 `tdd-cycle`로 돌아가 Red → Green 한다.

## 4. 영역별 추가 검증
| 영역 | 검증 |
|---|---|
| server | `docker compose -f infra/prod/server/compose.yaml config`, 로컬에서 같은 설정으로 기동 |
| infra | `terraform fmt -check -recursive`, `terraform validate`, `terraform plan` + 무료 한도 가드 (**apply 금지**) |

## 5. 결과 기록
`.claude/state/verify-<브랜치(/→_)>.md`에 표로 저장한다 (PR 본문 "검증 결과"에 사용).

| 항목 | 결과 |
|---|---|
| 단위 / 통합 | 통과 N / skip 0 |
| 커버리지 | 라인 %, 브랜치 % |
| E2E | 시나리오별 3/3 |
| 수동 탐색 | 발견 사항 / 없음 |

모두 통과하면 `review` 스킬로 넘어간다.
