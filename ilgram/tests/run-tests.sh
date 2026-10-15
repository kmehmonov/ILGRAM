#!/usr/bin/env bash
# Compiles and runs the pure-Java ILGRAM tests on a plain JDK (no Android SDK needed).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$(mktemp -d)"
SRC="$ROOT/TMessagesProj/src/main/java/org/telegram/ilgram"
javac -d "$OUT" "$SRC/ILGRAMRules.java" "$SRC/ILGRAMRemoteConfig.java" \
  "$ROOT/ilgram/tests/ILGRAMRulesTest.java" "$ROOT/ilgram/tests/ILGRAMRemoteConfigTest.java"
java -cp "$OUT" ILGRAMRulesTest
java -cp "$OUT" ILGRAMRemoteConfigTest "$ROOT/ilgram/tests/fixtures/remote"
