# 02. 인프라

## A. 운영 인프라 (Oracle Cloud Always Free)

### 리소스

| 리소스 | 이름 | 사양 | 비고 |
|---|---|---|---|
| Compartment | `blockgate` | | 모든 리소스를 여기에 생성 |
| VCN | `blockgate-vcn` | 10.0.0.0/16 | |
| Subnet | `blockgate-subnet-public` | 10.0.1.0/24, public | 무료 티어에 NAT Gateway 없음 → public subnet |
| Internet Gateway | `blockgate-igw` | | |
| Security List | `blockgate-sl` | ingress: **TCP 25565만** (0.0.0.0/0) | SSH 22는 공개하지 않음 |
| Instance | `blockgate-vm` | VM.Standard.A1.Flex, **2 OCPU / 12GB** | Ubuntu 24.04 ARM64, `prevent_destroy` |
| Boot Volume | (인스턴스 포함) | **50GB** | |
| Block Volume | `blockgate-data` | **100GB**, paravirtualized | `/srv/blockgate`에 마운트, `prevent_destroy` |
| Object Storage | `blockgate-tfstate` | Terraform state 버킷 | 20GB 이내 |

- 무료 한도: A1 합계 4 OCPU / 24GB, 블록(부트 포함) 합계 200GB, Object Storage 20GB. **이 범위 밖 리소스는 만들지 않는다.**
- Freeform tags: `project=blockgate`, `managed-by=terraform`, `component=<infra|server>`
- Ubuntu 이미지 OCID는 리전마다 다르므로 `oci_core_images` data source로 조회한다 (하드코딩 금지).
- Osaka A1 용량 부족(`Out of host capacity`)이 나면 잠시 후 재시도한다. 계속 실패하면 개발자에게 보고.

### Terraform 구조

| 경로 | 내용 |
|---|---|
| `infra/prod/terraform/bootstrap/` | state 버킷만 생성 (local state, 최초 1회) |
| `infra/prod/terraform/` | 본 인프라. `backend "oci"`로 state 버킷 사용 (locking) |
| `infra/prod/terraform/cloud-init.yaml` | VM 초기화 템플릿 |
| `infra/prod/terraform/terraform.tfvars` | 🔑 개인 값 (gitignore) |
| `infra/prod/terraform/terraform.tfvars.example` | 키 목록만 (커밋) |

### cloud-init이 하는 일

| 순서 | 작업 |
|---|---|
| 1 | 패키지 업데이트, Docker Engine + Compose plugin 설치 |
| 2 | Tailscale 설치 → auth key로 tailnet 가입 (hostname `blockgate-vm`, Tailscale SSH 활성화) |
| 3 | 블록 볼륨 포맷(최초만) → `/srv/blockgate` 마운트 → fstab 등록 (`_netdev,nofail`) |
| 4 | iptables에 TCP 25565 허용 추가 후 `netfilter-persistent save` (Oracle Ubuntu 이미지는 기본적으로 막혀 있음) |
| 5 | 배포 사용자 `deploy` 생성, docker 그룹 추가, `/srv/blockgate` 소유권 부여 |

> Tailscale auth key는 user_data에 들어가므로 **일회용·만료형 키**를 쓴다.
> Tailscale 연결이 실패하면 OCI 콘솔의 Serial Console로 복구한다 (SSH 공개로 우회하지 않는다).

### 운영 서버 Compose (`infra/prod/server/`)

| 서비스 | 이미지 | 설정 |
|---|---|---|
| `mc` | `itzg/minecraft-server` (Java 25 태그, 버전 고정) | `TYPE=PAPER`, `VERSION=26.1.2`, `PAPER_BUILD=<고정>`, `MEMORY=8G`, `ONLINE_MODE=TRUE`, `ENFORCE_WHITELIST=TRUE`, `ENABLE_RCON=TRUE`(포트 미공개), `EULA`는 개발자 동의 후 |
| `postgres` | `postgres:17` | 포트 미공개(내부 네트워크만), 데이터 `/srv/blockgate/postgres` |

| 항목 | 값 |
|---|---|
| Compose 프로젝트명 | `blockgate-prod` |
| 데이터 경로 | `/srv/blockgate/mc` (월드·플러그인), `/srv/blockgate/postgres` |
| 공개 포트 | `25565:25565/tcp`만 |
| 비밀값 | VM의 `/srv/blockgate/.env` (CD가 GitHub Secrets로 생성), 레포에는 `.env.example`만 |
| 화이트리스트 | `WHITELIST` 환경변수 (닉네임 목록) |
| 메모리 배분 | MC 8G / Postgres ~1.5G / OS 여유 |

## B. 개발 인프라 (각 개발자 PC)

### 로컬 Compose (`infra/dev/`)

| 서비스 | 설정 |
|---|---|
| `mc` | 운영과 같은 이미지·버전·빌드. `ONLINE_MODE=FALSE`(봇 접속용), `127.0.0.1:25565`만 바인딩, RCON 활성(`127.0.0.1:25575` 바인딩) |
| `postgres` | `postgres:17.12`, `127.0.0.1:5432`만 바인딩, 데이터는 이름 있는 볼륨 `blockgate-dev_pgdata` |
| VitaminMCP 플러그인 | `infra/dev/plugins-extra/`에 다운로드 (gitignore), 컨테이너 plugins로 복사 |

| 항목 | 값 |
|---|---|
| Compose 프로젝트명 | `blockgate-dev` |
| 데이터 | MC: `infra/dev/data/` (gitignore) / DB: Docker 볼륨 `blockgate-dev_pgdata` (초기화는 `down -v`) |
| 플러그인 반영 | Gradle 태스크 `deployDev`: shadowJar → `infra/dev/data/plugins/` 복사 → `docker compose restart mc` |
| DB 접속 | `docker compose -p blockgate-dev exec postgres psql -U blockgate` |
| 통합 테스트 DB | 로컬 Compose DB를 **쓰지 않는다**. Testcontainers가 테스트마다 격리 DB를 띄운다 |

## C. 네트워크 경로 요약

| 경로 | 수단 |
|---|---|
| 플레이어 → 서버 | 인터넷 → `<VM 공인 IP>:25565` (online-mode + whitelist) |
| GitHub Actions → VM | Tailscale(ephemeral) → SSH `deploy@blockgate-vm` |
| 개발자 → VM 관리 | Tailscale SSH |
| RCON | VM 내부 `docker compose exec mc rcon-cli` (네트워크 노출 없음) |
| PostgreSQL | Compose 내부 네트워크만 |
