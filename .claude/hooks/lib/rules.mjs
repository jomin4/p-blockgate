// BlockGate 공통 규칙 — Claude 훅(.claude/hooks)과 Git 훅(.githooks)이 함께 사용한다.
// 규칙을 바꾸면 docs/05-git-github.md 도 같은 PR에서 갱신하고 test-hooks.mjs 를 통과시킨다.

export const COMMIT_TYPES = ['feat', 'fix', 'test', 'refactor', 'docs', 'chore', 'ci', 'build', 'perf', 'revert'];
export const BRANCH_TYPES = ['feat', 'fix', 'test', 'refactor', 'docs', 'chore', 'ci', 'build', 'perf'];
export const AREAS = ['plugin', 'server', 'infra', 'repo', 'docs'];

const HEADER_RE = new RegExp(`^(${COMMIT_TYPES.join('|')})\\((${AREAS.join('|')})\\)(!)?: (.+)$`);
const BRANCH_RE = new RegExp(`^(${BRANCH_TYPES.join('|')})/(\\d+)-[a-z0-9]+(?:-[a-z0-9]+)*$`);
const PASSTHROUGH_RE = /^(Merge |Revert "|fixup! |squash! |amend! )/;

export const COMMIT_EXAMPLE = 'feat(plugin): /home 명령어 추가';
export const BRANCH_EXAMPLE = 'feat/12-home-command';

/** 커밋·PR 제목 검사. 문제가 없으면 null, 있으면 사유 문자열. */
export function validateHeader(header) {
  const h = (header ?? '').trim();
  if (!h) return '제목이 비어 있습니다.';
  if (PASSTHROUGH_RE.test(h)) return null;
  const m = HEADER_RE.exec(h);
  if (!m) {
    return `형식이 맞지 않습니다: "${h}"\n` +
      `  형식: <type>(<영역키>): <제목>\n` +
      `  type: ${COMMIT_TYPES.join(', ')}\n` +
      `  영역키: ${AREAS.join(', ')}\n` +
      `  예: ${COMMIT_EXAMPLE}`;
  }
  const subject = m[4];
  if (!subject.trim()) return '제목 내용이 비어 있습니다.';
  if (/[.。]$/.test(subject)) return '제목 끝에 마침표를 붙이지 않습니다.';
  if ([...h].length > 100) return '제목이 너무 깁니다 (100자 이내, 권장 72자).';
  return null;
}

/** 브랜치 이름 검사. 문제가 없으면 null. */
export function validateBranch(name) {
  if (!name) return '브랜치 이름이 비어 있습니다.';
  if (name === 'main') return 'main 브랜치를 새로 만들거나 이 이름으로 작업할 수 없습니다.';
  if (!BRANCH_RE.test(name)) {
    return `브랜치 이름 형식이 맞지 않습니다: "${name}"\n` +
      `  형식: <type>/<이슈번호>-<설명> (설명은 영문 소문자·숫자·하이픈)\n` +
      `  type: ${BRANCH_TYPES.join(', ')}\n` +
      `  예: ${BRANCH_EXAMPLE}`;
  }
  return null;
}

/** 브랜치 이름에서 type 추출 (feat/12-x → feat). */
export function branchType(name) {
  const m = BRANCH_RE.exec(name ?? '');
  return m ? m[1] : null;
}

/** 리뷰 통과 표시 파일 이름 (브랜치의 / 를 _ 로). */
export function reviewMarkerName(branch) {
  return `review-${String(branch).replace(/[\\/]/g, '_')}.json`;
}
