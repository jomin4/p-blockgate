// 훅에서 쓰는 작은 git 헬퍼. 실패하면 null을 돌려준다 (훅이 git 오류로 죽지 않게).
import { execFileSync } from 'node:child_process';

export function git(args, cwd = process.cwd()) {
  try {
    return execFileSync('git', args, { cwd, encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim();
  } catch {
    return null;
  }
}

export function isGitRepo(cwd) {
  return git(['rev-parse', '--is-inside-work-tree'], cwd) === 'true';
}

export function currentBranch(cwd) {
  // 커밋이 아직 없는 레포에서도 동작하도록 symbolic-ref를 먼저 시도
  return git(['symbolic-ref', '--short', 'HEAD'], cwd) ?? git(['rev-parse', '--abbrev-ref', 'HEAD'], cwd);
}

export function headSha(cwd) {
  return git(['rev-parse', 'HEAD'], cwd);
}

export function repoRoot(cwd) {
  return git(['rev-parse', '--show-toplevel'], cwd);
}

/** origin/main이 로컬에 알려져 있는가 (= 원격에 main이 이미 존재) */
export function remoteMainExists(cwd) {
  return git(['rev-parse', '--verify', '--quiet', 'refs/remotes/origin/main'], cwd) !== null;
}

/** 브랜치에만 있는 커밋 제목들 (오래된 것부터) */
export function branchCommitSubjects(cwd) {
  for (const base of ['origin/main', 'main']) {
    if (git(['rev-parse', '--verify', '--quiet', base], cwd) === null) continue;
    const out = git(['log', '--reverse', '--format=%s', `${base}..HEAD`], cwd);
    if (out === null) continue;
    return out ? out.split('\n') : [];
  }
  return null;
}
