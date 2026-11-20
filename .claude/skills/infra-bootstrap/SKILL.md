---
name: infra-bootstrap
description: BlockGate 최초 인프라 세팅을 진행할 때 사용. "인프라 세팅 시작하자", "다음 Phase 진행해", "세팅 어디까지 했어?" 같은 요청이나, docs/09-setup-roadmap.md 진행표에 미완료 Phase가 남아 있을 때 발동한다.
---

# 인프라 세팅 (Phase 진행)

기준 문서: `docs/09-setup-roadmap.md` (Phase별 작업·🔑 입력·👤 사용자 작업·✅ 완료 조건)

## 절차

1. **현황 파악**: 로드맵 맨 아래 "진행 현황" 표에서 첫 번째 ⬜/🔄 Phase를 찾는다.
2. **계획 보고** (실행 전): 그 Phase의 작업 목록, 필요한 🔑 값, 👤 사용자 작업을 표로 보여주고 "진행할까요?"라고 묻는다.
3. **작업 단위**
   - Phase 1: 레포가 없으므로 main에 초기 커밋을 직접 push한다 (훅이 최초 push만 허용).
   - Phase 2부터: `type:task` 이슈(`issue-create` 스킬, task 템플릿) → `chore/<issue#>-phase-<n>-<desc>` 브랜치 → 작업 → `review` 스킬 → `ship` 스킬(PR·머지).
4. **🔑 값 받기**: 필요한 시점에 한 번에 물어본다. 받은 값은
   - 지정 위치(`terraform.tfvars`, `infra/*/.env`, `gh secret set` stdin, `~/.oci/config`)에만 쓴다.
   - 채팅·커밋·PR·로그에 **다시 출력하지 않는다** (확인은 앞 4자리 + `…`).
5. **👤 작업 안내**: 웹 콘솔 경로를 단계별 번호 목록으로 안내하고, 완료 답을 받은 뒤 결과를 확인한다.
6. **되돌리기 어려운 작업**은 실행 직전에 다시 확인받는다: `terraform apply`, 리소스 삭제, Secrets 덮어쓰기, EULA 수락, Rulesets 변경.
7. **완료 확인**: ✅ 조건을 실제 명령으로 검증하고 결과를 표로 보고한다.
8. **기록**: 로드맵의 체크박스와 진행 현황(⬜ → ✅, PR 번호)을 같은 PR에서 갱신한다.
9. **다음 안내**: 다음 Phase 이름과 필요한 🔑/👤를 미리 알려준다.

## 지켜야 할 것
- Phase 순서를 건너뛰지 않는다. 앞 Phase의 ✅가 실패하면 멈추고 보고한다.
- 버전은 `docs/01-tech-stack.md` 고정표를 따른다. 새로 고정한 값(PAPER_BUILD, VitaminMCP 버전 등)은 문서에 기록한다.
- Oracle 무료 한도 밖 리소스, 25565 외 공개 포트, 운영 서버의 VitaminMCP는 어떤 이유로도 만들지 않는다.
- A1 용량 부족(`Out of host capacity`)은 몇 분 간격으로 재시도하고, 계속 실패하면 보고한다.
- 모든 Phase 완료 시 "기능 개발 단계로 전환되었습니다"라고 보고한다 (CLAUDE.md 2장).
