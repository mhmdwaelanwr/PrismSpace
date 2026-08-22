#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ADB:-adb}"
OUT_DIR="${1:-${ROOT_DIR}/artifacts/device-validation}"
PACKAGE_NAME="com.prismspace.container"
TEST_CLASS="com.prismspace.container.EngineValidationTest"

ADB_ARGS=()
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  ADB_ARGS+=("-s" "${ANDROID_SERIAL}")
fi

adb_cmd() {
  "${ADB}" "${ADB_ARGS[@]}" "$@"
}

adb_cmd wait-for-device

SDK="$(adb_cmd shell getprop ro.build.version.sdk | tr -d '\r')"
ABI="$(adb_cmd shell getprop ro.product.cpu.abi | tr -d '\r')"
MANUFACTURER="$(adb_cmd shell getprop ro.product.manufacturer | tr -d '\r')"
MODEL="$(adb_cmd shell getprop ro.product.model | tr -d '\r')"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"

mkdir -p "${OUT_DIR}"

echo "== PrismSpace device engine validation =="
echo "device=${MANUFACTURER} ${MODEL} sdk=${SDK} abi=${ABI}"

(
  cd "${ROOT_DIR}"
  ./gradlew --no-daemon \
    :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class="${TEST_CLASS}"
)

REPORT_PATH="${OUT_DIR}/engine-validation-sdk${SDK}-${ABI}-${STAMP}.txt"
adb_cmd exec-out run-as "${PACKAGE_NAME}" cat files/diagnostics/latest.txt > "${REPORT_PATH}"

if [[ ! -s "${REPORT_PATH}" ]]; then
  echo "Device validation report is empty: ${REPORT_PATH}" >&2
  exit 3
fi

printf 'deviceManufacturer=%s\ndeviceModel=%s\ndeviceSdk=%s\ndeviceAbi=%s\n\n' \
  "${MANUFACTURER}" "${MODEL}" "${SDK}" "${ABI}" \
  > "${REPORT_PATH}.meta"

echo "Validation report: ${REPORT_PATH}"
echo "Metadata: ${REPORT_PATH}.meta"
cat "${REPORT_PATH}"
