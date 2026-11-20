---
name: local-dev
description: 개발자 PC의 로컬 개발 서버(infra/dev, Paper 26.1.2 + PostgreSQL + VitaminMCP)를 다룰 때 사용. "로컬 서버 띄워줘", "개발 서버 재시작", "플러그인 반영해줘", "DB 들어가 봐", "로컬 월드 초기화" 등.
---

# 로컬 개발 서버

Compose 프로젝트: `blockgate-dev` / 위치: `infra/dev/` (Phase 2 이후 사용 가능)

| 작업 | 명령 (레포 루트에서) |
|---|---|
| 시작 | `docker compose -p blockgate-dev -f infra/dev/compose.yaml --env-file infra/dev/.env up -d` |
| 상태 | `docker compose -p blockgate-dev ps` |
| 로그 | `docker compose -p blockgate-dev logs -f --tail 100 mc` |
| 기동 확인 | 로그에서 `Done (` |
| 재시작 | `docker compose -p blockgate-dev restart mc` (`/reload` 금지). `vitaminmcp-proxy`는 mc 네트워크의 소유자라 **따로 재시작하지 않는다** (필요하면 `up -d`) |
| 중지 | `docker compose -p blockgate-dev stop` |
| VitaminMCP 플러그인 받기 | `mkdir -p ~/.vitaminmcp/agents` → `gh release download 3.2.2 -R Backas03/VitaminMCP -p VitaminMCP.jar -D infra/dev/plugins-extra --clobber` → SHA-256이 `docs/01-tech-stack.md` 값과 같은지 확인 → 시작 → **첫 기동 뒤 한 번 재시작**(read-only 해제 패치 적용) |
| VitaminMCP 연결 확인 | `session_start`(인자 없음) → 응답의 `readOnly: false` 확인. 회귀용 `plugins/e2e/scenarios/smoke-help.json` |
| 플러그인 반영 | `cd plugins && ./gradlew deployDev` |
| 콘솔 명령 | `docker compose -p blockgate-dev exec mc rcon-cli <명령>` |
| DB 접속 | `docker compose -p blockgate-dev exec postgres psql -U blockgate blockgate` |
| 월드 초기화 | ⚠️ 확인 후: 중지 → `infra/dev/data/world*` 삭제 → 시작 |
| DB 초기화 | ⚠️ 확인 후: `docker compose -p blockgate-dev down -v` → 시작 (Flyway가 다시 마이그레이션) |

## 접속
- 개발자 클라이언트(26.1.2): 멀티플레이 → `localhost:25565` (로컬은 offline-mode라 봇과 함께 접속 가능)
- E2E 봇: VitaminMCP `session_start` (기본값으로 localhost에 연결)

## 문제 해결
| 증상 | 확인 |
|---|---|
| 컨테이너가 안 뜸 | Docker 실행 여부, `.env` 존재, 25565·25575·25585·5432 포트 사용 중인지 |
| 플러그인 미적용 | `infra/dev/data/plugins/`에 JAR가 있는지, 로그의 `Enabling` 줄 |
| VitaminMCP 연결 실패 | `curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:25585/mcp`가 401이면 정상. 000이면 위의 시작 명령(`up -d`)으로 다시 맞춘다 |
| VitaminMCP read-only | 로그에 `endpoint listening ... (read-only)` → 한 번 더 재시작 (패치는 설정 파일이 생긴 뒤 기동부터 적용) |
| Java 오류 | 이미지 태그가 Java 25인지 (`docs/01-tech-stack.md`) |

`infra/dev/data/`, `.env`는 커밋하지 않는다.
