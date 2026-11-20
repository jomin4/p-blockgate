#!/usr/bin/env node
// SessionStart 훅 — 모든 개발자 PC의 git 설정을 맞추고, 현재 단계를 Claude에게 알려준다.
// stdout 내용은 세션 컨텍스트에 추가된다. 어떤 경우에도 세션을 막지 않는다(exit 0).
import { readFileSync, existsSync, chmodSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import { git, isGitRepo, currentBranch } from './lib/git.mjs';

const root = process.env.CLAUDE_PROJECT_DIR || process.cwd();
const lines = ['[BlockGate] 세션 시작 점검'];

try {
  // 1) git 설정 통일
  if (isGitRepo(root)) {
    const want = { 'core.hooksPath': '.githooks', 'pull.rebase': 'true', 'fetch.prune': 'true' };
    const changed = [];
    for (const [k, v] of Object.entries(want)) {
      if (git(['config', '--local', '--get', k], root) !== v) {
        git(['config', '--local', k, v], root);
        changed.push(`${k}=${v}`);
      }
    }
    if (changed.length) lines.push(`- git 설정 적용: ${changed.join(', ')}`);
    lines.push(`- 현재 브랜치: ${currentBranch(root) ?? '(알 수 없음)'}`);
  } else {
    lines.push('- 아직 git 레포가 아닙니다 → infra-bootstrap Phase 1에서 초기화합니다.');
  }

  // 2) git 훅 실행 권한 (macOS/Linux)
  if (process.platform !== 'win32') {
    const dir = join(root, '.githooks');
    if (existsSync(dir)) {
      for (const f of readdirSync(dir)) {
        if (!f.includes('.')) { try { chmodSync(join(dir, f), 0o755); } catch { /* 무시 */ } }
      }
    }
  }

  // 3) 현재 단계 안내 (세팅 로드맵 진행표)
  const roadmap = join(root, 'docs', '09-setup-roadmap.md');
  if (existsSync(roadmap)) {
    const text = readFileSync(roadmap, 'utf8');
    const rows = text.split('\n').filter((l) => /^\|\s*\d\.\s/.test(l));
    const pending = rows.filter((l) => l.includes('⬜') || l.includes('🔄'));
    if (pending.length) {
      const next = pending[0].split('|')[1].trim();
      lines.push(`- 단계: 인프라 세팅 단계 (다음: Phase ${next}) → infra-bootstrap 스킬을 따르세요.`);
    } else {
      lines.push('- 단계: 기능 개발 단계 → CLAUDE.md 3장 워크플로우를 따르세요.');
    }
  }
} catch (e) {
  lines.push(`- 점검 중 오류(무시하고 진행): ${e.message}`);
}

process.stdout.write(lines.join('\n') + '\n');
process.exit(0);
