#include "hidden_api.h"

#include <android/api-level.h>
#include <dlfcn.h>

#include <array>

#include "JniSafe.h"
#include "Log.h"

namespace prism::hidden_api {
namespace {

using SetHiddenApiExemptionsFn = void (*)(JNIEnv*, jclass, jobjectArray);

constexpr std::array<const char*, 4> kCandidateSymbols = {
        "_ZN3artL32VMRuntime_setHiddenApiExemptionsEP7_JNIEnvP7_jclassP13_jobjectArray",
        "_ZN3art9VMRuntime22setHiddenApiExemptionsEP7_JNIEnvP7_jclassP13_jobjectArray",
        "art::VMRuntime::setHiddenApiExemptions(_JNIEnv*, _jclass*, _jobjectArray*)",
        nullptr,
};

SetHiddenApiExemptionsFn ResolveSymbol() {
    void* handle = dlopen("libart.so", RTLD_NOW);
    if (handle == nullptr) {
        PRISM_LOGW("hidden_api: dlopen(libart.so) failed");
        return nullptr;
    }

    SetHiddenApiExemptionsFn fn = nullptr;
    for (const char* symbol : kCandidateSymbols) {
        if (symbol == nullptr) {
            continue;
        }
        fn = reinterpret_cast<SetHiddenApiExemptionsFn>(dlsym(handle, symbol));
        if (fn != nullptr) {
            PRISM_LOGI("hidden_api: resolved %s", symbol);
            break;
        }
    }
    return fn;
}

}  // namespace

bool TryEnable(JNIEnv* env, Mode mode) {
    if (env == nullptr || mode == Mode::kDisabled) {
        return false;
    }

    const int api = android_get_device_api_level();
    if (api < 28) {
        return true;
    }

    // 2026 policy: do not rely on hidden-API bypass on Android 15/16 unless explicitly enabled.
    if (api >= 35 && mode != Mode::kExplicitUnsafeAllSupported) {
        PRISM_LOGW("hidden_api: bypass disabled on API %d by policy", api);
        return false;
    }

    if (mode == Mode::kBestEffortLegacyOnly && api >= 34) {
        PRISM_LOGW("hidden_api: best-effort mode limited to legacy platform behavior");
        return false;
    }

    const auto fn = ResolveSymbol();
    if (fn == nullptr) {
        return false;
    }

    prism::jni::LocalRef<jclass> vm_runtime(env, env->FindClass("dalvik/system/VMRuntime"));
    prism::jni::LocalRef<jclass> string_class(env, env->FindClass("java/lang/String"));
    if (!vm_runtime || !string_class || prism::jni::CheckAndClearJniException(env, "FindClass(hidden_api)")) {
        return false;
    }

    prism::jni::LocalRef<jobjectArray> exemptions(
            env,
            env->NewObjectArray(1, string_class.get(), prism::jni::ToJString(env, "L")));
    if (!exemptions || prism::jni::CheckAndClearJniException(env, "NewObjectArray(hidden_api)")) {
        return false;
    }

    fn(env, vm_runtime.get(), exemptions.get());
    if (prism::jni::CheckAndClearJniException(env, "setHiddenApiExemptions")) {
        return false;
    }

    PRISM_LOGI("hidden_api: exemptions call completed");
    return true;
}

}  // namespace prism::hidden_api
