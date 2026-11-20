#!/usr/bin/env node
// BlockGate 칸반 도구 — GitHub Projects의 Status를 gh CLI로 바꾼다.
//   node scripts/kanban.mjs add  <이슈번호>             프로젝트에 추가 + Status = Todo
//   node scripts/kanban.mjs move <이슈번호> "<Status>"  Status 변경 (없으면 먼저 추가)
//   node scripts/kanban.mjs show <이슈번호>             현재 Status 출력
// 프로젝트 정보는 .github/project.json 에서 읽는다. 실패 시 exit 1.
import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');

function fail(msg) {
  process.stderr.write(`kanban: ${msg}\n`);
  process.exit(1);
}

function loadConfig() {
  try {
    return JSON.parse(readFileSync(join(root, '.github', 'project.json'), 'utf8'));
  } catch (e) {
    fail(`.github/project.json 을 읽을 수 없습니다: ${e.message}`);
  }
}

function gh(args) {
  try {
    return execFileSync('gh', args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] });
  } catch (e) {
    const detail = (e.stderr || e.message || '').toString().trim();
    fail(`gh ${args.slice(0, 2).join(' ')} 실패: ${detail}`);
  }
}

function parseIssue(raw) {
  const n = Number(String(raw ?? '').replace(/^#/, ''));
  if (!Number.isInteger(n) || n <= 0) fail(`이슈 번호가 올바르지 않습니다: "${raw ?? ''}"`);
  return n;
}

/** 이슈의 이 프로젝트 아이템 { itemId, status } 또는 null. */
function findItem(cfg, issue) {
  const [owner, name] = cfg.repo.split('/');
  const query = `query($owner:String!,$name:String!,$num:Int!){
    repository(owner:$owner,name:$name){ issue(number:$num){
      projectItems(first:50){ nodes{ id project{ id }
        fieldValueByName(name:"Status"){ ... on ProjectV2ItemFieldSingleSelectValue{ name } } } } } } }`;
  const out = gh(['api', 'graphql', '-f', `query=${query}`,
    '-F', `owner=${owner}`, '-F', `name=${name}`, '-F', `num=${issue}`]);
  const data = JSON.parse(out).data?.repository?.issue;
  if (!data) fail(`#${issue} 이슈를 ${cfg.repo}에서 찾을 수 없습니다.`);
  const node = data.projectItems.nodes.find((n) => n.project.id === cfg.id);
  return node ? { itemId: node.id, status: node.fieldValueByName?.name ?? null } : null;
}

function addItem(cfg, issue) {
  const url = `https://github.com/${cfg.repo}/issues/${issue}`;
  const out = gh(['project', 'item-add', String(cfg.number), '--owner', cfg.owner, '--url', url, '--format', 'json']);
  const id = JSON.parse(out).id;
  if (!id) fail(`#${issue} 을 프로젝트에 추가하지 못했습니다.`);
  return id;
}

function setStatus(cfg, itemId, status) {
  const optionId = cfg.statusField.options[status];
  if (!optionId) {
    fail(`알 수 없는 Status: "${status}" (가능: ${Object.keys(cfg.statusField.options).join(' / ')})`);
  }
  gh(['project', 'item-edit', '--id', itemId, '--project-id', cfg.id,
    '--field-id', cfg.statusField.id, '--single-select-option-id', optionId]);
}

const [cmd, rawIssue, status] = process.argv.slice(2);
const usage = '사용법: node scripts/kanban.mjs add <이슈번호> | move <이슈번호> "<Status>" | show <이슈번호>';

if (!['add', 'move', 'show'].includes(cmd)) fail(usage);
const cfg = loadConfig();
const issue = parseIssue(rawIssue);

if (cmd === 'show') {
  const item = findItem(cfg, issue);
  console.log(item ? `#${issue}: ${item.status ?? '(Status 없음)'}` : `#${issue}: 프로젝트에 없음`);
} else {
  const target = cmd === 'add' ? 'Todo' : status;
  if (!target) fail(usage);
  if (!cfg.statusField.options[target]) {
    fail(`알 수 없는 Status: "${target}" (가능: ${Object.keys(cfg.statusField.options).join(' / ')})`);
  }
  const existing = findItem(cfg, issue);
  const itemId = existing?.itemId ?? addItem(cfg, issue);
  setStatus(cfg, itemId, target);
  console.log(`#${issue} → ${target}`);
}
