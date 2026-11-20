---
name: issue-create
description: G1에서 방안이 선택된 뒤, 또는 버그·작업(task)을 등록할 때 GitHub 이슈를 템플릿대로 만들고 칸반 Todo에 올린다. "이슈 올려줘"라고 할 때도 사용.
---

# 이슈 생성 (워크플로우 3단계)

## 절차

1. **템플릿 선택**: 기능 → `.github/ISSUE_TEMPLATE/feature.md`, 버그 → `bug.md`, 작업 → `task.md`
2. **본문 작성**: 템플릿의 모든 섹션을 채워 `.claude/state/issue-body.md`에 쓴다 (frontmatter 제외). 비어 있는 섹션은 "해당 없음"으로.
3. **제목**: `[기능] <요약>` / `[버그] <요약>` / `[작업] <요약>` (한국어, 50자 이내)
4. **라벨**: `area:<영역>`(1개 이상) + `type:<feature|bug|task|refactor>` + `priority:<P0|P1|P2>`
   - 우선순위 기본값: 기능 P1, 버그 P1(서버 크래시·데이터 손상은 P0), 작업 P2
5. **생성**:
   ```bash
   gh issue create --title "[기능] /home 명령어" --body-file .claude/state/issue-body.md \
     --label "area:plugin,type:feature,priority:P1"
   ```
6. **칸반**: `node scripts/kanban.mjs add <이슈번호>` (Todo)
7. **G1 기록**: 기능이면 이슈 코멘트로 "G1 승인: 방안 N (<이름>) — 선택자, 이유"를 남긴다.
8. **보고**: 이슈 번호·링크·라벨을 한 줄로 알리고 `design-plan` 스킬로 넘어간다 (작업 이슈는 바로 작업 시작 가능).

## 참고
- `scripts/kanban.mjs`가 아직 없으면(Phase 1 이전) 칸반 단계는 건너뛰고 보고에 표시한다.
