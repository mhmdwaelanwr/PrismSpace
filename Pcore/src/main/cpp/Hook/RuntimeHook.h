#pragma once

#include <jni.h>

namespace prism::hook::runtime {

enum RuntimeMode {
    RUNTIME_MODE_UNAVAILABLE = 0,
    RUNTIME_MODE_INTERCEPT = 1,
    RUNTIME_MODE_OBSERVE_ONLY = 2
};

// Hooks java/lang/Runtime.nativeLoad overloads or activates observe-only mode.
[[nodiscard]] bool Install(JNIEnv* env);

// Returns the current active mode for Runtime observability.
RuntimeMode GetMode();

// Takes a baseline snapshot of loaded libraries.
// Should be called from the async bootstrap thread if in OBSERVE_ONLY mode.
void TakeBaselineSnapshot();

// Performs a diff of currently loaded libraries against the last snapshot
// and traces any new library loads.
void TriggerObserveDiff();

}  // namespace prism::hook::runtime
