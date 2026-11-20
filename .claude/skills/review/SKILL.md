---
name: review
description: 검증을 통과한 작업 브랜치를 서브에이전트로 2단계 리뷰(spec-reviewer → quality-reviewer)하고, 통과하면 리뷰 표시를 기록할 때 사용. PR 생성 전에 반드시 거친다. "리뷰 돌려줘"에도 발동한다.
---

# 2단계 리뷰 (워크플로우 9단계)

## 절차

1. **spec-reviewer** 서브에이전트 호출: 이슈 번호, 설계 문서 경로, 브랜치 이름 전달 (인프라 Phase면 로드맵 Phase 전달).
2. FAIL이면 Critical/Major를 고친다.
   - 동작이 바뀌는 수정은 TDD로 (Red → Green).
   - 고친 뒤 테스트를 다시 돌리고 spec-reviewer를 다시 호출한다.
3. spec이 PASS면 **quality-reviewer** 호출 → 같은 방식으로 수정·재리뷰.
   - quality 수정 후에는 spec-reviewer도 다시 한 번 통과시킨다 (수정이 스펙을 깨지 않았는지).
4. 두 리뷰어 모두 PASS면 기록:
   ```bash
   node .claude/hooks/mark-review.mjs pass "<spec 요약>" "<quality 요약>"
   ```
5. Minor 지적 중 고치지 않은 것은 PR 본문 "리뷰 결과"에 남긴다.
6. `ship` 스킬로 넘어간다.

## 규칙
- 3라운드 안에 PASS가 안 나오면 멈추고 개발자에게 쟁점을 표로 보고한다.
- 리뷰 표시는 **현재 HEAD**에 묶인다. 이후 커밋(rebase 포함)이 생기면 다시 리뷰해야 PR을 만들 수 있다 (훅이 검사).
- 리뷰어 결과를 무시하거나, 리뷰 없이 `mark-review`를 실행하지 않는다.
