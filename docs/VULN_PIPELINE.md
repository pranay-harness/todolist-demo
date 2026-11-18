# End-to-end vulnerability management with Devin

```
 ┌──────────── Vuln Scan (vuln-scan.yml) ────────────┐
 │ on demand · weekly · push to main                 │
 │ Trivy (SCA, IaC, secrets) + Semgrep (SAST)        │──► GitHub Security tab (SARIF)
 │ triage-ready summary  ──► Devin API v3 session ───┼──┐
 └───────────────────────────────────────────────────┘  │
                                                        ▼
 Devin (cloud via API, or Devin desktop via /vuln-e2e): triage → fix pom.xml → validate locally → PR devin/vuln-*
                                                        │
 ┌──────── Vuln PR Validate (vuln-pr-validate.yml) ─────┴┐
 │ rescan base vs head   → fail on new HIGH/CRITICAL     │
 │ regression.yml        → run-comprehensive-tests.sh on │
 │                         base + head, fail on NEW      │
 │                         failures; japicmp per bumped  │
 │                         dependency                    │
 │ sticky PR comment + required check "Vuln PR verdict"  │
 └───────────────────────────────────────────────────────┘
```

Every stage calls the same scripts, so CI, Devin cloud and Devin desktop produce identical results:

| Piece | Path |
|---|---|
| Scanner wrapper (local binary or pinned Docker image) | `scripts/vuln/scan.sh` |
| Normalize / compare / test-diff / dep-diff / Devin request | `scripts/vuln/vulnctl.py` |
| Regression wrapper around your suite | `scripts/vuln/regression.sh` → `scripts/run-comprehensive-tests.sh` |
| Breaking-change detection | `scripts/detect-breaking-changes.sh` (japicmp) |
| Accepted risk (never auto-fixed or gated) | `.vuln-accepted` |
| Devin playbook (desktop + cloud) | `.devin/skills/vuln-e2e/SKILL.md` |
| Prompt for API-started sessions | `.devin/prompts/remediate.md` |

## One-time setup

1. **Connect Devin to GitHub.** In the Devin web app, go to Settings → Integrations → GitHub and give the app
   access to `pranay-harness/todolist-demo`. Devin needs this to push branches and open PRs.
2. **Create a service user for CI.** In Settings → Service users, create one with the *Member* role and copy its API
   key (it starts with `cog_`). Your org ID is under Settings → Devin API. Then add both as repo secrets:
   ```bash
   gh secret set DEVIN_API_KEY --repo pranay-harness/todolist-demo
   ```
   ```bash
   gh secret set DEVIN_ORG_ID --repo pranay-harness/todolist-demo
   ```
   If the secrets are missing, everything still runs except the Devin hand-off, which logs a warning.
3. **Optional repo variables** (Settings → Secrets and variables → Actions → Variables):
   | Variable | Effect |
   |---|---|
   | `DEVIN_AUTO_REMEDIATE=true` | The weekly scheduled scan also starts a Devin session (fully hands-off) |
   | `DEVIN_MAX_ACU` | ACU cap per session (default 10) |
   | `DEVIN_PLAYBOOK_ID` | Attach a Devin playbook to API-started sessions |
4. **Branch protection on `main`.** Require the status check **Vuln PR verdict**.
5. Code scanning needs no extra setup: SARIF upload is free on public repos. Results appear under Security →
   Code scanning, split into the `trivy` and `semgrep` categories.

## Running it

### Automated (GitHub Actions)
- **On demand:** Actions → **Vuln Scan** → Run workflow, with `remediate` ticked.
  ```bash
  gh workflow run vuln-scan.yml -f remediate=true -f min_severity=HIGH -f max_findings=5
  ```
- **Scheduled:** runs Mondays at 03:00 UTC. It remediates automatically only if `DEVIN_AUTO_REMEDIATE=true`.
- **Dedupe:** no new session starts while a `devin/vuln-*` PR is open.
- **Every PR** to `main` (Devin's or anyone's) gets the rescan, regression and breaking-change checks.
- **Regression only:** Actions → **Regression**, optionally setting `base_ref=main` to diff against main.

### Devin desktop
Open this repo in Devin and invoke the skill:

```
/vuln-e2e triage        # scan + triage table, no changes
/vuln-e2e fix HIGH 3    # fix up to 3 packages >= HIGH and open a PR
/vuln-e2e validate      # rescan + regression + japicmp for the current branch vs main
/vuln-e2e all           # full loop, including waiting on the PR checks
```

Local prerequisites:
- `brew install trivy`
- Semgrep is optional: `pipx install semgrep`, or run Docker.
- JDK 8+ and Maven.

If `~/.m2/settings.xml` points at a mirror that returns 401, prefix commands with
`MAVEN_ARGS="-s scripts/vuln/maven-central-settings.xml"`.

### Manual, without Devin
```bash
scripts/vuln/scan.sh . reports/vuln && cat reports/vuln/summary.md
```
```bash
scripts/vuln/regression.sh && cat target/regression/results.json
```

## How the gates decide

- **Rescan:** fingerprints are `pkg+CVE` for dependencies, `rule+file` for SAST and misconfig, and `rule+line` for
  secrets. A finding is "introduced" if it is on head but not on base. The gate fails on any new HIGH/CRITICAL that is
  not in `.vuln-accepted`.
- **Regression:** the suite runs on both the base commit and the PR. Every stage runs even when tests fail
  (`maven.test.failure.ignore`). The gate fails only on tests that fail on head but pass on base, or if the build
  breaks.
- **Breaking changes:** japicmp runs for each version change in `pom.xml`. The result is advisory and shown in the
  comment; Devin must address it in the PR description.

## Known baseline (as of this setup)

On `main`, 3 of 28 tests fail before any remediation. Because they also fail on base, the PR gate treats them
as pre-existing:

| Test | Why it fails |
|---|---|
| `DependencyUpgradeTest.testMySQLConnectorUpgrade` | Expects the MySQL 8 driver (`com.mysql.cj.jdbc.Driver`). Passes once Devin upgrades to `com.mysql:mysql-connector-j` 8.x, a good demo of "newly passing". |
| `DependencyUpgradeTest.testJacksonSafeDeserialization` | Expects `JsonMappingException`, but `readValue("[...]", Object.class)` returns a `List` on every Jackson version. The test itself needs fixing. |
| `VulnerabilityValidationTest.testVulnerabilityCodeIsolation` | Looks for `src/main/java/vulnerability/`, but the directory is at the repo root (`vulnerability/`). |
