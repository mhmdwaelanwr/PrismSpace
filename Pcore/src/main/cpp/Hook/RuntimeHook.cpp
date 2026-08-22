#include "RuntimeHook.h"

#include <mutex>
#include <string>
#include <string_view>
#include <dlfcn.h>
#include <vector>
#include <link.h>
#include <set>
#include <algorithm>

#include "../JniHook/JniHook.h"
#include "../JniSafe.h"
#include "../Log.h"
#include "../PrismCore.h"

namespace prism::hook::runtime {
namespace {

RuntimeMode g_runtime_mode = RUNTIME_MODE_UNAVAILABLE;
std::mutex g_snapshot_mutex;
std::set<std::string> g_last_snapshot;

using NativeLoad2Fn = jstring (*)(JNIEnv*, jclass, jstring, jobject);
using NativeLoad3Fn = jstring (*)(JNIEnv*, jclass, jstring, jobject, jclass);
using NativeLoad3LegacyFn = jstring (*)(JNIEnv*, jclass, jstring, jobject, jstring);
using NativeLoad3VoidFn = void (*)(JNIEnv*, jclass, jstring, jobject, jstring);

std::mutex& InstallMutex() {
    static std::mutex mutex;
    return mutex;
}

bool& Installed() {
    static bool installed = false;
    return installed;
}

NativeLoad2Fn& OriginalNativeLoad2() {
    static NativeLoad2Fn fn = nullptr;
    return fn;
}

NativeLoad3Fn& OriginalNativeLoad3() {
    static NativeLoad3Fn fn = nullptr;
    return fn;
}

NativeLoad3LegacyFn& OriginalNativeLoad3Legacy() {
    static NativeLoad3LegacyFn fn = nullptr;
    return fn;
}

NativeLoad3VoidFn& OriginalNativeLoad3Void() {
    static NativeLoad3VoidFn fn = nullptr;
    return fn;
}

void TraceLoadResult(JNIEnv* env, jstring error_string) {
    if (env == nullptr) return;
    if (prism::jni::CheckAndClearJniException(env, "Runtime.nativeLoad result check")) {
        prism::core::TraceNativeAlways("runtime_load_result_failure: exception");
        return;
    }
    if (error_string == nullptr) {
        prism::core::TraceNativeAlways("runtime_load_result_success");
    } else {
        prism::jni::UtfChars err(env, error_string);
        prism::core::TraceNativeAlways("runtime_load_result_failure: " + err.str());
    }
}

std::string RedirectLibraryPath(JNIEnv* env, jstring filename) {
    if (filename == nullptr || env == nullptr) {
        return {};
    }
    prism::jni::UtfChars chars(env, filename);
    const std::string original_path = chars.str();

    prism::core::TraceNativeAlways("runtime_load_redirect_candidate: " + original_path);

    std::string redirected = prism::core::RedirectPath(original_path);
    if (redirected != original_path) {
        prism::core::TraceNativeAlways("runtime_load_redirect_applied: " + original_path + " -> " + redirected);
    } else {
        prism::core::TraceNativeAlways("runtime_load_passthrough");
    }
    return redirected;
}

jstring NativeLoad2Hook(JNIEnv* env, jclass clazz, jstring filename, jobject loader) {
    prism::core::TraceNativeAlways("runtime_load_invoked (2-arg)");
    const auto original = OriginalNativeLoad2();
    if (original == nullptr) {
        return nullptr;
    }

    const std::string redirected_path = RedirectLibraryPath(env, filename);
    prism::jni::LocalRef<jstring> redirected_filename(env, prism::jni::ToJString(env, redirected_path));
    jstring call_filename = redirected_filename ? redirected_filename.get() : filename;

    jstring result = original(env, clazz, call_filename, loader);
    TraceLoadResult(env, result);
    return result;
}

jstring NativeLoad3Hook(JNIEnv* env, jclass clazz, jstring filename, jobject loader, jclass caller) {
    prism::core::TraceNativeAlways("runtime_load_invoked (3-arg-class)");
    const auto original = OriginalNativeLoad3();
    if (original == nullptr) {
        return nullptr;
    }

    const std::string redirected_path = RedirectLibraryPath(env, filename);
    prism::jni::LocalRef<jstring> redirected_filename(env, prism::jni::ToJString(env, redirected_path));
    jstring call_filename = redirected_filename ? redirected_filename.get() : filename;

    jstring result = original(env, clazz, call_filename, loader, caller);
    TraceLoadResult(env, result);
    return result;
}

jstring NativeLoad3LegacyHook(JNIEnv* env, jclass clazz, jstring filename, jobject loader, jstring ldLibraryPath) {
    prism::core::TraceNativeAlways("runtime_load_invoked (3-arg-legacy)");
    const auto original = OriginalNativeLoad3Legacy();
    if (original == nullptr) {
        return nullptr;
    }

    const std::string redirected_path = RedirectLibraryPath(env, filename);
    prism::jni::LocalRef<jstring> redirected_filename(env, prism::jni::ToJString(env, redirected_path));
    jstring call_filename = redirected_filename ? redirected_filename.get() : filename;

    jstring result = original(env, clazz, call_filename, loader, ldLibraryPath);
    TraceLoadResult(env, result);
    return result;
}

void NativeLoad3VoidHook(JNIEnv* env, jclass clazz, jstring filename, jobject loader, jstring ldLibraryPath) {
    prism::core::TraceNativeAlways("runtime_load_invoked (3-arg-void)");
    const auto original = OriginalNativeLoad3Void();
    if (original == nullptr) {
        return;
    }

    const std::string redirected_path = RedirectLibraryPath(env, filename);
    prism::jni::LocalRef<jstring> redirected_filename(env, prism::jni::ToJString(env, redirected_path));
    jstring call_filename = redirected_filename ? redirected_filename.get() : filename;

    original(env, clazz, call_filename, loader, ldLibraryPath);
    if (prism::jni::CheckAndClearJniException(env, "Runtime.nativeLoad void result check")) {
        prism::core::TraceNativeAlways("runtime_load_result_failure: exception");
    } else {
        prism::core::TraceNativeAlways("runtime_load_result_success");
    }
}

void* FindSymbolRobust(const std::vector<const char*>& libs, const std::vector<const char*>& syms) {
    for (const char* lib : libs) {
        void* handle = dlopen(lib, RTLD_NOW);
        if (!handle) {
            continue;
        }
        for (const char* sym : syms) {
            void* ptr = dlsym(handle, sym);
            if (ptr) {
                std::string msg = "runtime_sym_found: ";
                msg += lib;
                msg += " ";
                msg += sym;
                prism::core::TraceNativeAlways(msg);
                return ptr;
            }
        }
        dlclose(handle);
    }
    return nullptr;
}

struct PhdrContext {
    std::set<std::string>* libs;
};

int PhdrCallback(struct dl_phdr_info* info, size_t size, void* data) {
    (void)size;
    PhdrContext* context = static_cast<PhdrContext*>(data);
    if (info->dlpi_name != nullptr && info->dlpi_name[0] != '\0') {
        context->libs->insert(info->dlpi_name);
    }
    return 0;
}

std::set<std::string> CaptureSnapshot() {
    std::set<std::string> libs;
    PhdrContext context = {&libs};
    dl_iterate_phdr(PhdrCallback, &context);
    if (libs.empty()) {
        // Passive /proc/self/maps parser could go here if phdr is restricted.
        // For H2-B, phdr is the preferred stable path.
    } else {
        prism::core::TraceNativeAlways("runtime_observe_source_phdr");
    }
    return libs;
}

struct RuntimeCandidate {
    const char* name;
    const char* sig;
    void* hook;
    void** original_storage;
    std::vector<const char*> syms;
};

}  // namespace

RuntimeMode GetMode() {
    return g_runtime_mode;
}

void TakeBaselineSnapshot() {
    if (g_runtime_mode != RUNTIME_MODE_OBSERVE_ONLY) return;

    prism::core::TraceNativeAlways("runtime_observe_baseline_start");
    std::lock_guard lock(g_snapshot_mutex);
    g_last_snapshot = CaptureSnapshot();
    if (!g_last_snapshot.empty()) {
        prism::core::TraceNativeAlways("runtime_observe_baseline_success");
    } else {
        prism::core::TraceNativeAlways("runtime_observe_baseline_failure");
    }
}

void TriggerObserveDiff() {
    if (g_runtime_mode != RUNTIME_MODE_OBSERVE_ONLY) return;

    prism::core::TraceNativeAlways("runtime_observe_diff_start");
    std::set<std::string> current = CaptureSnapshot();
    std::vector<std::string> new_libs;

    {
        std::lock_guard lock(g_snapshot_mutex);
        std::set_difference(current.begin(), current.end(),
                            g_last_snapshot.begin(), g_last_snapshot.end(),
                            std::back_inserter(new_libs));
        g_last_snapshot = std::move(current);
    }

    for (const auto& lib : new_libs) {
        prism::core::TraceNativeAlways("runtime_observe_new_library: " + lib);
    }
    prism::core::TraceNativeAlways("runtime_observe_diff_success");
}

bool Install(JNIEnv* env) {
    if (env == nullptr) {
        return false;
    }

    std::lock_guard lock(InstallMutex());
    if (Installed()) {
        return true;
    }

    prism::core::TraceNativeAlways("runtime_install_start");

    prism::jni::LocalRef<jclass> runtime_class(env, env->FindClass("java/lang/Runtime"));
    if (!runtime_class || prism::jni::CheckAndClearJniException(env, "FindClass(java/lang/Runtime)")) {
        prism::core::TraceNativeAlways("runtime_install_failure: class_not_found");
        g_runtime_mode = RUNTIME_MODE_OBSERVE_ONLY;
        prism::core::TraceNativeAlways("runtime_mode_observe_only");
        return true;
    }

    std::vector<const char*> libs = {"libopenjdk.so", "libjavacore.so", "libart.so", "libcore.so"};

    std::vector<RuntimeCandidate> candidates = {
        {
            "nativeLoad",
            "(Ljava/lang/String;Ljava/lang/ClassLoader;Ljava/lang/Class;)Ljava/lang/String;",
            (void*)NativeLoad3Hook,
            (void**)&OriginalNativeLoad3(),
            {
                "Java_java_lang_Runtime_nativeLoad__Ljava_lang_String_2Ljava_lang_ClassLoader_2Ljava_lang_Class_2",
                "Java_java_lang_Runtime_nativeLoad",
                "Runtime_nativeLoad"
            }
        },
        {
            "nativeLoad",
            "(Ljava/lang/String;Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/lang/String;",
            (void*)NativeLoad3LegacyHook,
            (void**)&OriginalNativeLoad3Legacy(),
            {
                "Java_java_lang_Runtime_nativeLoad__Ljava_lang_String_2Ljava_lang_ClassLoader_2Ljava_lang_String_2",
                "Java_java_lang_Runtime_nativeLoad",
                "Runtime_nativeLoad"
            }
        },
        {
            "nativeLoad",
            "(Ljava/lang/String;Ljava/lang/ClassLoader;Ljava/lang/String;)V",
            (void*)NativeLoad3VoidHook,
            (void**)&OriginalNativeLoad3Void(),
            {
                "Java_java_lang_Runtime_nativeLoad__Ljava_lang_String_2Ljava_lang_ClassLoader_2Ljava_lang_String_2",
                "Java_java_lang_Runtime_nativeLoad",
                "Runtime_nativeLoad"
            }
        },
        {
            "nativeLoad",
            "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/String;",
            (void*)NativeLoad2Hook,
            (void**)&OriginalNativeLoad2(),
            {
                "Java_java_lang_Runtime_nativeLoad__Ljava_lang_String_2Ljava_lang_ClassLoader_2",
                "Java_java_lang_Runtime_nativeLoad",
                "Runtime_nativeLoad"
            }
        }
    };

    bool installed_any = false;

    for (const auto& cand : candidates) {
        std::string candidate_info = "runtime_install_candidate: ";
        candidate_info += cand.sig;
        prism::core::TraceNativeAlways(candidate_info);

        void* original_ptr = FindSymbolRobust(libs, cand.syms);
        if (original_ptr == nullptr) {
            prism::core::TraceNativeAlways("runtime_install_symbol_not_found");
            continue;
        }

        *cand.original_storage = original_ptr;

        JNINativeMethod method[] = {
            {const_cast<char*>(cand.name), const_cast<char*>(cand.sig), cand.hook}
        };

        if (env->RegisterNatives(runtime_class.get(), method, 1) == JNI_OK) {
            prism::core::TraceNativeAlways("runtime_install_success");
            prism::core::TraceNativeAlways("runtime_mode_intercept");
            g_runtime_mode = RUNTIME_MODE_INTERCEPT;
            installed_any = true;
            break;
        } else {
            env->ExceptionClear();
            prism::core::TraceNativeAlways("runtime_install_reg_failed");
            *cand.original_storage = nullptr;
        }
    }

    if (!installed_any) {
        prism::core::TraceNativeAlways("runtime_install_dlsym_fallback_start");
        void* ptr = FindSymbolRobust({"libjavacore.so", "libart.so", "libopenjdk.so"}, {"Runtime_nativeLoad", "Java_java_lang_Runtime_nativeLoad"});
        if (ptr) {
            prism::core::TraceNativeAlways("runtime_install_success_dlsym");
        }
        prism::core::TraceNativeAlways("runtime_install_failure");
        prism::core::TraceNativeAlways("runtime_mode_observe_only");
        g_runtime_mode = RUNTIME_MODE_OBSERVE_ONLY;
        // Even if interception fails, we return true to proceed with H2-B.
        return true;
    }

    Installed() = true;
    return true;
}

}  // namespace prism::hook::runtime
