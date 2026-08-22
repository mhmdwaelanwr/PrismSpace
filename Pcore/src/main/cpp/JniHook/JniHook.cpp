#include "JniHook.h"

#include <cstring>

#include "../Log.h"

namespace prism::hook {

JniHookRegistry& JniHookRegistry::Instance() {
    static JniHookRegistry registry;
    return registry;
}

void JniHookRegistry::ConfigureArtLayout(ArtMethodLayout layout) {
    std::lock_guard lock(mutex_);
    art_layout_ = layout;
}

jclass JniHookRegistry::FindHookClass(JNIEnv* env, std::string_view class_name) {
    std::lock_guard lock(mutex_);

    auto it = class_cache_.find(std::string(class_name));
    if (it != class_cache_.end()) {
        return it->second.get();
    }

    prism::jni::LocalRef<jclass> local(env, env->FindClass(std::string(class_name).c_str()));
    if (!local || prism::jni::CheckAndClearJniException(env, "FindClass(HookNativeMethod)")) {
        return nullptr;
    }

    prism::jni::GlobalRef<jclass> global(env, local.get());
    jclass out = global.get();
    class_cache_.emplace(std::string(class_name), std::move(global));
    return out;
}

bool JniHookRegistry::CaptureOriginal(JNIEnv* env, jclass clazz, const NativeHookSpec& spec) const {
    if (spec.original == nullptr) {
        return true;
    }

    if (!art_layout_.has_value() || !art_layout_->CanCaptureOriginal()) {
        PRISM_LOGE("HookNativeMethod(%s): original capture requested but ART layout is not configured",
                   spec.method_name != nullptr ? spec.method_name : "<null>");
        return false;
    }

    jmethodID method = spec.is_static
                               ? env->GetStaticMethodID(clazz, spec.method_name, spec.signature)
                               : env->GetMethodID(clazz, spec.method_name, spec.signature);
    if (method == nullptr || prism::jni::CheckAndClearJniException(env, "GetMethodID(HookNativeMethod)")) {
        return false;
    }

    const auto* raw = reinterpret_cast<const std::byte*>(method);

    if (art_layout_->access_flags_offset != 0) {
        std::uint32_t access_flags = 0;
        std::memcpy(&access_flags, raw + art_layout_->access_flags_offset, sizeof(access_flags));
        if ((access_flags & art_layout_->native_flag_mask) == 0U) {
            PRISM_LOGE("HookNativeMethod(%s): target is not marked native", spec.method_name);
            return false;
        }
    }

    void* original = nullptr;
    std::memcpy(&original, raw + art_layout_->native_entry_offset, sizeof(original));
    if (original == nullptr) {
        PRISM_LOGE("HookNativeMethod(%s): native entry is null", spec.method_name);
        return false;
    }

    *spec.original = original;
    return true;
}

bool JniHookRegistry::HookNativeMethod(JNIEnv* env, const NativeHookSpec& spec) {
    if (env == nullptr || spec.class_name == nullptr || spec.method_name == nullptr ||
        spec.signature == nullptr || spec.replacement == nullptr) {
        return false;
    }

    jclass clazz = FindHookClass(env, spec.class_name);
    if (clazz == nullptr) {
        return false;
    }

    if (!CaptureOriginal(env, clazz, spec)) {
        return false;
    }

    JNINativeMethod method[] = {{const_cast<char*>(spec.method_name),
                                 const_cast<char*>(spec.signature),
                                 spec.replacement}};

    if (!prism::jni::RegisterNatives(env, clazz, method, 1, spec.method_name)) {
        PRISM_LOGE("HookNativeMethod(%s): RegisterNatives failed", spec.method_name);
        return false;
    }

    PRISM_LOGI("HookNativeMethod: %s %s", spec.class_name, spec.method_name);
    return true;
}

}  // namespace prism::hook
