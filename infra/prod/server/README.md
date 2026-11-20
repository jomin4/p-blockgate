# infra/prod/server — 운영 서버 설정

Phase 6에서 Claude Code가 `compose.yaml`을 작성한다. 명세는 `docs/02-infrastructure.md` A장 "운영 서버 Compose".

| 파일 | 내용 | 커밋 |
|---|---|---|
| `compose.yaml` | `mc`(Paper 26.1.2, online-mode, whitelist, 25565만) + `postgres:17`(포트 없음) | ✅ |
| `.env.example` | 필요한 키 목록 | ✅ |
| VM `/srv/blockgate/.env` | 실제 값 (server-cd가 Secrets로 생성) | ❌ |

## 규칙
- 이 폴더 변경은 PR → `server-cd`로만 반영한다 (VM에서 직접 수정 금지).
- VitaminMCP 등 테스트 도구를 넣지 않는다.
- 재시작은 restart로 (`/reload` 금지).
