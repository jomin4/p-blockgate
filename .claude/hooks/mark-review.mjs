#!/usr/bin/env node
// review 스킬이 두 리뷰어(spec → quality)를 모두 통과한 뒤에만 실행한다.
// 사용: node .claude/hooks/mark-review.mjs pass "<spec 요약>" "<quality 요약>"
//       node .claude/hooks/mark-review.mjs clear
// 결과: .claude/state/review-<branch>.json  (gh pr create 훅이 HEAD와 일치하는지 검사)
import { mkdirSync, writeFileSync, rmSync } from 'node:fs';
import { join } from 'node:path';
import { currentBranch, headSha, repoRoot } from './lib/git.mjs';
import { reviewMarkerName } from './lib/rules.mjs';

const [mode = 'pass', spec = '', quality = ''] = process.argv.slice(2);
const root = repoRoot(process.cwd()) ?? process.cwd();
const branch = currentBranch(root);
if (!branch || branch === 'main') {
  console.error('작업 브랜치에서 실행하세요.');
  process.exit(1);
}
const dir = join(root, '.claude', 'state');
const file = join(dir, reviewMarkerName(branch));

if (mode === 'clear') {
  rmSync(file, { force: true });
  console.log(`리뷰 표시 삭제: ${file}`);
  process.exit(0);
}
if (mode !== 'pass') {
  console.error('mode는 pass 또는 clear');
  process.exit(1);
}
mkdirSync(dir, { recursive: true });
const record = { branch, head: headSha(root), result: 'pass', spec, quality, at: new Date().toISOString() };
writeFileSync(file, JSON.stringify(record, null, 2));
console.log(`리뷰 통과 기록: ${branch} @ ${record.head?.slice(0, 7)}`);
