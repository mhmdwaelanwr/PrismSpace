#include "PrismCore.h"
#include <jni.h>
#include <array>
#include <mutex>
#include <string>
#include <vector>
#include <chrono>
#include <android/log.h>

#include "PolicyEngine.h"
#include "IO.h"
#include "hidden_api.h"
#include "JniSafe.h"
#include "Hook/UnixFileSystemHook.h"
#include "Hook/RuntimeHook.h"
#include "Hook/DexFileHook.h"
#include "Hook/VMClassLoaderHook.h"
#include "Hook/BinderHook.h"

namespace {
constexpr const char* kNativeTraceTag = "PrismNativeBootstrap";

std::mutex& NativeTraceMutex() { static std::mutex m; return m; }
std::string& NativeTracePath() { static std::string p; return p; }
std::vector<std::string>& NativeTraceBacklog() { static std::vector<std::string> b; return b; }

void AppendTraceLineLocked(std::string_view line) {
    const auto& path = NativeTracePath();
    if (path.empty()) {
        NativeTraceBacklog().emplace_back(line);
        return;
    }
    FILE* fp = std::fopen(path.c_str(), "ab");
    if (!fp) return;
    std::fwrite(line.data(), 1, line.size(), fp);
    std::fwrite("\n", 1, 1, fp);
    std::fclose(fp);
}
}

namespace prism::core {

void TraceNativeAlways(std::string_view event) {
    __android_log_print(ANDROID_LOG_ERROR, kNativeTraceTag, "%.*s", (int)event.size(), event.data());
    std::lock_guard lock(NativeTraceMutex());
    auto now = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count();
    AppendTraceLineLocked(std::to_string(now) + " " + std::string(event));
}

std::string RedirectPath(std::string_view path) {
    return prism::io::RedirectPath(path);
}

// Stubs/Impls for Hooks
bool ShouldHideClass(JNIEnv* env, std::string_view class_name) { return false; }
void ReportDexOpened(JNIEnv* env, std::string_view dex_path) {}
void ReportNativeLibraryLoaded(JNIEnv* env, std::string_view so_path) {}

static jboolean NativeInstallPolicyFromFd(JNIEnv*, jclass, jint fd, jlong size, jlong gen, jint transport) {
    if (fd < 0) return JNI_FALSE;
    return prism::io::PolicyEngine::Instance().InstallPolicy(fd, (size_t)size, (uint64_t)gen) ? JNI_TRUE : JNI_FALSE;
}

static void NativeSetBinderMode(JNIEnv*, jclass, jint mode) {
    prism::io::PolicyEngine::Instance().SetBinderMode((uint32_t)mode);
}

static jlongArray NativeReadBinderMetrics(JNIEnv* env, jclass) {
    auto m = prism::io::PolicyEngine::Instance().GetBinderMetrics();
    jlongArray res = env->NewLongArray(15);
    if (!res) return nullptr;
    jlong e[15] = {
        (jlong)m.total_calls, (jlong)m.shadow_candidates, (jlong)m.would_spoof_outside_scope,
        (jlong)m.would_spoof_system_uid, (jlong)m.would_spoof_self_uid, (jlong)m.validation_failures,
        (jlong)m.rollback_rejections, (jlong)m.security_exception_correlated, (jlong)m.remote_exception_correlated,
        (jlong)m.crash_correlated, (jlong)m.system_uid_calls, (jlong)m.normal_app_uid_calls,
        (jlong)m.self_uid_calls, (jlong)m.current_generation, (jlong)m.last_mode_change_elapsed_ms
    };
    env->SetLongArrayRegion(res, 0, 15, e);
    return res;
}

static void NativeIncrementBinderMetric(JNIEnv*, jclass, jint index) {
    prism::io::PolicyEngine::Instance().IncrementMetric((int)index);
}

static void NativeEnterAppScope(JNIEnv*, jclass) { prism::hook::binder::EnterAppScope(); }
static void NativeExitAppScope(JNIEnv*, jclass) { prism::hook::binder::ExitAppScope(); }

static jboolean NativeBootstrap(JNIEnv* env, jclass clazz, jstring tracePath) {
    if (tracePath) {
        prism::jni::UtfChars chars(env, tracePath);
        std::lock_guard lock(NativeTraceMutex());
        NativeTracePath() = chars.str();
        auto& backlog = NativeTraceBacklog();
        for (const auto& line : backlog) AppendTraceLineLocked(line);
        backlog.clear();
    }

    TraceNativeAlways("native_bootstrap_enter");
    prism::hook::unixfs::Install(env);
    prism::hook::runtime::Install(env);
    prism::hook::dexfile::Install(env);
    prism::hook::vmclassloader::Install(env);
    prism::hook::binder::Install(env);
    return JNI_TRUE;
}

void NativeEnableIO(JNIEnv*, jclass) {
    // Hooks are installed during nativeBootstrap; this keeps legacy lifecycle compatibility.
    TraceNativeAlways("native_enable_io");
}

void NativeAddIORule(JNIEnv* env, jclass, jstring from, jstring to) {
    if (!from || !to) {
        return;
    }
    prism::jni::UtfChars src(env, from);
    prism::jni::UtfChars dst(env, to);
    if (src.str().empty() || dst.str().empty()) {
        return;
    }
    prism::io::AddRule(src.str(), dst.str());
}

jboolean NativeDisableHiddenApi(JNIEnv* env, jclass) {
    return prism::hidden_api::TryEnable(env, prism::hidden_api::Mode::kBestEffortLegacyOnly)
            ? JNI_TRUE : JNI_FALSE;
}

jboolean NativeDisableResourceLoading(JNIEnv*, jclass) {
    // Resource hook pipeline is already initialized in bootstrap/runtime hooks.
    return JNI_TRUE;
}

void NativeNotifyMemoryPressure(JNIEnv*, jclass, jint level) {
    prism::io::PolicyEngine::Instance().HandleMemoryPressure((int) level);
}

void NativeSetAuditLogPath(JNIEnv* env, jclass, jstring path) {
    if (!path) {
        prism::io::PolicyEngine::Instance().SetAuditLogPath("");
        return;
    }
    prism::jni::UtfChars chars(env, path);
    prism::io::PolicyEngine::Instance().SetAuditLogPath(chars.str());
}

static const JNINativeMethod kMethods[] = {
    {(char*)"nativeInstallPolicyFromFd", (char*)"(IJJI)Z", (void*)NativeInstallPolicyFromFd},
    {(char*)"nativeSetBinderMode", (char*)"(I)V", (void*)NativeSetBinderMode},
    {(char*)"nativeReadBinderMetrics", (char*)"()[J", (void*)NativeReadBinderMetrics},
    {(char*)"nativeIncrementBinderMetric", (char*)"(I)V", (void*)NativeIncrementBinderMetric},
    {(char*)"nativeEnterAppScope", (char*)"()V", (void*)NativeEnterAppScope},
    {(char*)"nativeExitAppScope", (char*)"()V", (void*)NativeExitAppScope},
    {(char*)"nativeBootstrap", (char*)"(Ljava/lang/String;)Z", (void*)NativeBootstrap},
    {(char*)"nativeEnableIO", (char*)"()V", (void*)NativeEnableIO},
    {(char*)"nativeAddIORule", (char*)"(Ljava/lang/String;Ljava/lang/String;)V", (void*)NativeAddIORule},
    {(char*)"nativeDisableHiddenApi", (char*)"()Z", (void*)NativeDisableHiddenApi},
    {(char*)"nativeDisableResourceLoading", (char*)"()Z", (void*)NativeDisableResourceLoading},
    {(char*)"nativeNotifyMemoryPressure", (char*)"(I)V", (void*)NativeNotifyMemoryPressure},
    {(char*)"nativeSetAuditLogPath", (char*)"(Ljava/lang/String;)V", (void*)NativeSetAuditLogPath}
};

} // namespace prism::core

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    prism::jni::SetJavaVm(vm);
    JNIEnv* env;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;
    jclass clazz = env->FindClass("com/prismspace/container/core/NativeCore");
    if (!clazz || env->RegisterNatives(clazz, prism::core::kMethods,
            static_cast<jint>(sizeof(prism::core::kMethods) / sizeof(prism::core::kMethods[0]))) < 0) {
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL
Java_com_prismspace_container_core_NativeCore_nativeEnableIO(JNIEnv* env, jclass clazz) {
    prism::core::NativeEnableIO(env, clazz);
}

extern "C" JNIEXPORT void JNICALL
Java_com_prismspace_container_core_NativeCore_nativeAddIORule(JNIEnv* env, jclass clazz, jstring from, jstring to) {
    prism::core::NativeAddIORule(env, clazz, from, to);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_prismspace_container_core_NativeCore_nativeDisableHiddenApi(JNIEnv* env, jclass clazz) {
    return prism::core::NativeDisableHiddenApi(env, clazz);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_prismspace_container_core_NativeCore_nativeDisableResourceLoading(JNIEnv* env, jclass clazz) {
    return prism::core::NativeDisableResourceLoading(env, clazz);
}

extern "C" JNIEXPORT void JNICALL
Java_com_prismspace_container_core_NativeCore_nativeNotifyMemoryPressure(JNIEnv* env, jclass clazz, jint level) {
    prism::core::NativeNotifyMemoryPressure(env, clazz, level);
}

extern "C" JNIEXPORT void JNICALL
Java_com_prismspace_container_core_NativeCore_nativeSetAuditLogPath(JNIEnv* env, jclass clazz, jstring path) {
    prism::core::NativeSetAuditLogPath(env, clazz, path);
}
