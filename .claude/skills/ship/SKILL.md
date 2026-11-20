---
name: ship
description: 리뷰를 통과한 브랜치로 PR을 만들고 auto-merge(Squash)한 뒤, CI·CD를 지켜보고 배포가 정상인지 확인해 칸반을 Playtest로 옮길 때 사용. "PR 올리고 배포해"에도 발동한다.
---

# PR → 머지 → 배포 확인 (워크플로우 10~11단계)

## 1. 최신화
```bash
git fetch origin
git rebase origin/main        # main이 앞서 있으면
```
rebase로 HEAD가 바뀌면 → 전체 테스트 재실행 → `review` 스킬 재실행(변경분 중심) → 다시 여기로.

## 2. Push
```bash
git push -u origin <브랜치>                 # 이미 push했고 rebase했다면 --force-with-lease
```

## 3. PR 생성
1. `.github/pull_request_template.md`를 채워 `.claude/state/pr-body.md`에 쓴다.
   - `Refs #<이슈번호>` (Closes 금지), 테스트 리스트 체크, `.claude/state/verify-*.md`의 검증 결과, 리뷰 결과
2. 생성:
   ```bash
   gh pr create --base main --title "<type>(<영역>): <제목>" \
     --body-file .claude/state/pr-body.md --label "area:<영역>,type:<유형>"
   ```
   (훅이 제목 형식, Refs, 리뷰 표시 = HEAD, TDD 커밋 순서를 검사한다)
3. auto-merge:
   ```bash
   gh pr merge <PR번호> --auto --squash
   ```
   필수 상태 체크가 아직 없는 시기(세팅 Phase 1~6)에는 PR이 바로 머지 가능 상태라 auto-merge 활성화가 거절될 수 있다. 그때는 `gh pr merge <PR번호> --squash`로 머지한다.

## 4. CI 확인
```bash
gh pr checks <PR번호> --watch
```
실패하면 원인을 고친다(동작 수정은 TDD) → push → 리뷰 표시가 무효화되므로 `review` 재실행 → 다시 확인.

## 5. CD 확인 (머지 후)
```bash
gh run list --workflow plugin-cd.yml --branch main --limit 1     # 영역에 맞는 워크플로
gh run watch <run-id> --exit-status
```
| 결과 | 처리 |
|---|---|
| 성공 | 버전 태그·헬스 체크 결과 확인 → 6단계 |
| 실패 | 로그 요약을 개발자에게 보고 → `bugfix` 흐름(P0)으로 즉시 수정 |

## 6. Playtest 전환
1. `node scripts/kanban.mjs move <이슈번호> "Playtest"`
2. 이슈 코멘트: 배포 버전(태그), 변경 요약, 설계 문서의 **플레이테스트 포인트**
3. 개발자에게 알림:

| 항목 | 내용 |
|---|---|
| 배포 | `plugin-vX.Y.Z` 반영 완료 |
| 해볼 것 | 플레이테스트 포인트 |
| 다음 | 플레이 후 "좋아/승인" 또는 피드백을 알려주세요 |

4. 로컬: `git switch main && git pull` (작업 브랜치는 G3까지 유지)
