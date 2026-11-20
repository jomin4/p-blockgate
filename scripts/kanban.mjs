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
  let cfg;
  try {
    cfg = JSON.parse(readFileSync(join(root, '.github', 'project.json'), 'utf8'));
  } catch (e) {
    fail(`.github/project.json 을 읽을 수 없습니다: ${e.message}`);
  }
  const ok = cfg && typeof cfg.owner === 'string' && /^[^/]+\/[^/]+$/.test(cfg.repo ?? '')
    && Number.isInteger(cfg.number) && typeof cfg.id === 'string'
    && typeof cfg.statusField?.id === 'string' && cfg.statusField.options && typeof cfg.statusField.options === 'object';
  if (!ok) fail('.github/project.json 형식이 올바르지 않습니다 (owner, repo, number, id, statusField.id, statusField.options 필요).');
  return cfg;
}

/** Status 이름 → 옵션 ID. 프로토타입 키(toString 등)는 받지 않는다. */
function optionIdOf(cfg, status) {
  const opts = cfg.statusField.options;
  if (typeof status !== 'string' || !Object.hasOwn(opts, status)) {
    fail(`알 수 없는 Status: "${status ?? ''}" (가능: ${Object.keys(opts).join(' / ')})`);
  }
  return opts[status];
}

function sleep(ms) {
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms);
}

function gh(args) {
  try {
    return execFileSync('gh', args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] });
  } catch (e) {
    const detail = (e.stderr || e.message || '').toString().trim();
    fail(`gh ${args.slice(0, 2).join(' ')} 실패: ${detail}`);
  }
}

/** gh 실행 + JSON 파싱. 출력이 JSON이 아니면 원인 메시지로 실패. */
function ghJson(args) {
  const out = gh(args);
  try {
    return JSON.parse(out);
  } catch {
    fail(`gh ${args.slice(0, 2).join(' ')} 출력이 JSON이 아닙니다: ${out.slice(0, 200)}`);
  }
}

function parseIssue(raw) {
  const m = /^#?(\d+)$/.exec(String(raw ?? '').trim());
  const n = m ? Number(m[1]) : NaN;
  if (!Number.isSafeInteger(n) || n <= 0) fail(`이슈 번호가 올바르지 않습니다: "${raw ?? ''}"`);
  return n;
}

/** 이슈의 이 프로젝트 아이템 { itemId, status } 또는 null. */
function findItem(cfg, issue) {
  const [owner, name] = cfg.repo.split('/');
  const query = `query($owner:String!,$name:String!,$num:Int!){
    repository(owner:$owner,name:$name){ issue(number:$num){
      projectItems(first:50){ nodes{ id project{ id }
        fieldValueByName(name:"Status"){ ... on ProjectV2ItemFieldSingleSelectValue{ name } } } } } } }`;
  const res = ghJson(['api', 'graphql', '-f', `query=${query}`,
    '-f', `owner=${owner}`, '-f', `name=${name}`, '-F', `num=${issue}`]);
  const data = res?.data?.repository?.issue;
  if (!data) fail(`#${issue} 이슈를 ${cfg.repo}에서 찾을 수 없습니다.`);
  const node = (data.projectItems?.nodes ?? []).find((n) => n?.project?.id === cfg.id);
  return node ? { itemId: node.id, status: node.fieldValueByName?.name ?? null } : null;
}

function addItem(cfg, issue) {
  const url = `https://github.com/${cfg.repo}/issues/${issue}`;
  const id = ghJson(['project', 'item-add', String(cfg.number), '--owner', cfg.owner, '--url', url, '--format', 'json'])?.id;
  if (!id) fail(`#${issue} 을 프로젝트에 추가하지 못했습니다.`);
  return id;
}

function setStatus(cfg, itemId, status) {
  const optionId = optionIdOf(cfg, status);
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
  optionIdOf(cfg, target); // 보드를 바꾸기 전에 검증
  const existing = findItem(cfg, issue);
  const itemId = existing?.itemId ?? addItem(cfg, issue);
  setStatus(cfg, itemId, target);
  // 새로 추가한 경우 내장 Workflow "Item added → Todo"가 비동기로 Status를 덮을 수 있다 → 확인 후 한 번 더 설정
  if (!existing && target !== 'Todo') {
    sleep(3000);
    if (findItem(cfg, issue)?.status !== target) setStatus(cfg, itemId, target);
  }
  console.log(`#${issue} → ${target}`);
}
