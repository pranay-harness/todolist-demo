#!/usr/bin/env bash
# scripts/vuln/scan.sh
# Open-source vulnerability scan shared by GitHub Actions and Devin (desktop or cloud).
#
#   Trivy   -> dependencies (SCA), IaC/Dockerfile misconfig, secrets
#   Semgrep -> static analysis (SAST)
#
# Usage: scripts/vuln/scan.sh [target_dir] [out_dir]
#   target_dir  defaults to .
#   out_dir     defaults to <target_dir>/reports/vuln
#
# Uses local trivy/semgrep binaries when installed, otherwise pinned Docker images.
# Writes trivy.json, trivy.sarif, semgrep.sarif, findings.json and summary.md to out_dir.

set -euo pipefail

TARGET=$(cd "${1:-.}" && pwd)
OUT=${2:-$TARGET/reports/vuln}
mkdir -p "$OUT"
OUT=$(cd "$OUT" && pwd)

TRIVY_IMAGE=${TRIVY_IMAGE:-aquasec/trivy:0.65.0}
SEMGREP_IMAGE=${SEMGREP_IMAGE:-semgrep/semgrep:1.181.1}
SEMGREP_CONFIG=${SEMGREP_CONFIG:-p/default}
HERE=$(cd "$(dirname "$0")" && pwd)

have() { command -v "$1" >/dev/null 2>&1; }
docker_ok() { have docker && docker info >/dev/null 2>&1; }

SKIP_DIRS=(--skip-dirs target --skip-dirs reports --skip-dirs node_modules --skip-dirs .git)

echo "Scanning $TARGET -> $OUT"

# --- Trivy ---------------------------------------------------------------
if have trivy; then
    trivy fs --quiet --scanners vuln,misconfig,secret "${SKIP_DIRS[@]}" \
        --format json --output "$OUT/trivy.json" "$TARGET"
    trivy convert --quiet --format sarif --output "$OUT/trivy.sarif" "$OUT/trivy.json"
elif docker_ok; then
    docker run --rm -v "$TARGET:/src:ro" -v "$OUT:/out" -v trivy-cache:/root/.cache "$TRIVY_IMAGE" \
        fs --quiet --scanners vuln,misconfig,secret "${SKIP_DIRS[@]}" \
        --format json --output /out/trivy.json /src
    docker run --rm -v "$OUT:/out" -v trivy-cache:/root/.cache "$TRIVY_IMAGE" \
        convert --quiet --format sarif --output /out/trivy.sarif /out/trivy.json
    # Docker paths are /src-relative; make them repo-relative like the local binary.
    sed -i.bak 's#"/src/#"#g' "$OUT/trivy.json" "$OUT/trivy.sarif" && rm -f "$OUT"/*.bak
else
    echo "❌ trivy not found and Docker unavailable (brew install trivy)" >&2
    exit 2
fi
echo "✓ Trivy complete"

# --- Semgrep -------------------------------------------------------------
SEMGREP_ARGS=(scan --config "$SEMGREP_CONFIG" --metrics off --quiet --sarif
    --exclude target --exclude reports --exclude node_modules)
if have semgrep; then
    (cd "$TARGET" && semgrep "${SEMGREP_ARGS[@]}" --output "$OUT/semgrep.sarif" .) || true
elif docker_ok; then
    docker run --rm -v "$TARGET:/src:ro" -v "$OUT:/out" -w /src "$SEMGREP_IMAGE" \
        semgrep "${SEMGREP_ARGS[@]}" --output /out/semgrep.sarif . || true
else
    echo "⚠️  semgrep not found and Docker unavailable; skipping SAST (pipx install semgrep)" >&2
fi
[ -s "$OUT/semgrep.sarif" ] && echo "✓ Semgrep complete"

# --- Normalize -----------------------------------------------------------
python3 "$HERE/vulnctl.py" summarize --dir "$OUT"
echo "Report: $OUT/summary.md"
