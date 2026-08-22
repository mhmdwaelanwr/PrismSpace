#pragma once

#include <jni.h>

namespace prism::hook::vmclassloader {

enum VMCLMode {
    VMCL_MODE_UNAVAILABLE = 0,
    VMCL_MODE_INTERCEPT = 1
};

// Hooks java/lang/VMClassLoader.findLoadedClass safely via RegisterNatives.
// Passive/logging-first bring-up for class visibility observability.
[[nodiscard]] bool Install(JNIEnv* env);

// Returns the current active mode for VMClassLoader observability.
VMCLMode GetMode();

}  // namespace prism::hook::vmclassloader
