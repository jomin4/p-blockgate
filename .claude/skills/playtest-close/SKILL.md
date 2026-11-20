---
name: playtest-close
description: 배포된 기능을 개발자가 플레이한 뒤 피드백이나 승인을 줄 때 사용 ("해봤는데 좋아", "승인", "이거 좀 이상해", "닫아도 돼"). 피드백을 분류해 수정 흐름으로 보내거나, G3 승인 시 이슈를 닫고 브랜치를 정리한다.
---

# 플레이 피드백 → 승인·정리 (워크플로우 12~14단계, G3)

## 1. 피드백 분류

| 분류 | 예 | 처리 |
|---|---|---|
| 승인 | "좋아", "승인", "닫아도 돼" | → 3. 정리 |
| AC 안의 문제 (버그) | "명령어 쓰면 메시지가 두 번 나와" | 같은 이슈에서 수정: 재현 테스트 Red → Green |
| AC 변경 (사용감 조정) | "쿨타임 10초는 너무 길어, 5초로" | 설계 문서 수정 → **G2 재승인** → 수정 |
| 새 아이디어 | "이것도 추가하면 좋겠다" | 별도 이슈로 `feature-proposal` (이번 이슈 범위에 넣지 않음) |

모호하면 어느 분류인지 확인한다. 피드백은 원문 요약을 이슈 코멘트로 남긴다.

## 2. 수정 흐름 (승인 전)
1. `node scripts/kanban.mjs move <이슈번호> "In Progress"`
2. `git switch main && git pull && git switch -c fix/<이슈번호>-<feedback-요약>`
3. `tdd-cycle` → `verify` → `review` → `ship` (PR 본문 `Refs #<이슈번호>`)
4. 다시 Playtest → 피드백 대기

## 3. G3 승인 → 정리
1. 이슈 코멘트: `G3 승인 — <승인자>, <날짜>`
2. 이슈 닫기 (칸반 자동 Done):
   ```bash
   gh issue close <이슈번호> --reason completed
   ```
3. 이 이슈의 브랜치 정리 (원격·로컬):
   ```bash
   git fetch --prune
   git ls-remote --heads origin "*/<이슈번호>-*"      # 대상 확인
   git push origin --delete <브랜치>                 # 각각
   git branch -D <브랜치>                            # Squash 머지라 -D (PR 머지 확인 후)
   ```
   삭제 전에 각 브랜치의 PR이 머지되었는지 `gh pr list --state merged --head <브랜치>`로 확인한다.
4. `git switch main && git pull`
5. `.claude/state/`의 해당 브랜치 파일(review-*, verify-*) 삭제
6. 보고: "이슈 #N 완료 — Done 이동, 브랜치 정리 완료"
