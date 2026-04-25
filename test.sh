#!/bin/bash
# Build and run JUnit 5 unit tests for RuneScript

set -e

JUNIT_JAR="lib/junit-standalone.jar"
OUT_DIR="test-out"
SRC="src/RuneScript.java"
TEST_DIR="test"

if [ ! -f "$JUNIT_JAR" ]; then
    echo "JUnit JAR not found: $JUNIT_JAR"
    echo "Run: curl -sL https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar -o $JUNIT_JAR"
    exit 1
fi

mkdir -p "$OUT_DIR"

echo "Compiling RuneScript..."
javac -d "$OUT_DIR" "$SRC"

echo "Compiling tests..."
javac -cp "$OUT_DIR:$JUNIT_JAR" -d "$OUT_DIR" "$TEST_DIR"/*.java

echo "Running tests..."
java -cp "$OUT_DIR:$JUNIT_JAR" \
    org.junit.platform.console.ConsoleLauncher \
    --select-class=LexerTest \
    --select-class=ParserTest \
    --select-class=ResolverTest \
    --select-class=VMTest \
    --details=tree
