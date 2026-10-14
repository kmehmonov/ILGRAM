#!/usr/bin/env bash
# Compiles and runs the pure-Java ILGRAM policy tests on a plain JDK (no Android SDK needed).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$(mktemp -d)"
javac -d "$OUT" "$ROOT/TMessagesProj/src/main/java/org/telegram/ilgram/ILGRAMRules.java" "$ROOT/ilgram/tests/ILGRAMRulesTest.java"
java -cp "$OUT" ILGRAMRulesTest
