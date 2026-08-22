#include "VMClassLoaderHook.h"

#include <mutex>
#include <string>
#include <vector>
#include <dlfcn.h>

#include "../JniHook/JniHook.h"
#include "../JniSafe.h"
#include "../Log.h"
#include "../PrismCore.h"

namespace prism::hook::vmclassloader {
namespace {

VMCLMode g_vmcl_mode = VMCL_MODE_UNAVAILABLE;

using FindLoadedClassFn = jclass (*)(JNIEnv*, jclass, jobject, jstring);

std::mutex& InstallMutex() {
    static std::mutex mutex;
    return mutex;
}

bool& Installed() {
    static bool installed = false;
    return installed;
}

FindLoadedClassFn& OriginalFindLoadedClass() {
    static FindLoadedClassFn fn = nullptr;
    return fn;
}

jclass FindLoadedClassHook(JNIEnv* env, jclass clazz, jobject loader, jstring name) {
    prism::core::TraceNativeAlways("vmcl_find_loaded_invoked");

    std::string class_name_str;
    if (name != nullptr) {
        prism::jni::UtfChars chars(env, name);
        class_name_str = chars.str();
        prism::core::TraceNativeAlways("vmcl_find_loaded_query: " + class_name_str);
    }

    const auto original = OriginalFindLoadedClass();
    if (original == nullptr || env == nullptr) {
        return nullptr;
    }

    // Call original implementation (passthrough)
    jclass result = original(env, clazz, loader, name);

    if (result != nullptr) {
        prism::core::TraceNativeAlways("vmcl_find_loaded_result_hit");
    } else {
        prism::core::TraceNativeAlways("vmcl_find_loaded_result_miss");
    }

    // Passive policy evaluation for H4 bring-up (no enforcement)
    if (!class_name_str.empty()) {
        prism::core::TraceNativeAlways("vmcl_hide_policy_considered");
        // We use ShouldHideClass logic only for telemetry if safe
        if (prism::core::ShouldHideClass(env, class_name_str)) {
            prism::core::TraceNativeAlways("vmcl_hide_policy_would_hide: " + class_name_str);
        } else {
            prism::core::TraceNativeAlways("vmcl_hide_policy_would_allow: " + class_name_str);
        }
    }

    prism::core::TraceNativeAlways("vmcl_find_loaded_passthrough");
    return result;
}

void* FindSymbolRobust(const std::vector<const char*>& libs, const std::vector<const char*>& syms) {
    for (const char* lib : libs) {
        void* handle = dlopen(lib, RTLD_NOW);
        if (!handle) continue;
        for (const char* sym : syms) {
            void* ptr = dlsym(handle, sym);
            if (ptr) {
                prism::core::TraceNativeAlways("vmcl_sym_found: " + std::string(lib) + " " + std::string(sym));
                return ptr;
            }
        }
        dlclose(handle);
    }
    return nullptr;
}

struct VMCLCandidate {
    const char* name;
    const char* sig;
    void* hook;
    void** original_storage;
    std::vector<const char*> syms;
};

}  // namespace

VMCLMode GetMode() {
    return g_vmcl_mode;
}

bool Install(JNIEnv* env) {
    if (env == nullptr) return false;
    std::lock_guard lock(InstallMutex());
    if (Installed()) return true;

    prism::core::TraceNativeAlways("vmcl_install_start");

    prism::jni::LocalRef<jclass> vmcl_class(env, env->FindClass("java/lang/VMClassLoader"));
    if (!vmcl_class || prism::jni::CheckAndClearJniException(env, "FindClass(java/lang/VMClassLoader)")) {
        prism::core::TraceNativeAlways("vmcl_install_failure: class_not_found");
        g_vmcl_mode = VMCL_MODE_UNAVAILABLE;
        return false;
    }

    std::vector<const char*> libs = {"libart.so", "libopenjdk.so", "libjavacore.so"};

    std::vector<VMCLCandidate> candidates = {
        {
            "findLoadedClass",
            "(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/lang/Class;",
            (void*)FindLoadedClassHook,
            (void**)&OriginalFindLoadedClass(),
            {
                "Java_java_lang_VMClassLoader_findLoadedClass",
                "VMClassLoader_findLoadedClass"
            }
        }
    };

    bool installed_any = false;
    for (const auto& cand : candidates) {
        prism::core::TraceNativeAlways("vmcl_install_candidate: " + std::string(cand.name) + " " + std::string(cand.sig));

        void* original_ptr = FindSymbolRobust(libs, cand.syms);
        if (original_ptr == nullptr) {
            prism::core::TraceNativeAlways("vmcl_install_symbol_not_found");
            continue;
        }

        *cand.original_storage = original_ptr;
        JNINativeMethod method[] = {{const_cast<char*>(cand.name), const_cast<char*>(cand.sig), cand.hook}};
        if (env->RegisterNatives(vmcl_class.get(), method, 1) == JNI_OK) {
            prism::core::TraceNativeAlways("vmcl_install_success: " + std::string(cand.name));
            installed_any = true;
            break;
        } else {
            env->ExceptionClear();
            prism::core::TraceNativeAlways("vmcl_install_reg_failed: " + std::string(cand.name));
            *cand.original_storage = nullptr;
        }
    }

    if (installed_any) {
        g_vmcl_mode = VMCL_MODE_INTERCEPT;
        prism::core::TraceNativeAlways("vmcl_mode_intercept");
    } else {
        g_vmcl_mode = VMCL_MODE_UNAVAILABLE;
        prism::core::TraceNativeAlways("vmcl_mode_unavailable");
        prism::core::TraceNativeAlways("vmcl_install_failure");
    }

    Installed() = installed_any;
    return installed_any;
}

}  // namespace prism::hook::vmclassloader
