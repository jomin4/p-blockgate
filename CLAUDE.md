# BlockGate — Claude Code 작업 지침

이 파일은 이 레포에서 일하는 **모든 개발자의 Claude Code가 똑같이 따르는 규칙**이다.
여기 적힌 흐름에서 벗어나지 않는다. 판단이 애매하면 멈추고 개발자에게 묻는다.

## 1. 프로젝트 한 줄 요약

친구들이 Claude Code로 함께 개발하면서 같은 Minecraft 서버(Paper 26.1.2)에서 게임하는 프로젝트.
개발자는 **제안·피드백·검수·플레이**만 하고, Claude Code가 **조사·이슈·설계·구현·테스트·리뷰·PR·머지·배포 확인·정리**를 한다.

## 2. 지금 어느 단계인가

- `docs/09-setup-roadmap.md`의 Phase 체크리스트가 **모두 끝나기 전**이면 → **인프라 세팅 단계**.
  `infra-bootstrap` 스킬을 따라 Phase 순서대로 진행한다.
- 모두 끝났으면 → **기능 개발 단계**. 아래 3장의 워크플로우를 따른다.

## 3. 기능 개발 워크플로우 (요약)

| # | 단계 | 스킬 | 칸반 | 게이트 |
|---|---|---|---|---|
| 1~2 | 제안 → 조사 → 방안 2~3개 제시 | `feature-proposal` | | **G1 방안 선택** |
| 3 | 이슈 생성 | `issue-create` | Todo | |
| 4 | 설계 문서 (AC, 기대값, 테스트 리스트) | `design-plan` | | **G2 설계 승인** |
| 5~6 | 브랜치 생성 → TDD 사이클 반복 | `tdd-cycle` | In Progress | |
| 7~8 | E2E 인수 테스트 + 전체 검증 | `verify` | | |
| 9 | 서브에이전트 2단계 리뷰 | `review` | | |
| 10~11 | PR → auto-merge → 배포 확인 | `ship` | Playtest | |
| 12~14 | 플레이 피드백 → 승인 시 정리 | `playtest-close` | Done | **G3 플레이 승인** |

버그는 `bugfix` 스킬. 자세한 내용은 `docs/03-dev-workflow.md`.

## 4. 게이트 규칙 (절대 건너뛰지 않음)

- **G1·G2·G3는 개발자의 명시적 승인**("1번으로 가자", "승인", "좋아 진행해" 등)이 있어야 다음으로 넘어간다.
- 침묵이나 모호한 답은 승인이 아니다. 다시 묻는다.
- 승인 내용은 이슈 코멘트로 기록한다.

## 5. TDD 규칙 (Canon TDD)

1. 테스트 리스트(설계 문서) → 2. 하나만 테스트로 작성 → **실행해서 Red 확인** → 3. 최소 구현으로 Green → 4. Green 상태에서만 Refactor → 5. 리스트가 빌 때까지 반복.

- 실패하는 테스트 없이 프로덕션 코드를 쓰지 않는다.
- 커밋은 `test(...)`(Red) → `feat(...)`/`fix(...)`(Green) 쌍으로 남긴다.
- **skip된 테스트는 실패로 간주**한다 (MockBukkit 미구현 메서드는 skip으로 보고됨).
- 금지: assertion 약화, `@Disabled`, 구현에 맞춰 기대값 수정, 예외 삼키기, 통과시키려고 기존 테스트 수정.
- 기대값은 **설계 문서가 기준**이다. 설계가 틀렸다고 판단되면 멈추고 보고 → G2 재승인.
- 자세한 내용: `docs/04-tdd-testing.md`

## 6. Git · GitHub 규칙

- 브랜치: main만 장기 브랜치. 작업 브랜치 `<type>/<issue#>-<desc>` (예: `feat/12-home-command`). worktree 사용 안 함.
- 커밋·PR 제목: `<type>(<영역키>): <제목>` — type: feat, fix, test, refactor, docs, chore, ci, build, perf / 영역키: plugin, server, infra, repo, docs
- PR 본문에 `Refs #N` (Closes 금지 — 이슈는 G3 승인 후 닫는다).
- 머지: `gh pr merge --auto --squash`만 사용. 직접 push to main 금지, `--no-verify` 금지, force-push 금지.
- 훅(`.claude/hooks`, `.githooks`)이 위 규칙을 강제한다. 훅이 막으면 **우회하지 말고 원인을 고친다.**
- 자세한 내용: `docs/05-git-github.md`

## 7. 고정 버전 (임의로 바꾸지 않음)

Minecraft·Paper **26.1.2** / Java **Temurin 25** / paper-api `26.1.2.build.N-stable` / MockBukkit `mockbukkit-v26.1.2` / PostgreSQL 17.
버전 변경은 `docs/10-version-policy.md` 절차로만, `server` 영역 별도 이슈로 진행한다.

## 8. 보안 원칙 (위반 금지)

- 외부 공개 포트는 **TCP 25565만**. RCON·PostgreSQL·SSH는 인터넷에 열지 않는다 (관리 접속은 Tailscale).
- 비밀값(OCI 키·OCID, Tailscale 키, RCON/DB 비밀번호, 웹훅)은 **GitHub Secrets 또는 gitignore된 파일**에만. 커밋·로그·PR·이슈에 절대 출력하지 않는다.
- 운영 서버에는 **VitaminMCP 설치 금지**, 봇 접속·임의 콘솔 명령·DB 직접 쓰기 금지.
- Oracle **Always Free 리소스만** 생성한다. 무료 한도를 넘는 변경은 멈추고 보고.
- Minecraft EULA 수락은 **개발자 동의를 받은 뒤**에만 설정한다.
- 서버 재시작은 restart로 한다. `/reload` 금지.

## 9. 문서 지도

| 문서 | 내용 |
|---|---|
| `docs/00-decisions.md` | 결정 사항 최종본 (요약표) |
| `docs/01-tech-stack.md` | 기술 스택·버전 |
| `docs/02-infrastructure.md` | 운영(OCI)·개발(로컬) 인프라 상세 |
| `docs/03-dev-workflow.md` | 기능 개발 14단계 상세 |
| `docs/04-tdd-testing.md` | TDD·테스트 규칙, Gradle 설정 |
| `docs/05-git-github.md` | 브랜치·커밋·라벨·Rulesets·칸반 |
| `docs/06-ci-cd.md` | CI/CD 워크플로 명세 |
| `docs/07-claude-harness.md` | 스킬·훅·서브에이전트·MCP 구조 |
| `docs/08-security.md` | 보안 규칙 |
| `docs/09-setup-roadmap.md` | **인프라 세팅 Phase 체크리스트** |
| `docs/10-version-policy.md` | 버전 업그레이드 규칙 |
| `docs/plans/_TEMPLATE.md` | 설계 문서 템플릿 |

## 10. 소통 방식

- 개발자에게는 **한국어**로, 결과는 가능하면 **표**로 정리한다.
- 진행 보고는 짧게. 결정이 필요한 것만 묻는다.
- 도구·규칙은 기능이 늘어나면 유연하게 추가·변경할 수 있다. 단, 바꿀 때는 이 문서와 관련 docs를 **같은 PR에서** 갱신한다.
