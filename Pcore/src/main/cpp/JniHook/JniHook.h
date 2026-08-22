#pragma once

#include <cstddef>
#include <cstdint>
#include <mutex>
#include <optional>
#include <string>
#include <string_view>
#include <unordered_map>

#include "../JniSafe.h"

namespace prism::hook {

struct ArtMethodLayout final {
    std::size_t native_entry_offset = 0;
    std::size_t access_flags_offset = 0;
    std::uint32_t native_flag_mask = 0x0100;

    [[nodiscard]] bool CanCaptureOriginal() const {
        return native_entry_offset != 0;
    }
};

struct NativeHookSpec final {
    const char* class_name = nullptr;
    const char* method_name = nullptr;
    const char* signature = nullptr;
    bool is_static = true;
    void* replacement = nullptr;
    void** original = nullptr;
};

class JniHookRegistry final {
public:
    static JniHookRegistry& Instance();

    void ConfigureArtLayout(ArtMethodLayout layout);
    [[nodiscard]] bool HookNativeMethod(JNIEnv* env, const NativeHookSpec& spec);

private:
    JniHookRegistry() = default;

    [[nodiscard]] jclass FindHookClass(JNIEnv* env, std::string_view class_name);
    [[nodiscard]] bool CaptureOriginal(JNIEnv* env, jclass clazz, const NativeHookSpec& spec) const;

    mutable std::mutex mutex_;
    std::optional<ArtMethodLayout> art_layout_;
    std::unordered_map<std::string, prism::jni::GlobalRef<jclass>> class_cache_;
};

}  // namespace prism::hook
