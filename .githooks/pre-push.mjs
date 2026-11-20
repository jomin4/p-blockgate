// pre-push 훅 — main 직접 push 차단(원격에 main이 없을 때의 최초 생성만 허용), 브랜치 이름 검사.
import { readFileSync } from 'node:fs';
import { validateBranch } from '../.claude/hooks/lib/rules.mjs';

const ZERO = /^0+$/;
let input = '';
try { input = readFileSync(0, 'utf8'); } catch { /* 없음 */ }

const errors = [];
for (const line of input.split(/\r?\n/).filter(Boolean)) {
  const [localRef, localSha, remoteRef, remoteSha] = line.split(' ');
  if (!remoteRef?.startsWith('refs/heads/')) continue; // 태그 등은 통과
  const name = remoteRef.slice('refs/heads/'.length);
  const deleting = ZERO.test(localSha ?? '');
  if (name === 'main') {
    if (deleting) errors.push('main 브랜치는 삭제할 수 없습니다.');
    else if (!ZERO.test(remoteSha ?? '')) errors.push('main으로 직접 push할 수 없습니다. 작업 브랜치 → PR로 반영하세요.');
    // remoteSha가 0 = 원격에 main이 없음 = 최초 push → 허용
    continue;
  }
  if (deleting) continue; // 작업 브랜치 삭제는 허용
  const err = validateBranch(name);
  if (err) errors.push(err);
  void localRef;
}

if (errors.length) {
  process.stderr.write(`[BlockGate pre-push] push를 막았습니다.\n- ${errors.join('\n- ')}\n`);
  process.exit(1);
}
