#include "PrismCore.h"

#include <string>

namespace prism::core {

bool Initialize(JNIEnv*, jclass) {
    return true;
}

void Shutdown(JNIEnv*) {
}

int RewriteCallingUid(JNIEnv*, int original_uid) {
    return original_uid;
}

bool ShouldHideClass(JNIEnv*, std::string_view) {
    return false;
}

void ReportDexOpened(JNIEnv*, std::string_view) {
}

void ReportNativeLibraryLoaded(JNIEnv*, std::string_view) {
}

std::string RedirectPath(std::string_view path) {
    return std::string(path);
}

}  // namespace prism::core

