---
name: tdd-cycle
description: G2 승인 후 브랜치를 만들고 설계 문서의 테스트 리스트를 Canon TDD(Red → Green → Refactor)로 하나씩 구현할 때 사용. "구현 시작해", "다음 테스트 진행해"에도 발동한다. 버그 수정의 재현 테스트도 이 규칙을 따른다.
---

# TDD 구현 (워크플로우 5~6단계)

## 시작

```bash
git switch main && git pull
git switch -c feat/<이슈번호>-<slug>          # 버그는 fix/, 리팩터링은 refactor/
node scripts/kanban.mjs move <이슈번호> "In Progress"
git add docs/plans/<이슈번호>-<slug>.md
git commit -m "docs(docs): #<이슈번호> <기능> 설계 문서"
```

## 사이클 (테스트 리스트가 빌 때까지 반복)

| 단계 | 할 일 | 확인 |
|---|---|---|
| 1. 고르기 | 리스트에서 다음 T 항목 **하나** | |
| 2. Red | 테스트 작성 (`@DisplayName("T3 / AC-2: ...")`, `@Tag`) → 해당 테스트만 실행 | **실패를 직접 확인**, 실패 이유가 예상과 같은지 확인 (컴파일 실패도 Red) |
| 3. 커밋 | `test(<영역>): T3 <시나리오> 실패 테스트` | |
| 4. Green | 통과에 필요한 **최소 구현** → 모듈 전체 테스트 실행 | 새 테스트 + 기존 테스트 모두 통과, skip 0 |
| 5. 커밋 | `feat(<영역>): <동작>` (버그는 `fix`) + 설계 문서 체크박스 ☐→☑ | |
| 6. Refactor | 필요할 때만, Green 상태에서. 끝나면 전체 테스트 | 동작 변화 없음 |
| 7. 커밋 | `refactor(<영역>): <내용>` (있을 때만) | |

### 실행 명령 (plugins/ 에서)

```bash
./gradlew :blockgate-core:test --tests "dev.blockgate.core.HomeServiceTest"   # Red/Green 확인
./gradlew :blockgate-core:test                                               # 모듈 단위 전체
./gradlew :blockgate-core:integrationTest                                    # 통합(Testcontainers)
```

### E2E 레벨 항목
- 시나리오를 `plugins/e2e/scenarios/<이슈번호>-T<n>-<slug>.json`에 저장한다 (VitaminMCP `bot_run_scenario`에 넘기는 입력 그대로).
- Red: `./gradlew deployDev` 후 시나리오 실행 → 실패 확인 → `test(...)` 커밋.
- Green: 구현 → `deployDev` → 시나리오 통과 확인 → `feat(...)` 커밋.

## 규칙
- 실패하는 테스트 없이 프로덕션 코드를 쓰지 않는다. 한 번에 테스트 하나.
- 기대값은 설계 문서 기준. 구현 결과에 맞춰 바꾸지 않는다.
- 금지: assertion 약화, `@Disabled`, assume 회피, 빈 catch, 통과시키려고 기존 테스트 수정.
- 진행 중 새 케이스를 발견하면 설계 문서 리스트에 `T<n> (추가)`로 넣고 같은 사이클로 처리한다.
- **멈추고 보고**: 설계가 틀렸다고 판단될 때(G2 재승인), 같은 테스트에서 3번 연속 Green 실패, 범위가 크게 늘어날 때.
- main이 앞서 나가면 `git fetch && git rebase origin/main` 후 전체 테스트를 다시 실행한다.

## 끝
리스트의 모든 항목이 ☑이면 `verify` 스킬로 넘어간다.
