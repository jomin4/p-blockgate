# infra/dev — 로컬 개발 서버

Phase 2에서 Claude Code가 `compose.yaml`을 작성한다. 명세는 `docs/02-infrastructure.md` B장.

| 파일 | 내용 | 커밋 |
|---|---|---|
| `compose.yaml` | `mc`(Paper 26.1.2, offline-mode, 127.0.0.1) + `postgres:17`(127.0.0.1:5432) | ✅ |
| `.env.example` | 필요한 환경변수 키 목록 | ✅ |
| `.env` | 실제 값 | ❌ |
| `data/` | 월드·플러그인·로그 | ❌ |
| `plugins-extra/` | VitaminMCP 플러그인 JAR | ❌ |

사용법은 `.claude/skills/local-dev/SKILL.md` 참고 ("로컬 서버 띄워줘").
