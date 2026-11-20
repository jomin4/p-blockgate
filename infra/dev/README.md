# infra/dev — 로컬 개발 서버

Paper 26.1.2(빌드 고정) + PostgreSQL 17 로컬 서버. 명세는 `docs/02-infrastructure.md` B장.

| 파일 | 내용 | 커밋 |
|---|---|---|
| `compose.yaml` | `mc`(Paper 26.1.2, offline-mode, 127.0.0.1:25565, RCON 127.0.0.1:25575) + `postgres:17.12`(127.0.0.1:5432) | ✅ |
| `.env.example` | 필요한 환경변수 키 목록 | ✅ |
| `.env` | 실제 값 | ❌ |
| `data/` | MC 월드·플러그인·로그 (DB는 Docker 볼륨 `blockgate-dev_pgdata`) | ❌ |
| `plugins-extra/` | VitaminMCP 플러그인 JAR | ❌ |

## 처음 한 번
1. `infra/dev/.env.example`을 `infra/dev/.env`로 복사하고 비밀번호를 바꾼다 (Claude에게 "로컬 서버 띄워줘"라고 하면 대신 만든다).
2. Minecraft EULA(https://aka.ms/MinecraftEULA)에 동의한 경우에만 `MC_EULA=TRUE`로 바꾼다.

사용법은 `.claude/skills/local-dev/SKILL.md` 참고 ("로컬 서버 띄워줘").
