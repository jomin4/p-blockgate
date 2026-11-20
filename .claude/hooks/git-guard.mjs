#!/usr/bin/env node
// PreToolUse(Bash) 훅 — BlockGate Git·GitHub 규칙을 명령 실행 전에 강제한다.
// 차단 시 exit 2 + stderr 메시지 (Claude Code가 이 메시지를 Claude에게 전달한다).
// 훅 자체에 오류가 나면 작업을 막지 않도록 exit 0 으로 통과시킨다.
import { readFileSync } from 'node:fs';
import { evaluate } from './lib/guard.mjs';

try {
  let raw = '';
  try { raw = readFileSync(0, 'utf8'); } catch { /* stdin 없음 */ }
  const input = JSON.parse(raw || '{}');
  if (input.tool_name !== 'Bash') process.exit(0);
  const command = input.tool_input?.command ?? '';
  const cwd = input.cwd || process.env.CLAUDE_PROJECT_DIR || process.cwd();
  const found = evaluate(command, cwd);
  if (found.length) {
    process.stderr.write(
      '[BlockGate git-guard] 규칙 위반으로 명령을 막았습니다:\n- ' + found.join('\n- ') +
      '\n(규칙: CLAUDE.md 6장, docs/05-git-github.md — 훅을 우회하지 말고 원인을 고치세요.)\n');
    process.exit(2);
  }
  process.exit(0);
} catch (e) {
  process.stderr.write(`[BlockGate git-guard] 훅 내부 오류로 검사를 건너뜀: ${e.message}\n`);
  process.exit(0);
}
