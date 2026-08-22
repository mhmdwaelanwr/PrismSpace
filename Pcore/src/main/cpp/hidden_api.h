#pragma once

#include <jni.h>

namespace prism::hidden_api {

enum class Mode {
    kDisabled = 0,
    kBestEffortLegacyOnly = 1,
    kExplicitUnsafeAllSupported = 2,
};

[[nodiscard]] bool TryEnable(JNIEnv* env, Mode mode);

}  // namespace prism::hidden_api
