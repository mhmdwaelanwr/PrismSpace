#pragma once

#include <jni.h>

#include <string>
#include <string_view>

namespace prism::core {

bool RegisterCoreNativesForClass(JNIEnv* env, jclass native_core_class);
bool Initialize(JNIEnv* env, jclass native_core_class);
void Shutdown(JNIEnv* env);

void TraceNativeAlways(std::string_view event_name);

[[nodiscard]] int RewriteCallingUid(JNIEnv* env, int original_uid);
[[nodiscard]] bool ShouldHideClass(JNIEnv* env, std::string_view class_name);
void ReportDexOpened(JNIEnv* env, std::string_view dex_path);
void ReportNativeLibraryLoaded(JNIEnv* env, std::string_view so_path);

[[nodiscard]] std::string RedirectPath(std::string_view path);

}  // namespace prism::core
