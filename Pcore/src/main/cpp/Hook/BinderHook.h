#pragma once

#include <jni.h>
#include <cstdint>

namespace prism::hook::binder {

enum BinderMode {
    BINDER_MODE_UNAVAILABLE = 0,
    BINDER_MODE_INTERCEPT = 1
};

// TLS Scope for Binder Spoofing
enum BinderScope : uint8_t {
    BINDER_SCOPE_NONE = 0,
    BINDER_SCOPE_APP_SERVER_TXN = 1  // Spoofing allowed only in this scope
};

// Hooks android/os/Binder.getCallingUid safely via RegisterNatives.
[[nodiscard]] bool Install(JNIEnv* env);

// Returns the current active mode for Binder observability.
BinderMode GetMode();

// JNI Control for TLS Scope
void EnterAppScope();
void ExitAppScope();

}  // namespace prism::hook::binder
