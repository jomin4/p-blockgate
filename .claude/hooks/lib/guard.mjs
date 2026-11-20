// git-guard 검사 로직 (git-guard.mjs가 실행, test-hooks.mjs가 테스트).

import { readFileSync, existsSync } from 'node:fs';
import { join, isAbsolute } from 'node:path';
import { validateHeader, validateBranch, branchType, reviewMarkerName } from './rules.mjs';
import { isGitRepo, currentBranch, headSha, remoteMainExists, branchCommitSubjects, repoRoot } from './git.mjs';

const problems = [];
const block = (msg) => problems.push(msg);

/** heredoc 본문과 따옴표 문자열을 지워 "명령 구조"만 남긴다 (본문 속 'git push' 같은 글자에 오탐하지 않게). */
export function stripLiterals(cmd) {
  let s = cmd.replace(/<<-?\s*(['"]?)([A-Za-z_][A-Za-z0-9_]*)\1[^\n]*\n[\s\S]*?\n[ \t]*\2[ \t]*(?=\n|$)/g, '<<HEREDOC');
  s = s.replace(/'[^']*'/g, "''");
  s = s.replace(/"(?:[^"\\]|\\.)*"/g, '""');
  s = s.replace(/\$\((?:[^()]|\([^()]*\))*\)/g, '$()');
  return s;
}

/** 명령을 &&, ||, ;, |, 줄바꿈 단위로 나누고 앞쪽 환경변수 대입을 제거한다. */
export function segments(stripped) {
  return stripped
    .split(/&&|\|\||;|\||\n/)
    .map((seg) => seg.trim().split(/\s+/).filter(Boolean))
    .map((tokens) => {
      while (tokens.length && /^[A-Za-z_][A-Za-z0-9_]*=/.test(tokens[0])) tokens.shift();
      if (tokens[0] === 'sudo' || tokens[0] === 'command' || tokens[0] === 'exec') tokens.shift();
      return tokens;
    })
    .filter((t) => t.length);
}

/** git 전역 옵션(-C, -c, --no-pager 등)을 건너뛰고 [서브커맨드, 나머지]를 돌려준다. */
function gitSub(tokens) {
  let i = 1;
  while (i < tokens.length) {
    const t = tokens[i];
    if (t === '-C' || t === '-c' || t === '--git-dir' || t === '--work-tree') { i += 2; continue; }
    if (t.startsWith('-')) { i += 1; continue; }
    break;
  }
  return [tokens[i], tokens.slice(i + 1)];
}

/** 원본 명령에서 옵션 값을 뽑는다 (따옴표 포함). */
function optionValue(original, names) {
  for (const name of names) {
    const re = new RegExp(`(?:^|\\s)${name}(?:=|\\s+)("((?:[^"\\\\]|\\\\.)*)"|'([^']*)'|(\\S+))`);
    const m = re.exec(original);
    if (m) return m[2] ?? m[3] ?? m[4] ?? '';
  }
  return null;
}

function checkGit(tokens, cwd) {
  const [sub, rest] = gitSub(tokens);
  if (!sub) return;

  if (sub === 'commit') {
    if (rest.includes('--no-verify') || rest.some((t) => /^-[a-zA-Z]*n[a-zA-Z]*$/.test(t))) {
      block('git commit --no-verify(-n)는 금지입니다. 커밋 메시지 훅이 막는 원인을 고치세요.');
    }
    if (currentBranch(cwd) === 'main' && remoteMainExists(cwd)) {
      block('main에서 직접 커밋하지 않습니다. 작업 브랜치(<type>/<이슈번호>-<설명>)를 만든 뒤 커밋하세요.');
    }
  }

  if (sub === 'push') {
    if (rest.includes('--no-verify')) block('git push --no-verify는 금지입니다.');
    if (rest.includes('--force') || rest.includes('-f') || rest.some((t) => /^-[a-zA-Z]*f[a-zA-Z]*$/.test(t) && !t.startsWith('--'))) {
      block('git push --force/-f는 금지입니다. rebase 후에는 작업 브랜치에서만 --force-with-lease를 사용하세요.');
    }
    if (rest.includes('--delete') || rest.includes('-d')) {
      if (rest.some((t) => t === 'main' || t.endsWith(':main'))) block('main 브랜치는 삭제할 수 없습니다.');
      return;
    }
    const refs = rest.filter((t) => !t.startsWith('-'));
    const targetsMain = refs.slice(1).some((r) => r === 'main' || /(^|:)(refs\/heads\/)?main$/.test(r) || r === 'HEAD:main');
    const branch = currentBranch(cwd);
    const implicitMain = refs.length <= 1 && branch === 'main';
    if (targetsMain || implicitMain) {
      const firstPush = !remoteMainExists(cwd);
      if (firstPush && branch === 'main') return; // Phase 1 최초 push만 허용
      block('main으로 직접 push할 수 없습니다. 작업 브랜치 → PR → auto-merge(Squash)로 반영하세요.');
    }
    if (rest.includes('--force-with-lease') && (branch === 'main' || targetsMain)) {
      block('main에는 force push를 할 수 없습니다.');
    }
  }

  if (sub === 'checkout' || sub === 'switch') {
    const flagIdx = rest.findIndex((t) => (sub === 'checkout' ? ['-b', '-B'] : ['-c', '-C', '--create', '--force-create']).includes(t));
    if (flagIdx >= 0) {
      const name = rest[flagIdx + 1];
      const err = validateBranch(name);
      if (err) block(err);
    }
  }

  if (sub === 'branch') {
    const nonCreate = ['-d', '-D', '--delete', '-l', '--list', '-a', '-r', '--all', '--remotes', '-v', '-vv',
      '--show-current', '--merged', '--no-merged', '--contains', '--no-contains', '-u', '--set-upstream-to',
      '--unset-upstream', '--edit-description', '--format', '--sort', '--points-at'];
    if (rest.some((t) => nonCreate.includes(t) || t.startsWith('--format=') || t.startsWith('--sort=') || t.startsWith('--set-upstream-to='))) return;
    const renameIdx = rest.findIndex((t) => t === '-m' || t === '-M');
    const positional = rest.filter((t) => !t.startsWith('-'));
    if (renameIdx >= 0) {
      const newName = positional[positional.length - 1];
      const err = validateBranch(newName);
      if (err) block(err);
      return;
    }
    if (positional.length) {
      const err = validateBranch(positional[0]);
      if (err) block(err);
    }
  }

  if (sub === 'worktree') block('worktree는 사용하지 않습니다. 일반 브랜치로 작업하세요.');

  if (sub === 'config' && rest.includes('core.hooksPath')) {
    const val = rest[rest.indexOf('core.hooksPath') + 1];
    if (val && val !== '.githooks') block('core.hooksPath는 .githooks 로 고정입니다.');
    if (rest.includes('--unset')) block('core.hooksPath를 해제할 수 없습니다.');
  }
}

function checkPrCreate(tokens, original, cwd) {
  if (tokens.some((t) => ['--fill', '--fill-first', '--fill-verbose', '-f'].includes(t))) {
    block('gh pr create --fill 대신 --title 과 --body-file(또는 --body)을 명시하세요.');
  }
  const title = optionValue(original, ['--title', '-t']);
  if (title === null) {
    block('PR 제목(--title)이 필요합니다. 형식: <type>(<영역키>): <제목>');
  } else {
    const err = validateHeader(title);
    if (err) block(`PR 제목 ${err}`);
  }

  // 본문은 --body, heredoc, --body-file 어느 방식이든 검사한다 (명령 전체 + 본문 파일 내용)
  let body = original;
  const bodyFile = optionValue(original, ['--body-file', '-F']);
  if (bodyFile && bodyFile !== '-') {
    const p = isAbsolute(bodyFile) ? bodyFile : join(cwd, bodyFile);
    try { body += '\n' + readFileSync(p, 'utf8'); } catch { block(`PR 본문 파일을 읽을 수 없습니다: ${bodyFile}`); }
  }
  if (!/Refs\s+#\d+/.test(body)) block('PR 본문에 "Refs #<이슈번호>"가 필요합니다.');
  if (/\b(close[sd]?|fix(e[sd])?|resolve[sd]?)\s+#\d+/i.test(body)) {
    block('Closes/Fixes/Resolves #N 은 쓰지 않습니다 (이슈는 G3 승인 후 닫음). "Refs #N"을 사용하세요.');
  }

  if (!isGitRepo(cwd)) return;
  const branch = currentBranch(cwd);
  if (branch === 'main') { block('main에서는 PR을 만들 수 없습니다.'); return; }
  const branchErr = validateBranch(branch);
  if (branchErr) block(branchErr);

  // 로컬 리뷰 통과 표시 확인
  const root = repoRoot(cwd) ?? cwd;
  const marker = join(root, '.claude', 'state', reviewMarkerName(branch));
  if (!existsSync(marker)) {
    block(`로컬 리뷰 통과 표시가 없습니다 (${marker}). review 스킬로 spec-reviewer → quality-reviewer 리뷰를 먼저 통과하세요.`);
  } else {
    try {
      const m = JSON.parse(readFileSync(marker, 'utf8'));
      const head = headSha(cwd);
      if (m.result !== 'pass') block('리뷰 결과가 pass가 아닙니다. review 스킬을 다시 진행하세요.');
      else if (m.head !== head) block('리뷰 이후 새 커밋이 있습니다 (리뷰한 커밋 ≠ HEAD). 변경분에 대해 review 스킬을 다시 진행하세요.');
    } catch {
      block('리뷰 통과 표시 파일을 읽을 수 없습니다. review 스킬을 다시 진행하세요.');
    }
  }

  // TDD 순서 확인 (feat/fix 브랜치)
  const type = branchType(branch);
  if (type === 'feat' || type === 'fix') {
    const subjects = branchCommitSubjects(cwd);
    if (subjects) {
      const firstTest = subjects.findIndex((s) => /^test\(/.test(s));
      const firstImpl = subjects.findIndex((s) => /^(feat|fix)\(/.test(s));
      if (firstImpl >= 0 && (firstTest < 0 || firstTest > firstImpl)) {
        block('TDD 순서 위반: 첫 feat/fix 커밋보다 먼저 test(...) 커밋(Red)이 있어야 합니다.');
      }
    }
  }
}

function checkGh(tokens, original, cwd) {
  const [, group, action] = tokens;
  if (group === 'pr' && action === 'create') checkPrCreate(tokens, original, cwd);
  if (group === 'pr' && action === 'merge') {
    if (!tokens.includes('--squash') && !tokens.includes('-s')) block('gh pr merge는 --squash로만 합니다 (권장: --auto --squash).');
    if (tokens.some((t) => ['--merge', '-m', '--rebase', '-r', '--admin'].includes(t))) {
      block('gh pr merge에서 --merge/--rebase/--admin은 금지입니다.');
    }
  }
  if (group === 'repo' && action === 'delete') block('gh repo delete는 금지입니다.');
}

function checkOther(tokens) {
  if (tokens[0] === 'terraform' && tokens.includes('destroy')) {
    block('terraform destroy는 Claude가 실행하지 않습니다. 필요하면 개발자에게 이유를 설명하고 직접 실행하도록 안내하세요.');
  }
  if (tokens[0] === 'rm' && tokens.some((t) => /(^|\/)\.git(\/|$)/.test(t))) block('.git 디렉터리는 삭제할 수 없습니다.');
}

export function evaluate(command, cwd) {
  problems.length = 0;
  const stripped = stripLiterals(command);
  for (const tokens of segments(stripped)) {
    if (tokens[0] === 'git') checkGit(tokens, cwd);
    else if (tokens[0] === 'gh') checkGh(tokens, command, cwd);
    else checkOther(tokens);
  }
  return [...problems];
}

