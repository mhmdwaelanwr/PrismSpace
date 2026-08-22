#include "UnixFileSystemHook.h"

#include <mutex>
#include <string>
#include <string_view>
#include <dlfcn.h>
#include <vector>
#include <sys/stat.h>
#include <unistd.h>

#include "../JniHook/JniHook.h"
#include "../JniSafe.h"
#include "../Log.h"
#include "../PrismCore.h"

namespace prism::hook::unixfs {
namespace {

using Canonicalize0Fn = jstring (*)(JNIEnv*, jobject, jstring, jboolean);
using Canonicalize0LegacyFn = jstring (*)(JNIEnv*, jobject, jstring);
using GetBooleanAttributes0Fn = jint (*)(JNIEnv*, jobject, jobject);
using GetNameMax0Fn = jlong (*)(JNIEnv*, jobject, jstring);
using CreateFileExclusively0Fn = jboolean (*)(JNIEnv*, jobject, jstring);
using List0Fn = jobjectArray (*)(JNIEnv*, jobject, jobject);
using CreateDirectory0Fn = jboolean (*)(JNIEnv*, jobject, jobject);
using SetLastModifiedTime0Fn = jboolean (*)(JNIEnv*, jobject, jobject, jlong);
using SetReadOnly0Fn = jboolean (*)(JNIEnv*, jobject, jobject);
using GetLastModifiedTime0Fn = jlong (*)(JNIEnv*, jobject, jobject);
using GetSpace0Fn = jlong (*)(JNIEnv*, jobject, jobject, jint);
using SetPermission0Fn = jboolean (*)(JNIEnv*, jobject, jobject, jint, jboolean, jboolean);

struct FileCache final {
    prism::jni::GlobalRef<jclass> file_class;
    jmethodID ctor_string = nullptr;
    jmethodID get_path = nullptr;
    bool initialized = false;
};

FileCache& CachedFileApi() {
    static FileCache cache;
    return cache;
}

std::mutex& FileCacheMutex() {
    static std::mutex mutex;
    return mutex;
}

std::mutex& InstallMutex() {
    static std::mutex mutex;
    return mutex;
}

bool& Installed() {
    static bool installed = false;
    return installed;
}

bool EnsureFileApi(JNIEnv* env) {
    if (env == nullptr) {
        return false;
    }

    auto& cache = CachedFileApi();
    if (cache.initialized) {
        return true;
    }

    std::lock_guard lock(FileCacheMutex());
    if (cache.initialized) {
        return true;
    }

    prism::jni::LocalRef<jclass> file_class(env, env->FindClass("java/io/File"));
    if (!file_class || prism::jni::CheckAndClearJniException(env, "FindClass(java/io/File)")) {
        return false;
    }

    cache.ctor_string = env->GetMethodID(file_class.get(), "<init>", "(Ljava/lang/String;)V");
    cache.get_path = env->GetMethodID(file_class.get(), "getPath", "()Ljava/lang/String;");
    if (cache.ctor_string == nullptr || cache.get_path == nullptr ||
        prism::jni::CheckAndClearJniException(env, "File method lookup")) {
        return false;
    }

    cache.file_class.reset(env, file_class.get());
    cache.initialized = true;
    return true;
}

std::string RedirectPathTrace(std::string_view path) {
    {
        std::string msg = "unixfs_redirect_invoked: ";
        msg.append(path);
        prism::core::TraceNativeAlways(msg);
    }

    std::string redirected = prism::core::RedirectPath(path);
    if (redirected != path) {
        std::string msg = "unixfs_redirect_applied: ";
        msg.append(path);
        msg += " -> ";
        msg += redirected;
        prism::core::TraceNativeAlways(msg);
    } else {
        prism::core::TraceNativeAlways("unixfs_redirect_passthrough");
    }
    return redirected;
}

std::string ExtractFilePath(JNIEnv* env, jobject file) {
    if (env == nullptr || file == nullptr || !EnsureFileApi(env)) {
        return {};
    }

    auto& cache = CachedFileApi();
    prism::jni::LocalRef<jstring> path(
            env, static_cast<jstring>(env->CallObjectMethod(file, cache.get_path)));
    if (prism::jni::CheckAndClearJniException(env, "File.getPath")) {
        return {};
    }
    return prism::jni::ToUtf8(env, path.get());
}

jint GetBooleanAttributes0Hook(JNIEnv* env, jobject thiz, jobject file) {
    if (env == nullptr || file == nullptr) {
        return 0;
    }

    const std::string original_path = ExtractFilePath(env, file);
    if (original_path.empty()) {
        return 0;
    }

    const std::string redirected_path = RedirectPathTrace(original_path);

    struct stat st;
    if (stat(redirected_path.c_str(), &st) != 0) {
        return 0;
    }

    jint rv = 0x01; // BA_EXISTS
    if (S_ISREG(st.st_mode)) {
        rv |= 0x02; // BA_REGULAR
    }
    if (S_ISDIR(st.st_mode)) {
        rv |= 0x04; // BA_DIRECTORY
    }

    size_t last_slash = redirected_path.find_last_of('/');
    const char* name = (last_slash == std::string::npos) ? redirected_path.c_str() : &redirected_path[last_slash + 1];
    if (name[0] == '.') {
        rv |= 0x08; // BA_HIDDEN
    }

    return rv;
}

jboolean CheckAccessHook(JNIEnv* env, jobject thiz, jobject file, jint access) {
    if (env == nullptr || file == nullptr) {
        return JNI_FALSE;
    }

    const std::string original_path = ExtractFilePath(env, file);
    if (original_path.empty()) {
        return JNI_FALSE;
    }

    const std::string redirected_path = RedirectPathTrace(original_path);

    int mode = F_OK;
    if (access & 0x04) mode |= R_OK;
    if (access & 0x02) mode |= W_OK;
    if (access & 0x01) mode |= X_OK;

    return (::access(redirected_path.c_str(), mode) == 0) ? JNI_TRUE : JNI_FALSE;
}

jboolean AccessHookJString(JNIEnv* env, jobject thiz, jstring path, jint mode) {
    if (env == nullptr || path == nullptr) {
        return JNI_FALSE;
    }

    prism::jni::UtfChars chars(env, path);
    if (chars.str().empty()) {
        return JNI_FALSE;
    }

    const std::string redirected_path = RedirectPathTrace(chars.view());

    // Perform standard C++ access check on the redirected path
    if (::access(redirected_path.c_str(), mode) == 0) {
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

struct NativeMethodCandidate {
    const char* name;
    const char* sig;
    void* hook;
};

}  // namespace

bool Install(JNIEnv* env) {
    if (env == nullptr) {
        return false;
    }

    std::lock_guard lock(InstallMutex());
    if (Installed()) {
        return true;
    }

    prism::core::TraceNativeAlways("unixfs_install_start");

    if (!EnsureFileApi(env)) {
        prism::core::TraceNativeAlways("unixfs_install_file_api_failed");
        return false;
    }

    bool installed_any = false;

    // Bypassed risky methods
    prism::core::TraceNativeAlways("unixfs_hook_canonicalize0_bypassed");

    // RESILIENT JNI OVERRIDE LOOP - Standard UnixFileSystem
    {
        prism::jni::LocalRef<jclass> clazz(env, env->FindClass("java/io/UnixFileSystem"));
        if (clazz) {
            std::vector<NativeMethodCandidate> candidates = {
                {"getBooleanAttributes0", "(Ljava/io/File;)I", (void*)GetBooleanAttributes0Hook},
                {"getBooleanAttributes", "(Ljava/io/File;)I", (void*)GetBooleanAttributes0Hook},
                {"checkAccess", "(Ljava/io/File;I)Z", (void*)CheckAccessHook}
            };

            for (const auto& cand : candidates) {
                JNINativeMethod method[] = {{const_cast<char*>(cand.name), const_cast<char*>(cand.sig), cand.hook}};
                if (env->RegisterNatives(clazz.get(), method, 1) == JNI_OK) {
                    installed_any = true;
                    std::string msg = "unixfs_hook_reg_success_";
                    msg += cand.name;
                    prism::core::TraceNativeAlways(msg);
                } else {
                    env->ExceptionClear();
                }
            }
        } else {
            env->ExceptionClear();
        }
    }

    // FALLBACK JNI OVERRIDE - API 23 libcore.io.Posix/Linux
    if (!installed_any) {
        prism::core::TraceNativeAlways("unixfs_hook_fallback_loop_start");
        const char* classes[] = {"libcore/io/Posix", "libcore/io/Linux"};
        for (const char* cls_name : classes) {
            prism::jni::LocalRef<jclass> clazz(env, env->FindClass(cls_name));
            if (clazz) {
                JNINativeMethod method[] = {{"access", "(Ljava/lang/String;I)Z", (void*)AccessHookJString}};
                if (env->RegisterNatives(clazz.get(), method, 1) == JNI_OK) {
                    installed_any = true;
                    std::string msg = "unixfs_hook_fallback_reg_success_";
                    msg += cls_name;
                    prism::core::TraceNativeAlways(msg);
                    break;
                } else {
                    env->ExceptionClear();
                }
            } else {
                env->ExceptionClear();
            }
        }
    }

    // Bypassed remaining methods for H1 stability
    prism::core::TraceNativeAlways("unixfs_hook_getNameMax0_bypassed");
    prism::core::TraceNativeAlways("unixfs_hook_createFileExclusively0_bypassed");
    prism::core::TraceNativeAlways("unixfs_hook_list0_bypassed");
    prism::core::TraceNativeAlways("unixfs_hook_createDirectory0_bypassed");
    prism::core::TraceNativeAlways("unixfs_hook_setLastModifiedTime0_bypassed");
    prism::core::TraceNativeAlways("unixfs_hook_setReadOnly0_bypassed");
    prism::core::TraceNativeAlways("unixfs_hook_getLastModifiedTime0_bypassed");
    prism::core::TraceNativeAlways("unixfs_hook_getSpace0_bypassed");
    prism::core::TraceNativeAlways("unixfs_hook_setPermission0_bypassed");

    if (!installed_any) {
        prism::core::TraceNativeAlways("unixfs_install_failure_no_hooks");
        return false;
    }

    Installed() = true;
    prism::core::TraceNativeAlways("unixfs_install_success");
    return true;
}

}  // namespace prism::hook::unixfs
