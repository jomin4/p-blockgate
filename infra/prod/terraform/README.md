# infra/prod/terraform — OCI 인프라

Phase 5에서 Claude Code가 작성한다. 명세는 `docs/02-infrastructure.md` A장.

| 경로 | 내용 | 커밋 |
|---|---|---|
| `bootstrap/` | compartment + state 버킷 (local state, 최초 1회) | ✅ (state 제외) |
| `*.tf` | VCN, subnet, IGW, Security List(25565), A1 2/12, 블록 100GB, `backend "oci"` | ✅ |
| `cloud-init.yaml` | Docker, Tailscale, 볼륨 마운트, iptables, deploy 사용자 | ✅ |
| `scripts/free-tier-guard` | plan JSON 무료 한도 검사 | ✅ |
| `terraform.tfvars.example` | 키 목록 | ✅ |
| `terraform.tfvars` | 🔑 실제 값 | ❌ |

## 규칙
- Always Free 리소스만. VM·블록 볼륨 `prevent_destroy = true`.
- 이미지 OCID는 data source로 조회 (하드코딩 금지).
- `terraform apply`는 plan 요약을 보고하고 **개발자 확인 후** 실행. `terraform destroy`는 Claude가 실행하지 않는다.
