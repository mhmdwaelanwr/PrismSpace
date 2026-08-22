#pragma once

#include <jni.h>

namespace prism::hook::dexfile {

enum DexMode {
    DEX_MODE_UNAVAILABLE = 0,
    DEX_MODE_INTERCEPT = 1,
    DEX_MODE_OBSERVE_ONLY = 2
};

// Hooks dalvik/system/DexFile native methods safely via RegisterNatives.
// Supports both file-backed and in-memory DEX loading observability.
[[nodiscard]] bool Install(JNIEnv* env);

// Returns the current active mode for Dex observability.
DexMode GetMode();

}  // namespace prism::hook::dexfile
