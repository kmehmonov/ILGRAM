#!/usr/bin/env bash
# Installs the ILGRAM instrumentation build on the running emulator and runs the ILGRAM tests.
set -u
OUT=device-test-results
mkdir -p "$OUT"
APP=$(find TMessagesProj_AppTests/build/outputs/apk -name '*.apk' ! -name '*androidTest*' | head -1)
TEST=$(find TMessagesProj_AppTests/build/outputs/apk -name '*androidTest*.apk' | head -1)
echo "app=$APP test=$TEST"
adb install -r -g "$APP" | tee "$OUT/install.txt"
adb install -r -t -g "$TEST" | tee -a "$OUT/install.txt"
PKG=$(aapt2 dump packagename "$APP" 2>/dev/null || "$ANDROID_HOME"/build-tools/36.0.0/aapt2 dump packagename "$APP")
TPKG=$("$ANDROID_HOME"/build-tools/36.0.0/aapt2 dump packagename "$TEST")
echo "package=$PKG test package=$TPKG" | tee -a "$OUT/install.txt"
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS 2>/dev/null || true
adb logcat -c || true
adb shell am instrument -w -r \
  -e class org.telegram.ilgram.ILGRAMIntegrationTest,org.telegram.ilgram.ILGRAMSmokeTest \
  "$TPKG/androidx.test.runner.AndroidJUnitRunner" > "$OUT/instrument-raw.txt" 2>&1
cat "$OUT/instrument-raw.txt" | grep -E "INSTRUMENTATION_STATUS: (test|class)=|INSTRUMENTATION_STATUS_CODE|INSTRUMENTATION_CODE|OK \(|FAILURES|Tests run|Error|Exception" | head -300 > "$OUT/instrument-summary.txt"
cat "$OUT/instrument-summary.txt" | tail -60
adb logcat -d > "$OUT/logcat.txt" 2>/dev/null || true
adb shell ls "/sdcard/Android/data/$PKG/files/ilgram-screens" || true
adb pull "/sdcard/Android/data/$PKG/files/ilgram-screens" "$OUT/screens" || true
adb shell dumpsys activity top | head -40 > "$OUT/activity-top.txt" 2>/dev/null || true
# Spec test 28: an in-place update (same signature, adb install -r) keeps the app's data / session files
adb shell "run-as $PKG sh -c 'mkdir -p files && echo ilgram > files/ilgram_update_marker'" || true
adb install -r -g "$APP" > "$OUT/reinstall.txt" 2>&1
if adb shell "run-as $PKG cat files/ilgram_update_marker" 2>/dev/null | grep -q ilgram; then
  echo "UPDATE_KEEPS_DATA: PASS" | tee -a "$OUT/instrument-summary.txt"
else
  echo "UPDATE_KEEPS_DATA: FAIL" | tee -a "$OUT/instrument-summary.txt"
fi
grep -q "^OK (" "$OUT/instrument-raw.txt"
