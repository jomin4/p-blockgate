#!/usr/bin/env node
// 훅 회귀 테스트 — 훅을 고치면 반드시 실행: node .claude/hooks/test-hooks.mjs
// 임시 git 레포를 만들어 실제 git 상태로 검사한다.
import { execFileSync, spawnSync } from 'node:child_process';
import { mkdtempSync, writeFileSync, mkdirSync, cpSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { evaluate } from './lib/guard.mjs';
import { validateHeader, validateBranch } from './lib/rules.mjs';

const here = dirname(fileURLToPath(import.meta.url));
const projectRoot = join(here, '..', '..');
let pass = 0; let fail = 0;
const ok = (name, cond, extra = '') => {
  if (cond) { pass++; } else { fail++; console.log(`✗ ${name} ${extra}`); }
};

// ---------- 규칙 단위 ----------
ok('header ok', validateHeader('feat(plugin): /home 명령어 추가') === null);
ok('header breaking ok', validateHeader('feat(plugin)!: 데이터 구조 변경') === null);
ok('header no area', validateHeader('feat: 추가') !== null);
ok('header bad area', validateHeader('feat(core): 추가') !== null);
ok('header bad type', validateHeader('feature(plugin): 추가') !== null);
ok('header period', validateHeader('fix(plugin): 수정.') !== null);
ok('header merge passthrough', validateHeader('Merge branch main') === null);
ok('branch ok', validateBranch('feat/12-home-command') === null);
ok('branch no issue', validateBranch('feat/home-command') !== null);
ok('branch upper', validateBranch('feat/12-Home') !== null);
ok('branch main', validateBranch('main') !== null);

// ---------- 임시 레포 ----------
const dir = mkdtempSync(join(tmpdir(), 'bg-hooks-'));
const remote = mkdtempSync(join(tmpdir(), 'bg-remote-'));
const g = (args, cwd = dir) => execFileSync('git', args, { cwd, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim();
g(['init', '--bare', '-b', 'main'], remote);
g(['init', '-b', 'main']);
g(['config', 'user.email', 't@t']); g(['config', 'user.name', 't']);
cpSync(join(projectRoot, '.githooks'), join(dir, '.githooks'), { recursive: true });
mkdirSync(join(dir, '.claude'), { recursive: true });
cpSync(join(projectRoot, '.claude', 'hooks'), join(dir, '.claude', 'hooks'), { recursive: true });
g(['config', 'core.hooksPath', '.githooks']);
g(['remote', 'add', 'origin', remote]);
writeFileSync(join(dir, 'a.txt'), 'a');
g(['add', '.']);

// commit-msg 훅
const badCommit = spawnSync('git', ['commit', '-m', 'initial'], { cwd: dir, encoding: 'utf8' });
ok('commit-msg blocks bad message', badCommit.status !== 0, badCommit.stderr);
g(['commit', '-m', 'chore(repo): 프로젝트 초기 구성']);

// guard: 최초 main push 허용, 원격 main 생기기 전에는 main 커밋 허용 (Phase 1)
ok('first push to main allowed', evaluate('git push -u origin main', dir).length === 0);
ok('commit on main allowed before first push', evaluate('git commit -m "chore(repo): x"', dir).length === 0);
// pre-push: 최초 main push 허용
const firstPush = spawnSync('git', ['push', '-u', 'origin', 'main'], { cwd: dir, encoding: 'utf8' });
ok('pre-push allows first main push', firstPush.status === 0, firstPush.stderr);
g(['fetch', 'origin']);

// 이후 main push 차단
writeFileSync(join(dir, 'b.txt'), 'b'); g(['add', '.']); g(['commit', '-m', 'chore(repo): b 추가']);
ok('guard blocks push main (implicit)', evaluate('git push', dir).length > 0);
ok('guard blocks push origin main', evaluate('git push origin main', dir).length > 0);
ok('guard blocks HEAD:main', evaluate('git push origin HEAD:main', dir).length > 0);
const secondPush = spawnSync('git', ['push', 'origin', 'main'], { cwd: dir, encoding: 'utf8' });
ok('pre-push blocks main push', secondPush.status !== 0);
g(['reset', '--hard', 'origin/main']);

// 기타 git 규칙
ok('blocks --no-verify commit', evaluate('git commit --no-verify -m "feat(plugin): x"', dir).length > 0);
ok('blocks -n commit', evaluate('git commit -n -m "feat(plugin): x"', dir).length > 0);
ok('blocks commit on main after first push', evaluate('git commit -m "chore(repo): x"', dir).some((m) => m.includes('main에서 직접 커밋')));
ok('blocks bad branch', evaluate('git checkout -b my-branch', dir).length > 0);
ok('allows good branch', evaluate('git switch -c feat/12-home-command', dir).length === 0);
ok('blocks bad git branch create', evaluate('git branch tmp', dir).length > 0);
ok('allows branch delete', evaluate('git branch -D feat/12-home-command', dir).length === 0);
ok('blocks worktree', evaluate('git worktree add ../x', dir).length > 0);
ok('blocks force push', evaluate('git push --force origin feat/12-a', dir).length > 0);
ok('blocks -f push', evaluate('git push -f', dir).length > 0);
ok('blocks hooksPath change', evaluate('git config core.hooksPath /dev/null', dir).length > 0);
ok('ignores text in heredoc', evaluate("cat <<'EOF' > note.md\ngit push origin main\nEOF", dir).length === 0);
ok('ignores text in quotes', evaluate('echo "git push --force origin main"', dir).length === 0);
ok('chained commands checked', evaluate('git add . && git commit --no-verify -m "x"', dir).length > 0);
ok('blocks terraform destroy', evaluate('cd infra && terraform destroy', dir).length > 0);

// ---------- gh pr create ----------
g(['switch', '-c', 'feat/12-home-command']);
ok('allows -am commit on feature branch', evaluate('git commit -am "feat(plugin): x"', dir).length === 0);
writeFileSync(join(dir, 'c.txt'), 'c'); g(['add', '.']); g(['commit', '-m', 'feat(plugin): 구현 먼저']);
const prCmd = 'gh pr create --base main --title "feat(plugin): /home 명령어 추가" --body "Refs #12"';
let r = evaluate(prCmd, dir);
ok('pr blocked without review marker', r.some((m) => m.includes('리뷰 통과 표시')));
ok('pr blocked TDD order', r.some((m) => m.includes('TDD 순서')));

// TDD 순서를 맞춘 브랜치
g(['switch', 'main']); g(['branch', '-D', 'feat/12-home-command']);
g(['switch', '-c', 'feat/12-home-command']);
writeFileSync(join(dir, 'T.txt'), 't'); g(['add', '.']); g(['commit', '-m', 'test(plugin): T1 실패 테스트']);
writeFileSync(join(dir, 'I.txt'), 'i'); g(['add', '.']); g(['commit', '-m', 'feat(plugin): T1 구현']);
r = evaluate(prCmd, dir);
ok('TDD order ok', !r.some((m) => m.includes('TDD 순서')), r.join(' | '));
execFileSync('node', [join(dir, '.claude', 'hooks', 'mark-review.mjs'), 'pass', 'spec ok', 'quality ok'], { cwd: dir });
r = evaluate(prCmd, dir);
ok('pr allowed with marker', r.length === 0, r.join(' | '));
ok('pr blocked bad title', evaluate('gh pr create --title "home 추가" --body "Refs #12"', dir).length > 0);
ok('pr blocked no Refs', evaluate('gh pr create --title "feat(plugin): 추가" --body "설명"', dir).some((m) => m.includes('Refs')));
ok('pr blocked Closes', evaluate('gh pr create --title "feat(plugin): 추가" --body "Refs #12 Closes #12"', dir).some((m) => m.includes('Closes')));
ok('pr blocked --fill', evaluate('gh pr create --fill', dir).length > 0);
mkdirSync(join(dir, '.claude', 'state'), { recursive: true });
writeFileSync(join(dir, '.claude', 'state', 'pr-body.md'), '## 요약\nRefs #12\n');
ok('pr body-file ok', evaluate('gh pr create --title "feat(plugin): 추가" --body-file .claude/state/pr-body.md', dir).length === 0);
const heredocPr = "gh pr create --title \"feat(plugin): 추가\" --body \"$(cat <<'EOF'\n요약\n\nRefs #12\nEOF\n)\"";
ok('pr heredoc body ok', evaluate(heredocPr, dir).length === 0, evaluate(heredocPr, dir).join(' | '));
// 리뷰 후 새 커밋 → 차단
writeFileSync(join(dir, 'J.txt'), 'j'); g(['add', '.']); g(['commit', '-m', 'refactor(plugin): 정리']);
ok('pr blocked when HEAD moved', evaluate(prCmd, dir).some((m) => m.includes('새 커밋')));

// gh pr merge
ok('merge requires squash', evaluate('gh pr merge 5 --auto', dir).length > 0);
ok('merge squash ok', evaluate('gh pr merge 5 --auto --squash', dir).length === 0);
ok('merge admin blocked', evaluate('gh pr merge 5 --squash --admin', dir).length > 0);

// 브랜치 push / 삭제 push
ok('push feature branch ok', evaluate('git push -u origin feat/12-home-command', dir).length === 0);
ok('force-with-lease on feature ok', evaluate('git push --force-with-lease', dir).length === 0);
ok('delete remote branch ok', evaluate('git push origin --delete feat/12-home-command', dir).length === 0);
const badPush = spawnSync('git', ['push', 'origin', 'HEAD:wip'], { cwd: dir, encoding: 'utf8' });
ok('pre-push blocks bad branch name', badPush.status !== 0);

// git-guard 실행 파일 (stdin JSON → exit code)
const run = (cmd) => spawnSync('node', [join(dir, '.claude', 'hooks', 'git-guard.mjs')], {
  input: JSON.stringify({ tool_name: 'Bash', tool_input: { command: cmd }, cwd: dir }), encoding: 'utf8',
});
ok('entry exit 2 on violation', run('git worktree add ../x').status === 2);
ok('entry exit 0 on ok', run('git status').status === 0);
ok('entry ignores non-Bash', spawnSync('node', [join(dir, '.claude', 'hooks', 'git-guard.mjs')], {
  input: JSON.stringify({ tool_name: 'Read', tool_input: {} }), encoding: 'utf8' }).status === 0);

rmSync(dir, { recursive: true, force: true });
rmSync(remote, { recursive: true, force: true });
console.log(`\n훅 테스트: ${pass} 통과, ${fail} 실패`);
process.exit(fail ? 1 : 0);
