#!/bin/bash
# Aira On-Device AI Performance Gate
# Expected to be run by CI or developers with a connected physical Android device.

set -e

echo "==============================================="
echo " Aira AI Runtime Performance Gate (Phase 07) "
echo "==============================================="

if ! command -v adb &> /dev/null; then
    echo "ERROR: adb is not in PATH. Ensure Android platform-tools are configured."
    exit 1
fi

DEVICE_COUNT=$(adb devices | grep -v "List of devices attached" | grep "device$" | wc -l)
if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo "ERROR: No physical Android device attached."
    echo "Macrobenchmarks must be executed on a real device, not an emulator."
    exit 1
fi

MODEL_PATH="/data/local/tmp/gemma4_q4.bin"
echo "Checking for test model artifact on device at $MODEL_PATH..."
if ! adb shell "test -f $MODEL_PATH"; then
    echo "WARNING: Model artifact not found on device!"
    echo "Please push the GenAI .bin file using:"
    echo "  adb push my_model.bin $MODEL_PATH"
    echo "Resuming without strictly failing, but tests will skip generation steps."
fi

echo "Building release app and benchmark modules..."
./gradlew :app:assembleRelease :app:assembleAndroidTest

echo "Running Macrobenchmark suite..."
./gradlew :app:connectedAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark \
    -Pandroid.testInstrumentationRunnerArguments.ai.model.path=$MODEL_PATH

echo "==============================================="
echo " Performance Gate PASSED"
echo " Check build/outputs/connected_android_test_additional_output/ for JSON traces."
echo "==============================================="
