#include "DexFileHook.h"

#include <mutex>
#include <string>
#include <string_view>
#include <dlfcn.h>
#include <vector>

#include "../JniHook/JniHook.h"
#include "../JniSafe.h"
#include "../Log.h"
#include "../PrismCore.h"

namespace prism::hook::dexfile {
namespace {

DexMode g_dex_mode = DEX_MODE_UNAVAILABLE;

// Function pointer types for modern and legacy candidates
using OpenDexFileNativeFn = jobject (*)(JNIEnv*, jclass, jstring, jstring, jint, jobject, jobjectArray);
using OpenInMemoryDexFilesNativeFn = jobject (*)(JNIEnv*, jclass, jobjectArray, jobjectArray, jintArray, jintArray, jobject, jobjectArray);
using DefineClassNativeFn = jclass (*)(JNIEnv*, jclass, jstring, jobject, jobject, jobject);
using OpenDexFileNativeLegacyFn = jlong (*)(JNIEnv*, jclass, jstring, jstring, jint);

std::mutex& InstallMutex() {
    static std::mutex mutex;
    return mutex;
}

bool& Installed() {
    static bool installed = false;
    return installed;
}

// Storage for original function pointers
OpenDexFileNativeFn& OriginalOpenDexFile() {
    static OpenDexFileNativeFn fn = nullptr;
    return fn;
}

OpenInMemoryDexFilesNativeFn& OriginalOpenInMemory() {
    static OpenInMemoryDexFilesNativeFn fn = nullptr;
    return fn;
}

DefineClassNativeFn& OriginalDefineClass() {
    static DefineClassNativeFn fn = nullptr;
    return fn;
}

OpenDexFileNativeLegacyFn& OriginalOpenDexFileLegacy() {
    static OpenDexFileNativeLegacyFn fn = nullptr;
    return fn;
}

std::string RedirectDexPath(JNIEnv* env, jstring path, const char* type) {
    if (path == nullptr || env == nullptr) {
        return {};
    }
    prism::jni::UtfChars chars(env, path);
    const std::string original_path = chars.str();

    prism::core::TraceNativeAlways("dex_open_redirect_candidate (" + std::string(type) + "): " + original_path);

    std::string redirected = prism::core::RedirectPath(original_path);
    if (redirected != original_path) {
        prism::core::TraceNativeAlways("dex_open_redirect_applied: " + original_path + " -> " + redirected);
    } else {
        prism::core::TraceNativeAlways("dex_open_passthrough");
    }
    return redirected;
}

// Hook Implementations

// Candidate 1: Modern file-backed Dex open
jobject Hook_openDexFileNative(JNIEnv* env, jclass clazz, jstring source_name, jstring output_name, jint flags, jobject loader, jobjectArray elements) {
    prism::core::TraceNativeAlways("dex_open_invoked");
    const auto original = OriginalOpenDexFile();
    if (original == nullptr) return nullptr;

    const std::string redirected_source = RedirectDexPath(env, source_name, "source");
    const std::string redirected_output = RedirectDexPath(env, output_name, "output");

    prism::jni::LocalRef<jstring> source_local(env, prism::jni::ToJString(env, redirected_source));
    prism::jni::LocalRef<jstring> output_local(env, output_name != nullptr ? prism::jni::ToJString(env, redirected_output) : nullptr);

    jstring source_arg = source_local ? source_local.get() : source_name;
    jstring output_arg = output_name != nullptr ? (output_local ? output_local.get() : output_name) : nullptr;

    jobject result = original(env, clazz, source_arg, output_arg, flags, loader, elements);
    if (result != nullptr) {
        prism::core::TraceNativeAlways("dex_open_result_success");
    } else {
        prism::core::TraceNativeAlways("dex_open_result_failure");
    }
    return result;
}

// Candidate 2: Modern in-memory Dex open
jobject Hook_openInMemoryDexFilesNative(JNIEnv* env, jclass clazz, jobjectArray buffers, jobjectArray cookies, jintArray offset, jintArray length, jobject loader, jobjectArray elements) {
    prism::core::TraceNativeAlways("dex_inmemory_invoked");
    const auto original = OriginalOpenInMemory();
    if (original == nullptr) return nullptr;

    if (buffers != nullptr) {
        jsize count = env->GetArrayLength(buffers);
        prism::core::TraceNativeAlways("dex_inmemory_passthrough (count=" + std::to_string(count) + ")");
    }

    jobject result = original(env, clazz, buffers, cookies, offset, length, loader, elements);
    if (result != nullptr) {
        prism::core::TraceNativeAlways("dex_inmemory_result_success");
    } else {
        prism::core::TraceNativeAlways("dex_inmemory_result_failure");
    }
    return result;
}

// Candidate 3: Telemetry-only class definition
jclass Hook_defineClassNative(JNIEnv* env, jclass clazz, jstring name, jobject loader, jobject cookie, jobject dexFile) {
    if (name != nullptr) {
        prism::jni::UtfChars chars(env, name);
        // Telemetry only, no policy
        // prism::core::TraceNativeAlways("dex_define_class: " + chars.str());
    }
    const auto original = OriginalDefineClass();
    if (original == nullptr) return nullptr;
    return original(env, clazz, name, loader, cookie, dexFile);
}

// Candidate 4: Legacy file-backed Dex open
jlong Hook_openDexFileNative_legacy(JNIEnv* env, jclass clazz, jstring source_name, jstring output_name, jint flags) {
    prism::core::TraceNativeAlways("dex_open_invoked (legacy)");
    const auto original = OriginalOpenDexFileLegacy();
    if (original == nullptr) return 0;

    const std::string redirected_source = RedirectDexPath(env, source_name, "source");
    const std::string redirected_output = RedirectDexPath(env, output_name, "output");

    prism::jni::LocalRef<jstring> source_local(env, prism::jni::ToJString(env, redirected_source));
    prism::jni::LocalRef<jstring> output_local(env, output_name != nullptr ? prism::jni::ToJString(env, redirected_output) : nullptr);

    jstring source_arg = source_local ? source_local.get() : source_name;
    jstring output_arg = output_name != nullptr ? (output_local ? output_local.get() : output_name) : nullptr;

    jlong result = original(env, clazz, source_arg, output_arg, flags);
    if (result != 0) {
        prism::core::TraceNativeAlways("dex_open_result_success");
    } else {
        prism::core::TraceNativeAlways("dex_open_result_failure");
    }
    return result;
}

void* FindSymbolRobust(const std::vector<const char*>& libs, const std::vector<const char*>& syms) {
    for (const char* lib : libs) {
        void* handle = dlopen(lib, RTLD_NOW);
        if (!handle) continue;
        for (const char* sym : syms) {
            void* ptr = dlsym(handle, sym);
            if (ptr) {
                prism::core::TraceNativeAlways("dex_sym_found: " + std::string(lib) + " " + std::string(sym));
                return ptr;
            }
        }
        dlclose(handle);
    }
    return nullptr;
}

struct DexCandidate {
    const char* name;
    const char* sig;
    void* hook;
    void** original_storage;
    std::vector<const char*> syms;
    bool required_for_h3;
};

}  // namespace

DexMode GetMode() {
    return g_dex_mode;
}

bool Install(JNIEnv* env) {
    if (env == nullptr) return false;
    std::lock_guard lock(InstallMutex());
    if (Installed()) return true;

    prism::core::TraceNativeAlways("dex_install_start");

    prism::jni::LocalRef<jclass> dex_class(env, env->FindClass("dalvik/system/DexFile"));
    if (!dex_class || prism::jni::CheckAndClearJniException(env, "FindClass(dalvik/system/DexFile)")) {
        prism::core::TraceNativeAlways("dex_install_failure: class_not_found");
        g_dex_mode = DEX_MODE_UNAVAILABLE;
        return false;
    }

    std::vector<const char*> libs = {"libart.so", "libopenjdk.so", "libjavacore.so"};

    std::vector<DexCandidate> candidates = {
        // Candidate 1: Modern file-backed Dex/APK/JAR open (Android 10+)
        {
            "openDexFileNative",
            "(Ljava/lang/String;Ljava/lang/String;ILjava/lang/ClassLoader;[Ldalvik/system/DexPathList$Element;)Ljava/lang/Object;",
            (void*)Hook_openDexFileNative, (void**)&OriginalOpenDexFile(),
            {"Java_dalvik_system_DexFile_openDexFileNative", "DexFile_openDexFileNative"},
            true
        },
        // Candidate 2: Modern in-memory Dex open (Android 10+)
        {
            "openInMemoryDexFilesNative",
            "([Ljava/nio/ByteBuffer;[[B[I[ILjava/lang/ClassLoader;[Ldalvik/system/DexPathList$Element;)Ljava/lang/Object;",
            (void*)Hook_openInMemoryDexFilesNative, (void**)&OriginalOpenInMemory(),
            {"Java_dalvik_system_DexFile_openInMemoryDexFilesNative", "DexFile_openInMemoryDexFilesNative"},
            true
        },
        // Candidate 3: Optional telemetry-only class definition
        {
            "defineClassNative",
            "(Ljava/lang/String;Ljava/lang/ClassLoader;Ljava/lang/Object;Ldalvik/system/DexFile;)Ljava/lang/Class;",
            (void*)Hook_defineClassNative, (void**)&OriginalDefineClass(),
            {"Java_dalvik_system_DexFile_defineClassNative", "DexFile_defineClassNative"},
            false
        },
        // Candidate 4: Legacy file-backed fallback (Android < 10)
        {
            "openDexFileNative",
            "(Ljava/lang/String;Ljava/lang/String;I)J",
            (void*)Hook_openDexFileNative_legacy, (void**)&OriginalOpenDexFileLegacy(),
            {"Java_dalvik_system_DexFile_openDexFileNative", "DexFile_openDexFileNative"},
            true
        }
    };

    bool intercepted = false;
    for (const auto& cand : candidates) {
        prism::core::TraceNativeAlways("dex_install_candidate: " + std::string(cand.name) + " " + std::string(cand.sig));

        // Use FindSymbolRobust to find the original implementation
        void* original_ptr = FindSymbolRobust(libs, cand.syms);
        if (original_ptr == nullptr) {
            prism::core::TraceNativeAlways("dex_install_symbol_not_found");
            continue;
        }

        *cand.original_storage = original_ptr;
        JNINativeMethod method[] = {{const_cast<char*>(cand.name), const_cast<char*>(cand.sig), cand.hook}};
        if (env->RegisterNatives(dex_class.get(), method, 1) == JNI_OK) {
            prism::core::TraceNativeAlways("dex_install_success: " + std::string(cand.name));
            if (cand.required_for_h3) {
                intercepted = true;
            }
        } else {
            env->ExceptionClear();
            prism::core::TraceNativeAlways("dex_install_reg_failed: " + std::string(cand.name));
            *cand.original_storage = nullptr;
        }
    }

    if (intercepted) {
        g_dex_mode = DEX_MODE_INTERCEPT;
        prism::core::TraceNativeAlways("dex_mode_intercept");
        prism::core::TraceNativeAlways("dex_install_success");
    } else {
        g_dex_mode = DEX_MODE_UNAVAILABLE;
        prism::core::TraceNativeAlways("dex_mode_unavailable");
        prism::core::TraceNativeAlways("dex_install_failure");
    }

    Installed() = intercepted;
    return intercepted;
}

}  // namespace prism::hook::dexfile
