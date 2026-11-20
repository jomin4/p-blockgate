# 06. CI/CD 명세

> Phase 7에서 Claude Code가 이 명세대로 `.github/workflows/`를 작성한다.

## 1. 워크플로 목록

| 파일 | 트리거 | 대상 경로 | Job (필수 체크 이름) |
|---|---|---|---|
| `plugin-ci.yml` | pull_request → main | `plugins/**` | `plugin-ci-build`, `plugin-ci-test` |
| `plugin-cd.yml` | push → main | `plugins/**` | `plugin-cd-build`, `plugin-cd-deploy`, `plugin-cd-verify` |
| `server-ci.yml` | pull_request → main | `infra/prod/server/**` | `server-ci-validate` |
| `server-cd.yml` | push → main | `infra/prod/server/**` | `server-cd-deploy`, `server-cd-verify` |
| `infra-ci.yml` | pull_request → main | `infra/prod/terraform/**` | `infra-ci-validate`, `infra-ci-plan` |
| `infra-cd.yml` | push → main | `infra/prod/terraform/**` | `infra-cd-apply` |

- Repo CI는 두지 않는다.
- Job 이름 규칙: `<영역>-<ci|cd>-<단계>` (레포 전체에서 유일).

## 2. 경로 감지 방식 (필수 체크가 멈추지 않게)

| 규칙 | 내용 |
|---|---|
| 워크플로 레벨 `paths:` 필터 | **CI에는 쓰지 않는다** (필터로 안 돌면 필수 체크가 pending으로 남음) |
| 대신 | job 첫 step에서 변경 경로 감지(`dorny/paths-filter` 등) → 무관하면 이후 step을 건너뛰고 job은 **성공** 종료 |
| CD | 워크플로 레벨 `paths:` 필터 사용 가능 (필수 체크 아님) |

## 3. CI 상세

| Job | 단계 |
|---|---|
| `plugin-ci-build` | JDK 25(Temurin) → Gradle 캐시 → `./gradlew build -x test -x integrationTest` (작업 디렉터리 `plugins/`) |
| `plugin-ci-test` | `./gradlew test integrationTest jacocoTestReport` (Testcontainers는 러너의 Docker 사용) → skip 0 확인(빌드에 내장) → 변경분 커버리지 검사(라인 ≥80%, 브랜치 ≥70%) → 리포트 아티팩트 업로드 |
| `server-ci-validate` | `docker compose -f infra/prod/server/compose.yaml config` → 이미지 태그·`PAPER_BUILD` 고정 여부 검사 → `.env.example`에 필요한 키가 모두 있는지 검사 |
| `infra-ci-validate` | `terraform fmt -check -recursive` → `terraform init -backend=false` → `terraform validate` |
| `infra-ci-plan` | OCI 인증(Secrets) → `terraform plan -out` → **무료 한도 가드**(아래) → plan 요약을 PR 코멘트로 |

### 무료 한도 가드 (`infra/prod/terraform/scripts/free-tier-guard`)

`terraform show -json plan` 결과를 검사해 하나라도 위반하면 실패.

| 검사 | 기준 |
|---|---|
| 인스턴스 shape | `VM.Standard.A1.Flex`만 (Micro 제외 다른 shape 금지) |
| A1 합계 | OCPU ≤ 4, 메모리 ≤ 24GB |
| 블록 스토리지 합계 | 부트 + 블록 ≤ 200GB |
| 유료 리소스 | Load Balancer(유료 shape), NAT Gateway 등 금지 목록 |
| 보호 리소스 | `blockgate-vm`, `blockgate-data`의 delete/replace 금지 |

## 4. CD 상세

### plugin-cd (concurrency: `cd-server`, cancel-in-progress: false)

| 순서 | 단계 |
|---|---|
| 1 | `plugin-cd-build`: shadowJar 빌드 → 아티팩트 |
| 2 | 버전 계산: 직전 `plugin-v*` 태그 이후 커밋(=PR 제목)으로 SemVer 결정 (feat→minor, fix/perf→patch, `!`→major) |
| 3 | `plugin-cd-deploy`: Tailscale 접속(ephemeral, `TS_` Secrets) → JAR를 VM `/srv/blockgate/mc/plugins/`로 복사(이전 JAR는 같은 이름으로 교체) |
| 4 | RCON 카운트다운: `docker compose exec -T mc rcon-cli "say 서버가 30초 후 재시작됩니다"` → 10초 → 재시작 |
| 5 | `docker compose -p blockgate-prod restart mc` (**/reload 금지**) |
| 6 | `plugin-cd-verify`: 로그에서 `Done (` 와 플러그인 활성화 로그(`[BlockGateCore] Enabling`)를 제한 시간(예: 180초) 안에 확인, 예외 스택이 있으면 실패 |
| 7 | 성공 시 `plugin-vX.Y.Z` 태그 생성, 알림(`NOTIFY_WEBHOOK_URL`, 선택) |
| 실패 시 | 알림 + 워크플로 실패. 자동 롤백은 이후 단계 |

### server-cd (concurrency: `cd-server`)

| 순서 | 단계 |
|---|---|
| 1 | Tailscale 접속 → `infra/prod/server/`를 VM `/srv/blockgate/server/`로 동기화 |
| 2 | `.env`를 GitHub Secrets로 생성(`MC_`, `DB_` 접두사) |
| 3 | `docker compose -p blockgate-prod up -d` (변경된 서비스만 재생성) |
| 4 | `server-cd-verify`: 헬스 체크(위와 동일) → `server-vX.Y.Z` 태그 → 알림 |

### infra-cd (concurrency: `cd-infra`)

| 순서 | 단계 |
|---|---|
| 1 | `terraform plan` → 무료 한도 가드 재검사 |
| 2 | `terraform apply` (가드 통과 시만) |
| 3 | `infra-vX.Y.Z` 태그 → 알림 |

## 5. Secrets (GitHub Actions)

| 이름 | 용도 |
|---|---|
| `OCI_TENANCY_OCID`, `OCI_USER_OCID`, `OCI_FINGERPRINT`, `OCI_PRIVATE_KEY`, `OCI_REGION`, `OCI_COMPARTMENT_OCID` | Terraform |
| `OCI_NAMESPACE` | state 버킷 네임스페이스 |
| `TS_OAUTH_CLIENT_ID`, `TS_OAUTH_SECRET` | Actions의 Tailscale 접속 (tag: `tag:ci`) |
| `TS_VM_AUTHKEY` | VM 최초 가입용 (일회용) |
| `MC_RCON_PASSWORD`, `MC_WHITELIST`, `MC_OPS` | 서버 설정 |
| `DB_PASSWORD` | PostgreSQL |
| `NOTIFY_WEBHOOK_URL` | 배포 알림 (선택, Discord 웹훅 권장) |
| `TS_VM_HOSTNAME` | VM의 Tailscale 호스트명 (`blockgate-vm`) |

> 접두사 규칙: `OCI_`, `TS_`, `MC_`, `DB_`, `NOTIFY_` 외의 Secret 이름은 만들지 않는다.

## 6. 버전 태그

| 영역 | 태그 | 생성 |
|---|---|---|
| plugin | `plugin-vX.Y.Z` | plugin-cd 성공 시 |
| server | `server-vX.Y.Z` | server-cd 성공 시 |
| infra | `infra-vX.Y.Z` | infra-cd 성공 시 |
