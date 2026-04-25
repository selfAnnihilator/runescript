#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/../src/runescript.jar"

if [[ ! -f "$JAR" ]]; then
    echo "runescript.jar not found — run build.sh first"
    exit 1
fi

PASS=0
FAIL=0
ERRORS=()

for src in "$SCRIPT_DIR"/*.rn; do
    expected="${src%.rn}.expected"
    name="$(basename "$src")"

    if [[ ! -f "$expected" ]]; then
        echo "SKIP  $name  (no .expected file)"
        continue
    fi

    actual=$(java -jar "$JAR" "$src" 2>&1)

    if diff -q <(echo "$actual") "$expected" > /dev/null 2>&1; then
        echo "PASS  $name"
        PASS=$((PASS + 1))
    else
        echo "FAIL  $name"
        diff <(echo "$actual") "$expected" | sed 's/^/      /'
        ERRORS+=("$name")
        FAIL=$((FAIL + 1))
    fi
done

echo ""
echo "Results: $PASS passed, $FAIL failed"

if [[ $FAIL -gt 0 ]]; then
    exit 1
fi
