#pragma once

#include <jni.h>

namespace prism::hook::unixfs {

// Hooks java/io/UnixFileSystem native methods that consume filesystem paths.
// String and File-backed paths are redirected through prism::core::RedirectPath
// without relying on hidden fields or reflective access.
[[nodiscard]] bool Install(JNIEnv* env);

}  // namespace prism::hook::unixfs
