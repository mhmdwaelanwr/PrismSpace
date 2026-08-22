#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STAGE="${1:-A0}"
SRC_STAGE="${2:-B9}"
ABI="${3:-arm64-v8a}"
NDK_VERSION="${PRISM_NDK_VERSION:-29.0.13846066}"

# Prefer the exact side-by-side NDK requested by PrismSpace. Hosted runners often export
# ANDROID_NDK_HOME to a different preinstalled NDK, which would make a green probe misleading.
if [[ -n "${ANDROID_SDK_ROOT:-}" && -x "${ANDROID_SDK_ROOT}/ndk/${NDK_VERSION}/ndk-build" ]]; then
  NDK_ROOT="${ANDROID_SDK_ROOT}/ndk/${NDK_VERSION}"
elif [[ -n "${ANDROID_HOME:-}" && -x "${ANDROID_HOME}/ndk/${NDK_VERSION}/ndk-build" ]]; then
  NDK_ROOT="${ANDROID_HOME}/ndk/${NDK_VERSION}"
elif [[ -n "${ANDROID_NDK_HOME:-}" && -x "${ANDROID_NDK_HOME}/ndk-build" ]]; then
  SOURCE_PROPS="${ANDROID_NDK_HOME}/source.properties"
  if [[ -f "${SOURCE_PROPS}" ]] && grep -Eq "^Pkg\.Revision[[:space:]]*=[[:space:]]*${NDK_VERSION//./\.}([[:space:]]*)$" "${SOURCE_PROPS}"; then
    NDK_ROOT="${ANDROID_NDK_HOME}"
  else
    echo "ANDROID_NDK_HOME points to a different NDK; expected ${NDK_VERSION}: ${ANDROID_NDK_HOME}" >&2
    exit 2
  fi
else
  echo "Unable to locate exact Android NDK ${NDK_VERSION}" >&2
  exit 2
fi

case "${STAGE}" in
  A0|A1|A2|A3|A4) ;;
  *) echo "Invalid dependency stage: ${STAGE}" >&2; exit 2 ;;
esac

case "${SRC_STAGE}" in
  B1|B2|B3|B4|B5|B6|B7|B8|B9) ;;
  *) echo "Invalid source stage: ${SRC_STAGE}" >&2; exit 2 ;;
esac

OUT_BASE="${PRISM_NATIVE_PROBE_OUT:-${RUNNER_TEMP:-${ROOT_DIR}/.native-probe}}/${STAGE}-${SRC_STAGE}-${ABI}"
OBJ_DIR="${OUT_BASE}/obj"
LIB_DIR="${OUT_BASE}/libs"
rm -rf "${OUT_BASE}"
mkdir -p "${OBJ_DIR}" "${LIB_DIR}"

echo "== Prism native bring-up probe =="
echo "stage=${STAGE} src=${SRC_STAGE} abi=${ABI}"
echo "ndk=${NDK_ROOT}"
if [[ -f "${NDK_ROOT}/source.properties" ]]; then
  grep '^Pkg.Revision' "${NDK_ROOT}/source.properties" || true
fi

"${NDK_ROOT}/ndk-build" \
  -C "${ROOT_DIR}/Pcore/src/main" \
  NDK_PROJECT_PATH=. \
  APP_BUILD_SCRIPT=cpp/Android.mk \
  NDK_APPLICATION_MK=cpp/Application.mk \
  APP_ABI="${ABI}" \
  PRISM_DIAGNOSTIC_LAYERED_BRINGUP=true \
  PRISM_DIAGNOSTIC_DEP_STAGE="${STAGE}" \
  PRISM_DIAGNOSTIC_SRC_STAGE="${SRC_STAGE}" \
  NDK_OUT="${OBJ_DIR}" \
  NDK_LIBS_OUT="${LIB_DIR}" \
  -j2

SO_PATH="${LIB_DIR}/${ABI}/libprismspace.so"
if [[ ! -f "${SO_PATH}" ]]; then
  echo "Expected output missing: ${SO_PATH}" >&2
  exit 3
fi

PREBUILT_HOST="linux-x86_64"
case "$(uname -s)-$(uname -m)" in
  Linux-x86_64) PREBUILT_HOST="linux-x86_64" ;;
  Darwin-x86_64) PREBUILT_HOST="darwin-x86_64" ;;
  Darwin-arm64) PREBUILT_HOST="darwin-x86_64" ;;
esac
READELF="${NDK_ROOT}/toolchains/llvm/prebuilt/${PREBUILT_HOST}/bin/llvm-readelf"
if [[ -x "${READELF}" ]]; then
  echo "== ELF LOAD segments =="
  "${READELF}" -lW "${SO_PATH}" | awk '$1 == "LOAD" { print }'

  # The active diagnostic link path must be compatible with 16 KB page-size devices.
  if "${READELF}" -lW "${SO_PATH}" | awk '$1 == "LOAD" && $NF != "0x4000" { bad=1 } END { exit bad }'; then
    echo "16KB ELF alignment: OK"
  else
    echo "16KB ELF alignment: FAILED" >&2
    exit 4
  fi
else
  echo "llvm-readelf unavailable; skipping ELF alignment verification" >&2
fi

echo "Native bring-up probe ${STAGE}/${SRC_STAGE}/${ABI}: PASS"
