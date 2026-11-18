---
name: vuln-e2e
description: End-to-end vulnerability management for this repo - scan with Trivy + Semgrep, triage findings, fix in a PR, then validate the PR with a rescan, the regression suite and breaking-change checks. Use for "scan for vulns", "triage vulnerabilities", "fix vulns", "validate this vuln PR", or "run the vuln pipeline".
argument-hint: "[scan|triage|fix|validate|all] [min severity, default HIGH] [max items, default 5]"
---

# vuln-e2e

The same pipeline runs from GitHub Actions (`.github/workflows/vuln-*.yml`, `regression.yml`)
and from here. Always use the repo scripts so local results match CI.

| Mode | What it does | Stops at |
|---|---|---|
| `scan` | Run the scanners, print the summary | Report |
| `triage` | scan + classify every actionable item | Triage table |
| `fix` | triage + patch + local validation + PR | Open PR, green checks |
| `validate` | Rescan + regression + breaking changes for the current branch vs `main` | Verdict |
| `all` (default) | `fix`, then wait on CI and iterate | PR with green "Vuln PR verdict" |

## Ground rules

- **Never modify intentional demo code**: `vulnerability/`, `idor-vuln.java`, or anything matched by
  `.vuln-accepted`. Report those as *accepted risk*. Fix SAST findings in `src/main/java` only
  with a minimal, behaviour-preserving change; otherwise **Defer**.
- `php-app/` is a PHP2Shell test fixture. Use the `fix-php-vuln` skill for it, not this one.
- The compiler target is **Java 1.8** (`pom.xml`). Never pick a version that needs a newer JDK
  (e.g. Spring 6, Struts 7, Tomcat 10+/Jakarta, Jetty 10+) and never change the compiler target.
- For dependencies, change **only** versions (plus the minimum code a bump needs to compile).
  No refactors, no formatting churn, no test deletions or `@Ignore`.
- Do not edit tests to make them pass. Tests already failing on `main` are pre-existing, not yours.
- Never commit to `main`. Branch: `devin/vuln-<YYYYMMDD>-<short-slug>`.
- If Maven fails with a 401 from a corporate mirror, add
  `MAVEN_ARGS="-s scripts/vuln/maven-central-settings.xml"` to every Maven/script command.

## 1. Scan

```bash
scripts/vuln/scan.sh . reports/vuln          # needs trivy (brew install trivy); semgrep optional (pipx install semgrep) or Docker
cat reports/vuln/summary.md
```

`MIN_SEVERITY` (default `HIGH`) and `MAX_FINDINGS` (default `10`) env vars tune the summary.
Outputs: `findings.json` (normalized), `remediation.md` (grouped items), `trivy.sarif`, `semgrep.sarif`.

## 2. Triage

For each item in `reports/vuln/remediation.md` (dependencies are already grouped, one bump fixes many CVEs),
decide one of:

| Decision | When |
|---|---|
| **Fix now** | Fixed version exists in the same major line, is Java-8 compatible, and the package is used (direct dep in `pom.xml`) |
| **Fix with care** | Fix needs a major/minor-line jump or an artifact rename (e.g. `mysql:mysql-connector-java` → `com.mysql:mysql-connector-j`). Do it only if regression + japicmp stay clean |
| **Defer** | Only fix requires Java 11+/Jakarta, or no fixed version. Say why and the safest workaround |
| **Accept** | Matches `.vuln-accepted` or is intentional demo code |

To judge reachability, grep `src/main` for the package's imports. Unused deps are still worth bumping,
but note "not referenced in code" (lower risk).

Choose the **smallest version that clears every listed CVE** for that package within the allowed line,
verified on Maven Central (`https://repo1.maven.org/maven2/<group path>/<artifact>/maven-metadata.xml`).
Keep related artifacts aligned (all `spring-*` on one version, all `jackson-*` on one version).

Print the triage table before changing anything. In `triage` mode, stop here.

## 3. Fix

1. `git checkout -b devin/vuln-$(date +%Y%m%d)-<slug> origin/main`
2. Before editing anything (the branch still equals `main`), record the baseline. You need it to tell
   regressions from pre-existing failures:
   ```bash
   scripts/vuln/regression.sh && cp target/regression/results.json /tmp/base-results.json
   scripts/vuln/scan.sh . /tmp/base-scan
   ```
3. Apply the "Fix now" (and safe "Fix with care") bumps to `pom.xml`. At most `max items` packages per PR.

## 4. Validate (also the `validate` mode)

Run all three. Every one must pass before you open or update the PR.

```bash
# a) Regression: no NEW failures vs main
scripts/vuln/regression.sh
python3 scripts/vuln/vulnctl.py tests-diff --base /tmp/base-results.json \
  --head target/regression/results.json --out /tmp/regression.md; cat /tmp/regression.md

# b) Breaking changes, for every bumped dependency
git show origin/main:pom.xml > /tmp/base-pom.xml
python3 scripts/vuln/vulnctl.py depdiff --base /tmp/base-pom.xml --head pom.xml | \
  while read -r ga old new; do scripts/detect-breaking-changes.sh "$ga" "$old" "$new"; done

# c) Rescan: targeted findings gone, nothing new ≥ HIGH
scripts/vuln/scan.sh . reports/vuln
python3 scripts/vuln/vulnctl.py compare --base /tmp/base-scan/findings.json \
  --head reports/vuln/findings.json --out /tmp/rescan.md; cat /tmp/rescan.md
```

In `validate` mode with no `/tmp/base-*` files, create them from `origin/main` in a
`git worktree` first, then print a verdict combining all three.

If japicmp reports binary-incompatible changes, check whether this repo calls any of the affected
classes/methods (`grep -r` in `src/`). If it does, adapt the call site minimally or downgrade to an
earlier fixed version. If a bump causes new test failures you can't fix with minimal changes, revert
that one bump, move it to **Defer**, and keep the rest.

## 5. Pull request

`git commit -m "fix(deps): remediate <n> vulnerable dependencies"` and open a PR against `main`:

```markdown
## Summary
Remediates <N> vulnerabilities (<C> critical, <H> high) across <P> packages. Found by Trivy/Semgrep (scripts/vuln/scan.sh).

## Triage
| Package | From → To | CVEs fixed | Decision | Reachable | Notes |
|---|---|---|---|---|---|

## Validation (local)
- Rescan: <paste /tmp/rescan.md headline>
- Regression: <paste /tmp/regression.md headline>, pre-existing failures listed
- Breaking changes: <per-package japicmp result, and whether affected APIs are used>

## Deferred / accepted
<items + reason>
```

## 6. Wait for CI and iterate (`all` mode)

The **Vuln PR Validate** workflow re-runs the rescan, regression (base vs head) and japicmp, and posts a
sticky comment starting `🛡️ Vulnerability PR validation`. Wait for the **Vuln PR verdict** check.
If it fails, read the comment and the failing job log, fix, and push to the same branch. After 3 failed
attempts, stop and summarize what's blocking on the PR.
