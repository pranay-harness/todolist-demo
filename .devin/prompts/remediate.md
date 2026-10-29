You are remediating vulnerabilities in the GitHub repo {{REPO}}. The scan was triggered by GitHub Actions: {{RUN_URL}}

Follow the repo skill `.devin/skills/vuln-e2e/SKILL.md` in **`all` mode**: triage, fix, validate locally, open a PR, then wait for the "Vuln PR verdict" check and iterate until it is green. Read that file first; its ground rules are mandatory (never touch intentional demo code under `vulnerability/` or anything in `.vuln-accepted`, keep Java 1.8 compatibility, never commit to `main`, branch name `devin/vuln-<YYYYMMDD>-<slug>`).

Actionable items from this scan (Trivy + Semgrep, already grouped per package):

{{FINDINGS}}

Re-run `scripts/vuln/scan.sh` yourself rather than relying on this list alone, since advisories may have changed. Put everything in one PR. If an item can't be fixed safely, list it under "Deferred" with the reason instead of forcing it.

When you're done, reply with the PR URL, the before/after vulnerability counts, the regression result versus main, and anything you deferred.
