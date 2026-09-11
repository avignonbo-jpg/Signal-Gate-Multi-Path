#!/bin/bash
##############################################################################
# SignalGate Pulse — Emulator Launch Verification + Diagnostic Log Capture
#
# Used by .github/workflows/crash-diagnostic.yml.
#
# This script deliberately captures TWO Logcat streams:
#
#   1. full_logcat.txt
#      Complete unfiltered Logcat using:
#          adb logcat -v threadtime
#
#      This MUST remain unfiltered because Android/framework/vendor errors
#      such as:
#
#          E/ultipoint.pulse: No package ID 25 found...
#
#      may not use a SignalGate tag.
#
#   2. signalgate_diagnostic_logcat.txt
#      Focused SignalGate screening/decision pipeline using:
#
#          SignalGate:V
#          SignalGateScreening:V
#          CallScreeningEngine:V
#          SignalGateDecision:V
#          SignalGateData:V
#          SignalGatePersistence:V
#
# The two captures allow us to correlate the focused SignalGate pipeline
# against unrelated Android/framework/vendor events without losing either.
#
# WHY THIS IS A SEPARATE SCRIPT, NOT INLINE IN THE WORKFLOW YAML:
# reactivecircus/android-emulator-runner@v2 executes each line of a multi-line
# `script:` input as its own independent shell invocation. Keeping the complete
# flow here guarantees that background-process variables, loops, and cleanup
# occur within one shell process.
#
# Usage:
#
#   verify-launch-and-capture.sh <component> <package> <workspace_dir>
#
#   <component>
#       Fully-qualified "package/class" component name.
#
#   <package>
#       Installed applicationId used for process verification.
#
#   <workspace_dir>
#       Directory where final log artifacts are written.
##############################################################################

set -uo pipefail

COMPONENT="${1:?component (package/class) required}"
PACKAGE="${2:?package required}"
WORKSPACE_DIR="${3:?workspace dir required}"

FULL_LOG="/tmp/full_logcat.txt"
DIAGNOSTIC_LOG="/tmp/signalgate_diagnostic_logcat.txt"

echo "=== Clearing Logcat ==="
adb logcat -c

echo "=== Starting FULL Logcat capture ==="
adb logcat -v threadtime > "$FULL_LOG" &
FULL_LOGCAT_PID=$!

echo "=== Starting SignalGate diagnostic Logcat capture ==="
adb logcat -v threadtime -s \
    SignalGate:V \
    SignalGateScreening:V \
    CallScreeningEngine:V \
    SignalGateDecision:V \
    SignalGateData:V \
    SignalGatePersistence:V \
    > "$DIAGNOSTIC_LOG" &
DIAGNOSTIC_LOGCAT_PID=$!

echo "=== Launching app ($COMPONENT) ==="
adb shell am start -n "$COMPONENT"

echo "=== Verifying process actually started (polling up to 15s) ==="
LAUNCH_OK=0
for i in $(seq 1 15); do
    if adb shell pidof "$PACKAGE" 2>/dev/null | grep -q '[0-9]'; then
        LAUNCH_OK=1
        break
    fi
    sleep 1
done
if [ "$LAUNCH_OK" -eq 1 ]; then
    echo "App process confirmed running ($PACKAGE)"
else
    echo "::warning::adb shell pidof never saw $PACKAGE running during the 15s window"
fi

echo "=== Allowing asynchronous diagnostics to flush ==="
sleep 3

echo "=== Stopping Logcat captures ==="
kill "$FULL_LOGCAT_PID" 2>/dev/null || true
kill "$DIAGNOSTIC_LOGCAT_PID" 2>/dev/null || true
sleep 1
cp "$FULL_LOG" "$WORKSPACE_DIR/full_logcat.txt"
cp "$DIAGNOSTIC_LOG" "$WORKSPACE_DIR/signalgate_diagnostic_logcat.txt"
echo "=== SignalGate Diagnostic Logcat ==="
grep -E "SignalGate|CallScreeningEngine" "$DIAGNOSTIC_LOG" || echo "No SignalGate diagnostic lines found."
echo "=== Potential Android/framework errors from FULL Logcat ==="
grep -Ei "AndroidRuntime|FATAL EXCEPTION|Exception|Caused by|No package ID|Resources|resource|SecurityException|Timeout|ANR" "$FULL_LOG" || echo "No matching Android/framework error lines found."
echo "=== End Diagnostic Summary ==="
if [ "$LAUNCH_OK" -ne 1 ]; then
    echo "::error::App process never started. 'am start' likely targeted a component/applicationId that isn't installed, or the process died before pidof could observe it."
    echo "::error::See full_logcat.txt and signalgate_diagnostic_logcat.txt artifacts for details."
    exit 1
fi
exit 0
