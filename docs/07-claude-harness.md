# 07. Claude Code 하네스

모든 개발자의 Claude Code가 **같은 흐름으로** 움직이게 하는 장치들. 전부 레포에 포함되어 클론하면 바로 적용된다.

## 1. 구성 요소

| 구성 | 위치 | 역할 |
|---|---|---|
| 규칙 | `CLAUDE.md` | 항상 로드되는 핵심 규칙 |
| 스킬 | `.claude/skills/<name>/SKILL.md` | 단계별 절차. 자연어로 자동 발동 (슬래시 명령 불필요) |
| 서브에이전트 | `.claude/agents/*.md` | 조사·리뷰를 독립 컨텍스트에서 수행 |
| Claude 훅 | `.claude/hooks/*.mjs` + `.claude/settings.json` | 명령 실행 전 규칙 강제, 세션 시작 시 설정 |
| Git 훅 | `.githooks/` | 커밋 메시지·push 규칙을 git 수준에서 강제 (사람이 직접 커밋해도 적용) |
| MCP | `.mcp.json` | VitaminMCP (E2E) |
| 상태 | `.claude/state/` (gitignore) | 리뷰 통과 표시 등 로컬 상태 |

> 훅은 Node.js로 작성되어 Windows·macOS·Linux에서 동일하게 동작한다 (Node.js는 VitaminMCP 때문에 이미 필수).

## 2. 스킬 목록

| 스킬 | 발동 예시 (자연어) | 단계 |
|---|---|---|
| `infra-bootstrap` | "인프라 세팅 시작하자", "다음 Phase 진행해" | 최초 세팅 |
| `feature-proposal` | "OO 기능 만들고 싶어", "이런 거 추가하면 어때?" | 1~2 (G1) |
| `issue-create` | (G1 후 자동) "이슈 올려줘" | 3 |
| `design-plan` | (이슈 후 자동) "설계 보여줘" | 4 (G2) |
| `tdd-cycle` | (G2 후 자동) "구현 시작해" | 5~6 |
| `verify` | (TDD 완료 후 자동) "검증해줘" | 7~8 |
| `review` | (검증 후 자동) "리뷰 돌려줘" | 9 |
| `ship` | (리뷰 통과 후 자동) "PR 올리고 배포해" | 10~11 |
| `playtest-close` | "해봤는데 좋아", "이거 좀 이상해", "승인" | 12~14 (G3) |
| `bugfix` | "버그 있어", "OO가 안 돼" | 버그 흐름 |
| `local-dev` | "로컬 서버 띄워줘", "개발 서버 재시작" | 개발 환경 |

## 3. 서브에이전트

| 이름 | 하는 일 | 도구 제한 |
|---|---|---|
| `researcher` | 기능 제안 조사(공식 문서, Paper API, 기존 플러그인 사례) → 방안 2~3개 근거 | 읽기·웹 조회만 |
| `spec-reviewer` | 1단계 리뷰: 설계 문서 대비 AC·테스트 리스트 커버리지, 기대값 일치, 기존 테스트 변조 여부, TDD 커밋 순서 | 읽기·git 조회만 |
| `quality-reviewer` | 2단계 리뷰: 코드 품질, 스레드 안전성(메인 스레드 DB 금지), 보안, 성능, 규칙 준수 | 읽기·git 조회만 |

- 리뷰어는 코드를 **수정하지 않는다**. 결과(심각도별 목록)를 메인 Claude에게 돌려주고, 메인 Claude가 고친 뒤 다시 리뷰를 받는다.
- 두 리뷰어 모두 "통과"하면 `review` 스킬이 `.claude/state/review-<branch>.json`에 HEAD SHA를 기록한다.

## 4. 훅

### Claude 훅 (`.claude/settings.json`)

| 이벤트 | 스크립트 | 하는 일 |
|---|---|---|
| SessionStart | `session-start.mjs` | `core.hooksPath=.githooks` 등 git 설정 자동 적용, 현재 브랜치·단계 안내 |
| PreToolUse (Bash) | `git-guard.mjs` | 아래 표의 명령 차단·검사 |

| 검사 대상 | 규칙 |
|---|---|
| `--no-verify`, `-n`(commit) | 차단 (git 훅 우회 금지) |
| `git commit` on main | 원격에 main이 생긴 뒤에는 차단 (Phase 1 초기 커밋만 허용) |
| `git push` | main으로의 push 차단, force-push 차단 |
| 브랜치 생성 (`checkout -b`, `switch -c`, `branch <name>`) | 이름 형식 검사 |
| `gh pr create` | 제목 형식, 본문 `Refs #N`, 리뷰 통과 표시 = HEAD, `test(` 커밋 선행 |
| `gh pr merge` | `--auto --squash` 필수 |
| `git worktree` | 차단 |

### Git 훅 (`.githooks/`)

| 훅 | 하는 일 |
|---|---|
| `commit-msg` | Conventional Commits + 영역키 형식 검사 |
| `pre-push` | main push 차단 (원격에 main이 없을 때의 최초 push만 허용), 브랜치 이름 검사 |

## 5. MCP

| 서버 | 용도 | 비고 |
|---|---|---|
| `vitaminmcp` | 로컬 서버에서 봇 E2E | `vitaminmcp@3.2.2`로 고정 (Phase 3 검증). 플러그인 버전과 함께 올린다 (`docs/10-version-policy.md`) |

- GitHub 작업은 MCP 대신 `gh` CLI를 사용한다.
- DB는 `docker compose exec postgres psql`로 접근한다.

## 6. 하네스 변경 규칙

| 규칙 | 내용 |
|---|---|
| 변경 방법 | `chore(repo)` 이슈 → PR. 스킬·훅·CLAUDE.md·docs를 **같은 PR**에서 함께 갱신 |
| 훅 테스트 | 훅 수정 시 `node .claude/hooks/test-hooks.mjs` 통과 필수 |
| 개인 설정 | 개인 취향은 `.claude/settings.local.json`(gitignore)에만 |
