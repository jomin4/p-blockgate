# 08. 보안 규칙

## 1. 네트워크

| 대상 | 규칙 |
|---|---|
| 공개 포트 | **TCP 25565만** (Security List + VM iptables) |
| SSH | 인터넷에 열지 않음. Tailscale SSH만 |
| RCON | 포트 매핑 없음. VM 안에서 `docker compose exec mc rcon-cli`로만 |
| PostgreSQL | 포트 매핑 없음. Compose 내부 네트워크만 |
| 로컬 개발 | 모든 포트 `127.0.0.1` 바인딩 |

## 2. 접속 통제

| 항목 | 규칙 |
|---|---|
| 서버 인증 | `online-mode=true` (운영) |
| 화이트리스트 | `enforce-whitelist=true`, 친구 닉네임만 (`MC_WHITELIST`) |
| OP | 최소 인원 (`MC_OPS`) |
| GitHub | 친구들은 collaborator(Write). main은 Rulesets로 보호 |

## 3. 비밀값

| 비밀값 | 보관 위치 | 금지 |
|---|---|---|
| OCI OCID·fingerprint·API 키 | 로컬 `terraform.tfvars`(gitignore), `~/.oci/`, GitHub Secrets | 커밋, 채팅 외 출력, 로그 |
| Tailscale 키 | GitHub Secrets (`TS_`) | user_data에는 일회용·만료형 키만 |
| RCON / DB 비밀번호 | GitHub Secrets → VM `.env` (권한 600) | 레포·이슈·PR에 기록 |
| 웹훅 URL | GitHub Secrets | 공개 |

- Claude Code는 비밀값을 받으면 **지정된 파일·Secret에만 쓰고, 다시 출력하지 않는다** (확인 시 앞 4자리 정도만).
- `gh secret set`은 값을 stdin으로 넘긴다 (명령줄 인자에 값 노출 금지).
- `.gitignore`: `.env`, `*.tfvars`(예시 제외), `*.pem`, `*.key`, `*.tfstate*`, `.terraform/`, `.claude/settings.local.json`, `.claude/state/`.

## 4. 운영 서버 보호

| 금지 | 이유 |
|---|---|
| VitaminMCP 설치 | 테스트 도구가 운영 서버를 조작할 수 있음 |
| 봇 접속, 임의 콘솔 명령, DB 직접 쓰기 | 플레이 중인 친구들의 데이터 보호 |
| `/reload` | 플러그인 상태 오염 → restart만 |
| 수동 변경 | 서버 설정 변경은 `infra/prod/server/` PR → server-cd로만 |

## 5. 클라우드 비용

| 규칙 | 내용 |
|---|---|
| Always Free만 | 무료 한도 가드(CI)가 위반 시 실패 |
| 보호 | VM·블록 볼륨 `prevent_destroy` |
| 예산 알림 | OCI 콘솔에서 Budget 알림(예: $1) 설정 — Phase 5 체크리스트 |

## 6. 라이선스

| 항목 | 규칙 |
|---|---|
| Minecraft EULA | `EULA=TRUE` 설정 전 **개발자에게 동의 여부를 확인**한다 |
