#!/usr/bin/env bash
# scripts/vuln/regression.sh
# Runs the repo's regression pipeline (scripts/run-comprehensive-tests.sh) and records
# results to target/regression/results.json so base and PR runs can be compared.
#
# Usage: scripts/vuln/regression.sh            # record; exit 0 unless the build breaks
#        STRICT=1 scripts/vuln/regression.sh   # also exit 1 on any failing test
#
# Test failures don't abort the suite (maven.test.failure.ignore), so every stage runs
# and pre-existing failures can be told apart from regressions.

set -uo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
SUITE=scripts/run-comprehensive-tests.sh
# Base branches may predate the suite; fall back to the pipeline's copy.
[ -f "$SUITE" ] || SUITE="$HERE/../run-comprehensive-tests.sh"

mkdir -p target/regression
export MAVEN_ARGS="${MAVEN_ARGS:-} -B -ntp -Dmaven.test.failure.ignore=true"

bash "$SUITE" 2>&1 | tee target/regression/suite.log
status=${PIPESTATUS[0]}
build_ok=$([ "$status" -eq 0 ] && echo true || echo false)

python3 "$HERE/vulnctl.py" tests --reports target/surefire-reports \
    --out target/regression/results.json --build-ok "$build_ok"

[ "$build_ok" = true ] || { echo "❌ Regression suite errored (exit $status)"; exit "$status"; }
failed=$(python3 -c 'import json;print(len(json.load(open("target/regression/results.json"))["failed"]))')
echo "Regression suite finished: $failed failing test(s); see target/regression/results.json"
[ "${STRICT:-0}" = 1 ] && [ "$failed" != 0 ] && exit 1
exit 0
