# infra/dev — 로컬 개발 서버

Paper 26.1.2(빌드 고정) + PostgreSQL 17 로컬 서버. 명세는 `docs/02-infrastructure.md` B장.

| 파일 | 내용 | 커밋 |
|---|---|---|
| `compose.yaml` | `mc`(Paper 26.1.2, offline-mode, 127.0.0.1:25565, RCON 127.0.0.1:25575) + `postgres:17.12`(127.0.0.1:5432) | ✅ |
| `.env.example` | 필요한 환경변수 키 목록 | ✅ |
| `.env` | 실제 값 | ❌ |
| `data/` | MC 월드·플러그인·로그 (DB는 Docker 볼륨 `blockgate-dev_pgdata`) | ❌ |
| `plugins-extra/` | VitaminMCP 플러그인 JAR (3.2.2, 받는 법은 local-dev 스킬) | ❌ |
| `patches/` | 로컬 전용 설정 패치: VitaminMCP `read-only: false`, bukkit `connection-throttle: -1`(E2E 봇 연속 재접속) | ✅ |

## 처음 한 번
1. `infra/dev/.env.example`을 `infra/dev/.env`로 복사하고 비밀번호를 바꾼다 (Claude에게 "로컬 서버 띄워줘"라고 하면 대신 만든다).
2. Minecraft EULA(https://aka.ms/MinecraftEULA)에 동의한 경우에만 `MC_EULA=TRUE`로 바꾼다.
3. `~/.vitaminmcp/agents` 폴더를 미리 만든다 (Linux에서 Docker가 root 소유로 만들면 플러그인이 쓰지 못한다). 이 폴더는 컨테이너에 쓰기 가능으로 마운트되므로 **이 로컬 서버 전용**으로 쓴다.
   - Linux: 서버는 UID 1000으로 돈다. 내 UID가 1000이 아니면 폴더 권한을 맞추고, `sudo docker compose`는 쓰지 않는다(`~`가 /root로 풀린다).
4. VitaminMCP.jar를 `plugins-extra/`에 받는다 (local-dev 스킬). 첫 기동 뒤 한 번 재시작해야 `read-only: false` 패치가 적용된다.

사용법은 `.claude/skills/local-dev/SKILL.md` 참고 ("로컬 서버 띄워줘").

> Windows: `data/`는 NTFS 바인드 마운트라 월드 I/O가 느릴 수 있다. 느리면 레포를 WSL 파일시스템(`\\wsl$\...`)에 두면 빠르다.
