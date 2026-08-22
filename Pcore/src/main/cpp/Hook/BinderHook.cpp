#include "BinderHook.h"

#include <mutex>
#include <string>
#include <vector>
#include <dlfcn.h>

#include "../JniHook/JniHook.h"
#include "../JniSafe.h"
#include "../Log.h"
#include "../PrismCore.h"
#include "../PolicyEngine.h"

namespace prism::hook::binder {
namespace {

BinderMode g_binder_mode = BINDER_MODE_UNAVAILABLE;

// H5: Depth-based TLS Scope Filtering to handle recursion safely.
// Increment on enter, decrement on exit. Active if > 0.
thread_local uint32_t tls_scope_depth = 0;

using GetCallingUidFn = jint (*)();

std::mutex& InstallMutex() {
    static std::mutex mutex;
    return mutex;
}

bool& Installed() {
    static bool installed = false;
    return installed;
}

GetCallingUidFn& OriginalGetCallingUid() {
    static GetCallingUidFn fn = nullptr;
    return fn;
}

/**
 * H5 Binder Identity Spoofing Hook with Recursion-Safe TLS Scope.
 */
jint GetCallingUidHook() {
    const auto original = OriginalGetCallingUid();
    jint real_uid = (original != nullptr) ? original() : -1;

    // Delegate to validated PolicyEngine.
    // Scope is active if depth > 0.
    uint8_t active_scope = (tls_scope_depth > 0) ? BINDER_SCOPE_APP_SERVER_TXN : BINDER_SCOPE_NONE;

    return static_cast<jint>(prism::io::PolicyEngine::Instance().GetCallingUid(
        static_cast<uint32_t>(real_uid), active_scope));
}

void* FindSymbolRobust(const std::vector<const char*>& libs, const std::vector<const char*>& syms) {
    for (const char* lib : libs) {
        void* handle = dlopen(lib, RTLD_NOW);
        if (!handle) continue;
        for (const char* sym : syms) {
            void* ptr = dlsym(handle, sym);
            if (ptr) return ptr;
        }
        dlclose(handle);
    }
    return nullptr;
}

}  // namespace

BinderMode GetMode() {
    return g_binder_mode;
}

void EnterAppScope() {
    tls_scope_depth++;
}

void ExitAppScope() {
    if (tls_scope_depth > 0) {
        tls_scope_depth--;
    }
}

bool Install(JNIEnv* env) {
    if (env == nullptr) return false;
    std::lock_guard lock(InstallMutex());
    if (Installed()) return true;

    prism::core::TraceNativeAlways("binder_install_start");

    prism::jni::LocalRef<jclass> binder_class(env, env->FindClass("android/os/Binder"));
    if (!binder_class || prism::jni::CheckAndClearJniException(env, "FindClass(android/os/Binder)")) {
        g_binder_mode = BINDER_MODE_UNAVAILABLE;
        return false;
    }

    std::vector<const char*> libs = {"libbinder_ndk.so", "libbinder.so", "libart.so"};
    std::vector<const char*> syms = {"Java_android_os_Binder_getCallingUid", "Binder_getCallingUid"};

    void* original_ptr = FindSymbolRobust(libs, syms);
    OriginalGetCallingUid() = reinterpret_cast<GetCallingUidFn>(original_ptr);

    JNINativeMethod method[] = {{(char*)"getCallingUid", (char*)"()I", (void*)GetCallingUidHook}};

    if (env->RegisterNatives(binder_class.get(), method, 1) == JNI_OK) {
        prism::core::TraceNativeAlways("binder_install_success");
        g_binder_mode = BINDER_MODE_INTERCEPT;
        Installed() = true;
        return true;
    }

    env->ExceptionClear();
    g_binder_mode = BINDER_MODE_UNAVAILABLE;
    return false;
}

}  // namespace prism::hook::binder
