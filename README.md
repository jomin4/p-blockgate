# BlockGate

친구들이 **Claude Code로 함께 개발하면서 같은 서버에서 게임하는** Minecraft Java Edition 서버 프로젝트.

| 항목 | 내용 |
|---|---|
| 게임 | Minecraft Java Edition **26.1.2** (클라이언트도 26.1.2로 접속) |
| 서버 | Paper 26.1.2 on Oracle Cloud Always Free (Osaka, Ampere A1) |
| 개발 | Java 25 · Gradle · PostgreSQL 17 · TDD |
| 협업 | GitHub Flow · Kanban · Claude Code 하네스(스킬·훅·서브에이전트) |

## 이 폴더는 무엇인가

지금까지 결정한 기술 스택·인프라·개발 워크플로우를 **Claude Code가 읽고 그대로 세팅할 수 있도록** 정리한 출발점이다.
실제 코드(Terraform, Compose, Gradle, CI/CD)는 Claude Code가 `docs/09-setup-roadmap.md`의 Phase 순서대로 만든다.

## 시작하기 (인프라 담당자, 최초 1회)

1. 이 폴더를 GitHub 레포로 만들 위치에 둔다.
2. 폴더에서 Claude Code를 실행한다.
3. 이렇게 말한다: **"인프라 세팅 시작하자"**
4. Claude Code가 Phase마다 필요한 값(🔑 표시: OCI OCID, Tailscale 키 등)을 그때그때 물어본다. 채팅으로 알려주면 된다.

## 개발 참여하기 (모든 개발자)

| 단계 | 할 일 |
|---|---|
| 1. 준비 | Git, GitHub CLI(`gh auth login`), JDK 25, Docker, Node.js, Claude Code 설치 |
| 2. 클론 | `git clone <repo>` 후 폴더에서 Claude Code 실행 (훅이 자동으로 git 설정을 맞춘다) |
| 3. 로컬 서버 | Claude Code에게 "로컬 개발 서버 띄워줘" |
| 4. 기능 제안 | "OO 기능 만들고 싶어" → Claude Code가 조사 후 방안을 제시 |
| 5. 결정 | 방안 선택(G1) → 설계 승인(G2) → 배포 후 플레이해보고 승인(G3) |

개발자는 **제안·피드백·검수·플레이**만 한다. 나머지는 Claude Code가 한다.

## 문서

- `CLAUDE.md` — Claude Code가 항상 따르는 규칙
- `docs/00-decisions.md` — 결정 사항 최종본
- `docs/09-setup-roadmap.md` — 인프라 세팅 체크리스트
