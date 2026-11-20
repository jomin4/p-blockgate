# scripts

## `kanban.mjs` (Phase 1에서 구현)

GitHub Projects 칸반 상태를 바꾸는 도구. 스킬들이 이 인터페이스로 호출한다.

| 명령 | 동작 |
|---|---|
| `node scripts/kanban.mjs add <이슈번호>` | 이슈를 프로젝트에 추가하고 Status = Todo |
| `node scripts/kanban.mjs move <이슈번호> "<Status>"` | Status 변경 (Todo / In Progress / Playtest / Done) |
| `node scripts/kanban.mjs show <이슈번호>` | 현재 Status 출력 |

### 구현 명세
- `gh` CLI만 사용 (`gh project item-add`, `gh project item-list --format json`, `gh project item-edit`).
- 프로젝트 번호·ID·Status 필드 ID·옵션 ID는 `.github/project.json`에서 읽는다 (Phase 1에서 생성·커밋).
- 이슈가 프로젝트에 없으면 `move`가 먼저 추가한다.
- 실패 시 exit 1 + 원인 메시지. 성공 시 한 줄 출력 (`#12 → In Progress`).
- Done은 보통 "Item closed → Done" 자동화로 이동한다 (`move ... Done`은 예외 상황용).
