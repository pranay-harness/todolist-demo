#!/usr/bin/env python3
"""scripts/vuln/vulnctl.py - glue for the vulnerability management pipeline.

Subcommands
  summarize   trivy.json + semgrep.sarif -> findings.json, summary.md, remediation.md
  compare     base findings.json vs head findings.json -> new/fixed diff (gate)
  tests       surefire XML reports -> results.json
  tests-diff  base results.json vs head results.json -> new test failures (gate)
  depdiff     Maven dependency version changes between two pom.xml files
  devin-body  build the Devin API v3 create-session request body

Standard library only so it runs anywhere python3 exists (CI runners, Devin VMs).
"""
import argparse
import glob
import hashlib
import json
import os
import re
import sys
import xml.etree.ElementTree as ET

SEVERITIES = ["CRITICAL", "HIGH", "MEDIUM", "LOW", "UNKNOWN"]
RANK = {s: i for i, s in enumerate(SEVERITIES)}
SARIF_LEVEL = {"error": "HIGH", "warning": "MEDIUM", "note": "LOW", "none": "LOW"}


def sev_at_least(sev, minimum):
    return RANK.get(sev, RANK["UNKNOWN"]) <= RANK.get(minimum, RANK["UNKNOWN"])


def load_json(path, default=None):
    if not path or not os.path.exists(path):
        return default
    with open(path) as f:
        return json.load(f)


def write(path, text):
    with open(path, "w") as f:
        f.write(text)


# --------------------------------------------------------------------------
# Accepted risk (.vuln-accepted): one entry per line, "<path-prefix|rule|CVE>  # reason"
# --------------------------------------------------------------------------

def load_accepted(path):
    rules = []
    if path and os.path.exists(path):
        for line in open(path):
            entry, _, reason = line.partition("#")
            entry = entry.strip()
            if entry:
                rules.append((entry, reason.strip() or "accepted risk"))
    return rules


def accepted_reason(finding, rules):
    for entry, reason in rules:
        if finding["file"].startswith(entry) or entry in (finding["rule"], finding.get("vuln_id")):
            return reason
    return None


# --------------------------------------------------------------------------
# summarize
# --------------------------------------------------------------------------

def trivy_findings(doc):
    out = []
    for res in (doc or {}).get("Results") or []:
        target = res.get("Target", "")
        for v in res.get("Vulnerabilities") or []:
            out.append({
                "kind": "dependency",
                "tool": "trivy",
                "severity": v.get("Severity", "UNKNOWN"),
                "rule": v.get("VulnerabilityID", ""),
                "vuln_id": v.get("VulnerabilityID", ""),
                "title": v.get("Title") or v.get("VulnerabilityID", ""),
                "file": target,
                "line": None,
                "package": v.get("PkgName", ""),
                "installed": v.get("InstalledVersion", ""),
                "fixed": v.get("FixedVersion", ""),
                "url": v.get("PrimaryURL", ""),
                "fingerprint": f"dep|{target}|{v.get('PkgName')}|{v.get('VulnerabilityID')}",
            })
        for m in res.get("Misconfigurations") or []:
            if m.get("Status") == "PASS":
                continue
            line = (m.get("CauseMetadata") or {}).get("StartLine")
            out.append({
                "kind": "misconfig",
                "tool": "trivy",
                "severity": m.get("Severity", "UNKNOWN"),
                "rule": m.get("ID") or m.get("AVDID", ""),
                "title": m.get("Title", ""),
                "file": target,
                "line": line,
                "fix": m.get("Resolution", ""),
                "url": m.get("PrimaryURL", ""),
                "fingerprint": f"misconfig|{target}|{m.get('ID') or m.get('AVDID')}",
            })
        for s in res.get("Secrets") or []:
            out.append({
                "kind": "secret",
                "tool": "trivy",
                "severity": s.get("Severity", "UNKNOWN"),
                "rule": s.get("RuleID", ""),
                "title": s.get("Title", ""),
                "file": target,
                "line": s.get("StartLine"),
                "fingerprint": f"secret|{target}|{s.get('RuleID')}|{s.get('StartLine')}",
            })
    return out


def sarif_findings(doc, tool):
    out = []
    for run in (doc or {}).get("runs") or []:
        rules = {r.get("id"): r for r in ((run.get("tool") or {}).get("driver") or {}).get("rules") or []}
        seen = {}
        for r in run.get("results") or []:
            rule_id = r.get("ruleId", "")
            rule = rules.get(rule_id, {})
            level = r.get("level") or (rule.get("defaultConfiguration") or {}).get("level", "warning")
            loc = ((r.get("locations") or [{}])[0].get("physicalLocation") or {})
            path = (loc.get("artifactLocation") or {}).get("uri", "")
            line = (loc.get("region") or {}).get("startLine")
            msg = (r.get("message") or {}).get("text", "")
            # Line-insensitive fingerprint so unrelated edits don't churn the diff.
            base = f"sast|{path}|{rule_id}"
            seen[base] = seen.get(base, 0) + 1
            out.append({
                "kind": "sast",
                "tool": tool,
                "severity": SARIF_LEVEL.get(level, "MEDIUM"),
                "rule": rule_id,
                "title": (rule.get("shortDescription") or {}).get("text") or msg[:120],
                "file": path,
                "line": line,
                "message": msg,
                "fingerprint": f"{base}|{seen[base]}",
            })
    return out


def group_dependencies(findings):
    """One remediation item per (manifest, package): a single bump fixes many CVEs."""
    groups = {}
    for f in findings:
        if f["kind"] != "dependency":
            continue
        key = (f["file"], f["package"])
        g = groups.setdefault(key, {"file": f["file"], "package": f["package"],
                                    "installed": f["installed"], "severity": f["severity"],
                                    "ids": [], "fixed": set()})
        g["ids"].append(f["vuln_id"])
        if f.get("fixed"):
            g["fixed"].update(x.strip() for x in f["fixed"].split(","))
        if RANK.get(f["severity"], 9) < RANK.get(g["severity"], 9):
            g["severity"] = f["severity"]
    return sorted(groups.values(), key=lambda g: (RANK.get(g["severity"], 9), -len(g["ids"])))


def counts_table(findings):
    kinds = ["dependency", "sast", "misconfig", "secret"]
    rows = ["| Kind | " + " | ".join(SEVERITIES[:4]) + " |", "|---|" + "---|" * 4]
    for k in kinds:
        c = [sum(1 for f in findings if f["kind"] == k and f["severity"] == s) for s in SEVERITIES[:4]]
        rows.append(f"| {k} | " + " | ".join(map(str, c)) + " |")
    return "\n".join(rows)


def cmd_summarize(a):
    findings = trivy_findings(load_json(os.path.join(a.dir, "trivy.json"), {}))
    findings += sarif_findings(load_json(os.path.join(a.dir, "semgrep.sarif"), {}), "semgrep")
    accepted = load_accepted(a.accepted)
    for f in findings:
        reason = accepted_reason(f, accepted)
        f["accepted"] = reason
        f["actionable"] = (not reason and sev_at_least(f["severity"], a.min_severity)
                           and (f["kind"] != "dependency" or bool(f.get("fixed"))))
    findings.sort(key=lambda f: (RANK.get(f["severity"], 9), f["kind"], f["file"]))

    actionable = [f for f in findings if f["actionable"]]
    deps = group_dependencies(actionable)
    code = [f for f in actionable if f["kind"] != "dependency"]
    stats = {
        "total": len(findings),
        "accepted": sum(1 for f in findings if f["accepted"]),
        "actionable": len(actionable),
        "actionable_items": len(deps) + len(code),
        "by_severity": {s: sum(1 for f in findings if f["severity"] == s) for s in SEVERITIES},
    }
    json.dump({"stats": stats, "findings": findings}, open(os.path.join(a.dir, "findings.json"), "w"), indent=1)

    md = [f"## Vulnerability scan\n",
          f"**{stats['total']}** findings · **{stats['actionable']}** actionable "
          f"(≥{a.min_severity}, fix available, not accepted) · {stats['accepted']} accepted risk\n",
          counts_table(findings), ""]
    md += ["### Actionable remediation items\n"]
    md += remediation_lines(deps, code, a.limit) or ["None 🎉"]
    write(os.path.join(a.dir, "summary.md"), "\n".join(md) + "\n")
    write(os.path.join(a.dir, "remediation.md"), "\n".join(remediation_lines(deps, code, a.limit)) + "\n")
    print(json.dumps(stats))


def vkey(v):
    return [(0, int(x)) if x.isdigit() else (1, x) for x in re.split(r"[.\-_]", v)]


def fix_hint(installed, fixed):
    """Nearest fixed versions above the installed one, plus the newest."""
    try:
        ups = sorted((v for v in fixed if vkey(v) > vkey(installed)), key=vkey)
    except TypeError:
        ups = sorted(fixed)
    if len(ups) <= 4:
        return ", ".join(ups) or "n/a"
    return ", ".join(ups[:3]) + f" … {ups[-1]}"


def remediation_lines(deps, code, limit):
    lines = []
    for g in deps[:limit]:
        fixed = fix_hint(g["installed"], g["fixed"])
        ids = ", ".join(sorted(set(g["ids"]))[:6]) + (" …" if len(set(g["ids"])) > 6 else "")
        lines.append(f"- **{g['severity']}** dependency `{g['package']}` {g['installed']} in `{g['file']}` "
                     f"→ fixed in {fixed} ({len(set(g['ids']))} vulns: {ids})")
    for f in code[:max(0, limit - len(deps[:limit]))]:
        loc = f"{f['file']}:{f['line']}" if f.get("line") else f["file"]
        lines.append(f"- **{f['severity']}** {f['kind']} `{f['rule']}` at `{loc}` — {f['title']}")
    return lines


# --------------------------------------------------------------------------
# compare
# --------------------------------------------------------------------------

def cmd_compare(a):
    base = {f["fingerprint"]: f for f in (load_json(a.base, {}) or {}).get("findings", [])}
    head = {f["fingerprint"]: f for f in (load_json(a.head, {}) or {}).get("findings", [])}
    new = [head[k] for k in head if k not in base and not head[k]["accepted"]]
    fixed = [base[k] for k in base if k not in head]
    blocking = [f for f in new if sev_at_least(f["severity"], a.fail_on)]

    def row(f):
        what = f"`{f['package']}` {f['installed']}" if f["kind"] == "dependency" else f"`{f['file']}`"
        return f"| {f['severity']} | {f['kind']} | {f['rule']} | {what} |"

    md = ["### 🔁 Rescan: base vs PR\n",
          f"✅ **{len(fixed)}** fixed · 🆕 **{len(new)}** introduced · "
          f"{'❌' if blocking else '✅'} **{len(blocking)}** new ≥{a.fail_on}\n"]
    for title, items in (("Introduced", new), ("Fixed", fixed)):
        if items:
            md += [f"<details><summary>{title} ({len(items)})</summary>\n",
                   "| Severity | Kind | Rule | Where |", "|---|---|---|---|"]
            md += [row(f) for f in sorted(items, key=lambda f: RANK.get(f["severity"], 9))[:50]]
            md += ["\n</details>\n"]
    write(a.out, "\n".join(md) + "\n")
    print(json.dumps({"fixed": len(fixed), "new": len(new), "blocking": len(blocking)}))
    return 1 if blocking else 0


# --------------------------------------------------------------------------
# tests / tests-diff
# --------------------------------------------------------------------------

def cmd_tests(a):
    # Surefire (TestNG provider) writes both TestSuite and per-class reports; dedupe by name.
    status = {}
    reports = glob.glob(os.path.join(a.reports, "TEST-*.xml"))
    for path in sorted(reports):
        for tc in ET.parse(path).getroot().iter("testcase"):
            name = f"{tc.get('classname')}.{tc.get('name', '').split(' on ')[0]}"
            if tc.find("failure") is not None or tc.find("error") is not None:
                status[name] = "failed"
            else:
                status.setdefault(name, "skipped" if tc.find("skipped") is not None else "passed")
    results = {
        "total": len(status),
        "failed": sorted(n for n, v in status.items() if v == "failed"),
        "skipped": sum(1 for v in status.values() if v == "skipped"),
        "reports": len(reports),
    }
    results["build_ok"] = a.build_ok == "true"
    json.dump(results, open(a.out, "w"), indent=1)
    print(json.dumps({k: v if k != "failed" else len(v) for k, v in results.items()}))


def cmd_tests_diff(a):
    head = load_json(a.head, {})
    base = load_json(a.base, None)
    base_failed = set((base or {}).get("failed", []))
    # Without a base there is nothing to regress against: failures are informational.
    new = [t for t in head.get("failed", []) if t not in base_failed] if base else []
    fixed = sorted(base_failed - set(head.get("failed", []))) if base else []
    broken = not head.get("build_ok", True) or head.get("reports", 0) == 0
    ok = not new and not broken

    md = ["### 🧪 Regression suite (`scripts/run-comprehensive-tests.sh`)\n",
          f"{'✅' if ok else '❌'} {head.get('total', 0)} tests · {len(head.get('failed', []))} failing · "
          f"**{len(new)} new failures** vs base · {len(fixed)} newly passing\n"]
    if broken:
        md.append("❌ **Build/compile failed or no test reports were produced.**\n")
    if base is None:
        md.append("_No base to compare against; failing tests below are informational._\n")
        md += ["**Failing tests**\n"] + [f"- `{t}`" for t in head.get("failed", [])] + [""]
    for title, items in (("New failures (regressions)", new), ("Newly passing", fixed),
                         ("Pre-existing failures (also failing on base)",
                          sorted(set(head.get("failed", [])) & base_failed))):
        if items:
            md += [f"**{title}**\n"] + [f"- `{t}`" for t in items] + [""]
    write(a.out, "\n".join(md) + "\n")
    print(json.dumps({"new_failures": len(new), "fixed": len(fixed), "broken": broken}))
    return 0 if ok else 1


# --------------------------------------------------------------------------
# depdiff
# --------------------------------------------------------------------------

def pom_deps(path):
    if not path or not os.path.exists(path) or os.path.getsize(path) == 0:
        return {}
    text = re.sub(r'\sxmlns="[^"]+"', "", open(path).read(), count=1)
    root = ET.fromstring(text)
    props = {p.tag: (p.text or "").strip() for p in root.findall("properties/*")}
    deps = {}
    for d in root.iter("dependency"):
        g, art, v = (d.findtext(t, "").strip() for t in ("groupId", "artifactId", "version"))
        v = re.sub(r"\$\{([^}]+)\}", lambda m: props.get(m.group(1), m.group(0)), v)
        if g and art and v:
            deps[f"{g}:{art}"] = v
    return deps


def cmd_depdiff(a):
    base, head = pom_deps(a.base), pom_deps(a.head)
    for ga in sorted(set(base) & set(head)):
        if base[ga] != head[ga]:
            print(f"{ga} {base[ga]} {head[ga]}")
    # Artifact swaps (e.g. mysql:mysql-connector-java -> com.mysql:mysql-connector-j)
    for ga in sorted(set(head) - set(base)):
        print(f"{ga} - {head[ga]}", file=sys.stderr)


# --------------------------------------------------------------------------
# devin-body
# --------------------------------------------------------------------------

def cmd_devin_body(a):
    prompt = open(a.prompt).read()
    prompt = prompt.replace("{{REPO}}", a.repo).replace("{{RUN_URL}}", a.run_url)
    prompt = prompt.replace("{{FINDINGS}}", open(a.findings).read().strip() or "(none)")
    digest = hashlib.sha1(open(a.findings, "rb").read()).hexdigest()[:8]
    body = {
        "title": f"Vuln remediation {a.repo} ({digest})",
        "prompt": prompt,
        "repos": [a.repo],
        "tags": ["vuln-pipeline", "github-actions"],
    }
    if a.max_acu:
        body["max_acu_limit"] = int(a.max_acu)
    if a.playbook_id:
        body["playbook_id"] = a.playbook_id
    json.dump(body, sys.stdout)


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="cmd", required=True)

    s = sub.add_parser("summarize")
    s.add_argument("--dir", required=True)
    s.add_argument("--min-severity", default=os.environ.get("MIN_SEVERITY", "HIGH"), choices=SEVERITIES)
    s.add_argument("--accepted", default=".vuln-accepted")
    s.add_argument("--limit", type=int, default=int(os.environ.get("MAX_FINDINGS", "10")))

    s = sub.add_parser("compare")
    s.add_argument("--base", required=True)
    s.add_argument("--head", required=True)
    s.add_argument("--out", required=True)
    s.add_argument("--fail-on", default="HIGH", choices=SEVERITIES)

    s = sub.add_parser("tests")
    s.add_argument("--reports", default="target/surefire-reports")
    s.add_argument("--out", required=True)
    s.add_argument("--build-ok", default="true")

    s = sub.add_parser("tests-diff")
    s.add_argument("--base")
    s.add_argument("--head", required=True)
    s.add_argument("--out", required=True)

    s = sub.add_parser("depdiff")
    s.add_argument("--base", required=True)
    s.add_argument("--head", required=True)

    s = sub.add_parser("devin-body")
    s.add_argument("--prompt", required=True)
    s.add_argument("--findings", required=True)
    s.add_argument("--repo", required=True)
    s.add_argument("--run-url", default="")
    s.add_argument("--max-acu", default="")
    s.add_argument("--playbook-id", default="")

    a = p.parse_args()
    fn = globals()["cmd_" + a.cmd.replace("-", "_")]
    sys.exit(fn(a) or 0)


if __name__ == "__main__":
    main()
