# 09. 인프라 세팅 로드맵

> `infra-bootstrap` 스킬이 이 문서를 따라 Phase 순서대로 진행한다.
> 각 Phase가 끝나면 완료 조건을 확인하고 체크박스를 채운다. **Phase를 건너뛰지 않는다.**

## 표기

| 표기 | 의미 |
|---|---|
| 🔑 | 개발자에게 받아야 하는 값 (그 Phase에서 물어본다) |
| 👤 | 개발자가 웹 콘솔 등에서 직접 해야 하는 작업 (Claude가 단계별로 안내) |
| ✅ | 완료 조건 |

## 진행 방식

| 규칙 | 내용 |
|---|---|
| Phase 1 | 레포가 아직 없으므로 main에 초기 커밋을 직접 push (훅이 최초 push만 허용) |
| Phase 2부터 | 각 Phase를 `type:task` 이슈 → `chore/<issue#>-<desc>` 브랜치 → PR → auto-merge로 진행 (워크플로우 연습 겸) |
| 게이트 | 인프라 Phase는 G1·G3 대신 **Phase 시작 전 계획 확인**과 **완료 보고**로 대체 |
| 되돌리기 어려운 작업 | `terraform apply`, 리소스 삭제, Secrets 덮어쓰기, EULA 수락은 **실행 직전에 개발자 확인** |
| 비밀값 | 받은 값은 지정 위치에만 쓰고 다시 출력하지 않는다 (`docs/08-security.md`) |

---

## Phase 0. 사전 점검

| 항목 | 확인 명령 | 기준 |
|---|---|---|
| Git | `git --version` | 설치됨 |
| GitHub CLI | `gh auth status` | 로그인됨, `repo`·`project`·`workflow` 권한 |
| JDK | `java -version` | 25 (Temurin) |
| Docker | `docker compose version` | 실행 중 |
| Node.js | `node --version` | 20 이상 |
| Terraform | `terraform version` | 1.12 이상 (OCI backend) — 인프라 담당만 |
| OCI CLI | `oci --version` | 선택 (state 버킷 확인용) |

- [x] ✅ 위 표를 결과와 함께 보고, 없는 도구는 설치 방법 안내 (👤 설치)

## Phase 1. Git · GitHub 기반

🔑 레포 owner/이름(권장 `p-blockgate`), 공개 여부(권장 private), 참여 친구들의 GitHub 아이디

| # | 작업 |
|---|---|
| 1 | `git init -b main` → `git config core.hooksPath .githooks` → 초기 커밋 `chore(repo): 프로젝트 초기 구성` |
| 2 | `gh repo create` → main push (최초 push) |
| 3 | 레포 설정: Squash 머지만 허용, Allow auto-merge ON, 자동 head 브랜치 삭제 OFF |
| 4 | 라벨 생성: `.github/labels.yml` 기준 (기본 라벨은 삭제) |
| 5 | GitHub Project(Board) 생성: Status = Todo / In Progress / Playtest / Done, 레포에 연결 |
| 6 | 👤 Project 내장 Workflows: "Item closed → Done" ON, "Pull request merged → Done" OFF, "Item added → Todo" ON (Item closed·Item added 대상은 issue만) |
| 7 | `.github/project.json`에 project number·ID·Status 필드 ID·옵션 ID 기록 |
| 8 | `scripts/kanban.mjs` 구현: `add <issue#>`, `move <issue#> "<Status>"` (gh CLI 사용) → 동작 확인 |
| 9 | Rulesets(main): PR 필수, 승인 0, Squash만, force-push·삭제 금지 (필수 상태 체크는 Phase 7에서 추가) |
| 10 | collaborator 초대 (Write 권한) |
| 11 | 2번 push 이후 main 직접 커밋은 훅이 막는다 → 7~8번 결과물(`.github/project.json`, `scripts/kanban.mjs`)은 `[작업] Phase 1 마무리` 이슈 → `chore/<issue#>-phase-1-github` 브랜치 → review → PR로 반영 (첫 워크플로우 연습) |

- [x] ✅ `gh repo view`, 라벨 목록, 프로젝트 보드, Rulesets 조회 결과 보고
- [x] ✅ 테스트 이슈로 `kanban.mjs add/move` 동작 확인 후 테스트 이슈 삭제

## Phase 2. 로컬 개발 인프라 (`infra/dev`)

| # | 작업 |
|---|---|
| 1 | PaperMC API로 26.1.2의 **최신 stable 빌드 번호** 조회 → `PAPER_BUILD`로 고정, `docs/01-tech-stack.md`에 기록 |
| 2 | `infra/dev/compose.yaml`: `mc`(itzg, Java 25 태그 고정, Paper 26.1.2, `ONLINE_MODE=FALSE`, 127.0.0.1 바인딩, RCON 활성) + `postgres:17`(127.0.0.1:5432) |
| 3 | `infra/dev/.env.example` 작성 → `.env` 생성(gitignore) |
| 4 | 👤 **EULA 동의 확인** 후 `EULA=TRUE` |
| 5 | `docker compose -p blockgate-dev up -d` → 로그에서 `Done (` 확인 |
| 6 | `local-dev` 스킬의 명령(시작·중지·로그·psql·초기화)이 동작하는지 확인 |

- [x] ✅ 로컬 서버 기동, psql 접속, 데이터가 `infra/dev/data/`에만 생기는지 확인 (DB는 Docker 볼륨 `blockgate-dev_pgdata`)

## Phase 3. VitaminMCP 검증 (E2E 도구)

| # | 작업 |
|---|---|
| 1 | VitaminMCP 플러그인 JAR(26.1.2 지원 버전)을 `infra/dev/plugins-extra/`에 받고 로컬 서버에 설치 |
| 2 | `.mcp.json`의 `vitaminmcp` 서버로 `session_start` → `server_info` |
| 3 | `bot_spawn` → `command_exec`(봇으로 `/help`) → `assert_*` → `exceptions_recent` → `session_reset` |
| 4 | 동작한 버전으로 `.mcp.json`(npm 패키지 버전)과 플러그인 버전을 **고정**, `docs/01-tech-stack.md` 갱신 |
| 실패 시 | 증상·로그를 정리해 보고하고 멈춘다 (대안 결정은 개발자와) |

- [x] ✅ 봇 접속·명령·assert 결과 보고, 버전 고정 PR 머지

## Phase 4. 플러그인 골격 + 테스트 도구 (`plugins/`)

| # | 작업 |
|---|---|
| 1 | Gradle Wrapper(9.x), `settings.gradle.kts`, `gradle/libs.versions.toml`(모든 버전 고정) |
| 2 | `blockgate-core` 모듈: Paper 플러그인 메타(`paper-plugin.yml` 또는 `plugin.yml`, api-version 26.1.2 대응), Shadow + relocate |
| 3 | 소스셋 `test` + `integrationTest`, JUnit 5, AssertJ, MockBukkit v26.1.2, Mockito agent, Testcontainers, JaCoCo (`docs/04-tdd-testing.md` 8장) |
| 4 | **skip = 실패** 설정 → 일부러 skip 테스트를 만들어 빌드가 실패하는지 확인 → 제거 |
| 5 | DB 기반: HikariCP 풀, Flyway `V1__baseline.sql`, 비동기 실행기, 설정(`config.yml` + 환경변수) — **TDD로 작성** |
| 6 | 활성화 로그 `[BlockGateCore] Enabling` / DB 연결 결과 로그 (CD 헬스 체크에서 사용) |
| 7 | `deployDev` 태스크 → 로컬 서버에서 플러그인 활성화 확인 → VitaminMCP로 서버 예외 없음 확인 |

- [ ] ✅ `./gradlew check`(단위+통합) 통과, skip 0, JaCoCo 리포트 생성
- [ ] ✅ 로컬 서버에서 플러그인 활성화 + DB 연결 로그 확인

## Phase 5. OCI 인프라 (Terraform)

🔑 Tenancy OCID, User OCID, API 키 fingerprint, API 개인키 파일 경로, Region(`ap-osaka-1`), Object Storage namespace, SSH 공개키(시리얼 콘솔 복구용), Tailscale 일회용 auth key(`tag:server`)

| # | 작업 |
|---|---|
| 1 | 👤 OCI 콘솔에서 API 키 생성 → `~/.oci/config` 구성 (Claude가 단계 안내) |
| 2 | 👤 Tailscale: 계정 준비, ACL에 `tag:server`, `tag:ci` 정의 + SSH 규칙(`tag:ci` → `tag:server`의 `deploy`, 멤버 → `tag:server`) — Claude가 정책 JSON 제공 |
| 3 | `terraform.tfvars` 작성(gitignore) + `terraform.tfvars.example` 커밋 |
| 4 | `bootstrap/`: compartment `blockgate` + state 버킷 `blockgate-tfstate` (local state) → **apply 전 확인** |
| 5 | 본 인프라: VCN, public subnet, IGW, Security List(25565만), A1 2/12, 부트 50GB, 블록 100GB, `prevent_destroy`, freeform tags, `backend "oci"` |
| 6 | `cloud-init.yaml`: Docker, Tailscale(SSH), 블록 볼륨 마운트, iptables 25565, `deploy` 사용자 |
| 7 | `scripts/free-tier-guard` 작성 → plan에 실행 |
| 8 | `terraform plan` 요약 보고 → **개발자 확인 후 apply** (A1 용량 부족 시 재시도) |
| 9 | 👤 OCI Budget 알림 설정 (예: $1) |

- [ ] ✅ `tailscale ping blockgate-vm`, Tailscale SSH 접속, `docker version`, `/srv/blockgate` 마운트 확인
- [ ] ✅ 공인 IP에서 22번 포트가 닫혀 있는지 확인

## Phase 6. 운영 서버 (`infra/prod/server`)

🔑 화이트리스트 닉네임 목록, OP 닉네임, RCON·DB 비밀번호(Claude가 생성해도 됨 → Secrets로만 저장)

| # | 작업 |
|---|---|
| 1 | `compose.yaml`: `mc`(Phase 2와 같은 이미지·빌드, `ONLINE_MODE=TRUE`, `ENFORCE_WHITELIST=TRUE`, `MEMORY=8G`, 포트 25565만) + `postgres:17`(포트 없음), 데이터 `/srv/blockgate/*` |
| 2 | `.env.example` 커밋, VM `/srv/blockgate/.env`(600) 생성 |
| 3 | 첫 기동은 Tailscale SSH로 수동: `docker compose -p blockgate-prod up -d` → `Done (` 확인 |
| 4 | Phase 4의 core 플러그인 수동 배포 → 활성화·DB 연결 확인 |
| 5 | 👤 Minecraft 26.1.2 클라이언트로 `<공인 IP>:25565` 접속 테스트, 화이트리스트 밖 계정 차단 확인 |

- [ ] ✅ 친구 1명 이상 접속 성공, VitaminMCP가 운영 서버에 **없는지** 확인

## Phase 7. CI/CD

🔑 Tailscale OAuth client(`tag:ci`), 알림 웹훅 URL(선택)

| # | 작업 |
|---|---|
| 1 | `docs/06-ci-cd.md` 명세대로 6개 워크플로 작성 |
| 2 | GitHub Secrets 등록 (`gh secret set`, 값은 stdin) — 접두사 규칙 준수 |
| 3 | 테스트 PR로 CI 동작 확인 (관련 없는 영역 job이 성공으로 끝나는지 포함) |
| 4 | Rulesets에 필수 상태 체크 추가: `plugin-ci-build`, `plugin-ci-test`, `server-ci-validate`, `infra-ci-validate` |
| 5 | plugin 변경 PR 머지 → plugin-cd가 배포·헬스 체크·태그·알림까지 완료하는지 확인 |

- [ ] ✅ CI 4개 필수 체크 동작, plugin-cd 1회 성공, `plugin-v0.1.0` 태그 생성

## Phase 8. 워크플로우 리허설

| # | 작업 |
|---|---|
| 1 | 아주 작은 샘플 기능(예: `/bg ping` → `pong`)을 **14단계 전체**로 진행 (개발자가 G1·G2·G3 수행) |
| 2 | 진행 중 막힌 스킬·훅·템플릿을 `chore(repo)` PR로 수정 |
| 3 | 이 문서의 모든 체크박스 완료 확인 → `CLAUDE.md` 2장의 단계가 "기능 개발 단계"로 전환됨을 보고 |

- [ ] ✅ 샘플 기능이 Done까지 도달, 하네스 수정 사항 반영 완료

---

## 진행 현황

| Phase | 상태 | PR |
|---|---|---|
| 0. 사전 점검 | ✅ | — |
| 1. Git · GitHub | ✅ (collaborator 초대는 추후) | #3 |
| 2. 로컬 개발 인프라 | ✅ | #5 |
| 3. VitaminMCP 검증 | ✅ | #7 |
| 4. 플러그인 골격 + 테스트 | ⬜ | |
| 5. OCI 인프라 | ⬜ | |
| 6. 운영 서버 | ⬜ | |
| 7. CI/CD | ⬜ | |
| 8. 리허설 | ⬜ | |
