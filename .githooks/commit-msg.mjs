// commit-msg 훅 — 커밋 제목이 BlockGate 형식인지 검사한다.
import { readFileSync } from 'node:fs';
import { validateHeader } from '../.claude/hooks/lib/rules.mjs';

const file = process.argv[2];
const text = readFileSync(file, 'utf8');
const header = text.split(/\r?\n/).find((l) => l.trim() && !l.startsWith('#')) ?? '';
const err = validateHeader(header);
if (err) {
  process.stderr.write(`[BlockGate commit-msg] 커밋을 막았습니다.\n${err}\n(규칙: docs/05-git-github.md 2장)\n`);
  process.exit(1);
}
