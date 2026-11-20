# 05. Git · GitHub 규칙

## 1. 브랜치

| 항목 | 규칙 |
|---|---|
| 장기 브랜치 | `main` 하나 (GitHub Flow) |
| 작업 브랜치 | `<type>/<issue#>-<desc>` — desc는 영문 소문자·숫자·하이픈 |
| 예 | `feat/12-home-command`, `fix/31-shop-npe`, `chore/2-github-setup` |
| type | feat, fix, test, refactor, docs, chore, ci, build, perf |
| 금지 | worktree, develop 브랜치, main 직접 커밋·push |
| 정리 | G3 승인 후 원격·로컬 삭제 (자동 삭제 OFF) |

## 2. 커밋 · PR 제목 (Conventional Commits)

```
<type>(<영역키>): <제목>
```

| 요소 | 규칙 |
|---|---|
| type | feat, fix, test, refactor, docs, chore, ci, build, perf, revert |
| 영역키 | `plugin`(plugins/**), `server`(infra/prod/server/**), `infra`(infra/prod/terraform/**), `repo`(그 외 레포 공통·infra/dev), `docs`(docs/**) |
| 제목 | 한국어 가능, 72자 이내, 마침표 없음 |
| Breaking | `feat(plugin)!: ...` + 본문 `BREAKING CHANGE:` + `breaking-change` 라벨 |
| 예 | `feat(plugin): /home 명령어 추가`, `ci(repo): plugin-ci 워크플로 추가` |

- 커밋 메시지 형식은 `.githooks/commit-msg`가 검사한다.
- Squash 머지라 main에는 **PR 제목이 커밋 제목**이 된다 → PR 제목도 같은 형식.

## 3. 이슈

| 항목 | 규칙 |
|---|---|
| 템플릿 | `feature`, `bug`, `task` (`.github/ISSUE_TEMPLATE/`) |
| 생성 | Claude가 `gh issue create --body-file`로 템플릿 섹션을 채워 생성 |
| 라벨 | `area:*` 1개 이상, `type:*` 1개, `priority:*` 1개 필수 |
| 연결 | PR 본문 `Refs #N` (Closes 금지) |
| 닫기 | G3 승인 후 Claude가 닫음 → 칸반 자동 Done |

## 4. 라벨 (`.github/labels.yml`)

| 그룹 | 라벨 |
|---|---|
| 영역 | `area:plugin`, `area:server`, `area:infra`, `area:repo`, `area:docs` |
| 유형 | `type:feature`, `type:bug`, `type:task`, `type:refactor` |
| 우선순위 | `priority:P0`(즉시), `priority:P1`(이번 작업), `priority:P2`(나중) |
| 상태 | `status:blocked`, `status:needs-feedback` |
| 특수 | `db-migration`, `breaking-change` |

## 5. Rulesets (main)

| 규칙 | 값 |
|---|---|
| PR 필수 | ON |
| 필요 승인 수 | **0** (리뷰는 로컬 서브에이전트가 담당) |
| 필수 상태 체크 | `plugin-ci-build`, `plugin-ci-test`, `server-ci-validate`, `infra-ci-validate` (job 내부 경로 감지 → 무관하면 성공 처리) |
| 머지 방식 | Squash만 |
| force-push / 삭제 | 금지 |
| Allow auto-merge | ON (레포 설정) |
| head 브랜치 자동 삭제 | OFF (G3 후 정리) |
| bypass | 없음 |

## 6. 칸반 (GitHub Projects)

| 컬럼(Status) | 들어오는 시점 | 이동 주체 |
|---|---|---|
| Todo | 이슈 생성 | `issue-create` 스킬 |
| In Progress | 브랜치 생성 | `tdd-cycle` 스킬 |
| Playtest | 배포 확인 완료 | `ship` 스킬 |
| Done | 이슈 닫힘 | 프로젝트 내장 자동화 "Item closed → Done" |

- 내장 자동화 "Pull request merged → Done"은 **끈다** (머지 ≠ 완료).
- "Item closed → Done"·"Item added → Todo"의 대상은 **issue만** (PR은 보드에 올리지 않는다).
- 상태 이동은 `scripts/kanban.mjs`로 한다 (Phase 1에서 구현):
  `node scripts/kanban.mjs add <issue#>` / `node scripts/kanban.mjs move <issue#> "<Status>"`
- 프로젝트 ID·필드 ID는 `.github/project.json`에 저장 (비밀 아님, 커밋).

## 7. PR

| 항목 | 규칙 |
|---|---|
| 템플릿 | `.github/pull_request_template.md` |
| 제목 | 커밋 형식과 동일 |
| 본문 필수 | `Refs #N`, 테스트 리스트 체크, 검증 결과, 리뷰 결과 |
| 생성 조건 | 로컬 리뷰 통과 표시(`.claude/state/review-*.json`)가 현재 HEAD와 일치 (훅 검사) |
| 머지 | `gh pr merge <번호> --auto --squash` |
| 사람 리뷰 | 필수 아님. 개발자는 G3 플레이로 검수 |

## 8. 로컬 git 설정 (훅이 자동 적용)

| 설정 | 값 | 적용 |
|---|---|---|
| `core.hooksPath` | `.githooks` | SessionStart 훅이 자동 설정 |
| `pull.rebase` | `true` | SessionStart 훅이 자동 설정 |
| `fetch.prune` | `true` | SessionStart 훅이 자동 설정 |
