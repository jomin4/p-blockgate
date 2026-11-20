# workflows

Phase 7에서 `docs/06-ci-cd.md` 명세대로 아래 파일을 작성한다.

| 파일 | Job |
|---|---|
| `plugin-ci.yml` | `plugin-ci-build`, `plugin-ci-test` |
| `plugin-cd.yml` | `plugin-cd-build`, `plugin-cd-deploy`, `plugin-cd-verify` |
| `server-ci.yml` | `server-ci-validate` |
| `server-cd.yml` | `server-cd-deploy`, `server-cd-verify` |
| `infra-ci.yml` | `infra-ci-validate`, `infra-ci-plan` |
| `infra-cd.yml` | `infra-cd-apply` |
