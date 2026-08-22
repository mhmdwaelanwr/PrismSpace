#pragma once

#include <jni.h>

#include <cstring>
#include <string>
#include <string_view>
#include <type_traits>
#include <utility>

#include "Log.h"

namespace prism::jni {

inline JavaVM*& VmStorage() {
    static JavaVM* vm = nullptr;
    return vm;
}

inline void SetJavaVm(JavaVM* vm) {
    VmStorage() = vm;
}

inline JavaVM* GetJavaVm() {
    return VmStorage();
}

class ScopedEnv final {
public:
    ScopedEnv() : ScopedEnv(GetJavaVm()) {}

    explicit ScopedEnv(JavaVM* vm) : vm_(vm) {
        if (vm_ == nullptr) {
            return;
        }

        void* raw_env = nullptr;
        const jint rc = vm_->GetEnv(&raw_env, JNI_VERSION_1_6);
        if (rc == JNI_OK) {
            env_ = static_cast<JNIEnv*>(raw_env);
            return;
        }

        if (rc == JNI_EDETACHED) {
#if defined(__ANDROID__) || defined(ANDROID)
            if (vm_->AttachCurrentThread(&env_, nullptr) == JNI_OK) {
                attached_here_ = true;
            }
#else
            if (vm_->AttachCurrentThread(reinterpret_cast<void**>(&env_), nullptr) == JNI_OK) {
                attached_here_ = true;
            }
#endif
        }
    }

    ~ScopedEnv() {
        if (attached_here_ && vm_ != nullptr) {
            vm_->DetachCurrentThread();
        }
    }

    ScopedEnv(const ScopedEnv&) = delete;
    ScopedEnv& operator=(const ScopedEnv&) = delete;

    [[nodiscard]] JNIEnv* get() const { return env_; }
    [[nodiscard]] bool ok() const { return env_ != nullptr; }
    [[nodiscard]] explicit operator bool() const { return ok(); }
    [[nodiscard]] JNIEnv* operator->() const { return env_; }
    [[nodiscard]] operator JNIEnv*() const { return env_; }

private:
    JavaVM* vm_ = nullptr;
    JNIEnv* env_ = nullptr;
    bool attached_here_ = false;
};

template <typename T>
class LocalRef final {
public:
    LocalRef() = default;
    LocalRef(JNIEnv* env, T ref) : env_(env), ref_(ref) {}

    ~LocalRef() { reset(); }

    LocalRef(const LocalRef&) = delete;
    LocalRef& operator=(const LocalRef&) = delete;

    LocalRef(LocalRef&& other) noexcept : env_(other.env_), ref_(other.release()) {}

    LocalRef& operator=(LocalRef&& other) noexcept {
        if (this != &other) {
            reset();
            env_ = other.env_;
            ref_ = other.release();
        }
        return *this;
    }

    [[nodiscard]] T get() const { return ref_; }
    [[nodiscard]] explicit operator bool() const { return ref_ != nullptr; }
    [[nodiscard]] operator T() const { return ref_; }

    T release() {
        T tmp = ref_;
        ref_ = nullptr;
        return tmp;
    }

    void reset(T next = nullptr) {
        if (env_ != nullptr && ref_ != nullptr) {
            env_->DeleteLocalRef(ref_);
        }
        ref_ = next;
    }

private:
    JNIEnv* env_ = nullptr;
    T ref_ = nullptr;
};

template <typename T>
class GlobalRef final {
public:
    GlobalRef() = default;

    GlobalRef(JNIEnv* env, T local_ref) {
        reset(env, local_ref);
    }

    ~GlobalRef() { reset(); }

    GlobalRef(const GlobalRef&) = delete;
    GlobalRef& operator=(const GlobalRef&) = delete;

    GlobalRef(GlobalRef&& other) noexcept : ref_(other.release()) {}

    GlobalRef& operator=(GlobalRef&& other) noexcept {
        if (this != &other) {
            reset();
            ref_ = other.release();
        }
        return *this;
    }

    void reset(JNIEnv* env, T local_ref) {
        reset();
        if (env != nullptr && local_ref != nullptr) {
            ref_ = static_cast<T>(env->NewGlobalRef(local_ref));
        }
    }

    void reset() {
        if (ref_ == nullptr) {
            return;
        }
        ScopedEnv env;
        if (!env.ok()) {
            PRISM_LOGE("GlobalRef reset failed: no JNIEnv");
            return;
        }
        env->DeleteGlobalRef(ref_);
        ref_ = nullptr;
    }

    [[nodiscard]] T get() const { return ref_; }
    [[nodiscard]] explicit operator bool() const { return ref_ != nullptr; }
    [[nodiscard]] operator T() const { return ref_; }

    T release() {
        T tmp = ref_;
        ref_ = nullptr;
        return tmp;
    }

private:
    T ref_ = nullptr;
};

class UtfChars final {
public:
    UtfChars(JNIEnv* env, jstring value) : env_(env), value_(value) {
        if (env_ != nullptr && value_ != nullptr) {
            chars_ = env_->GetStringUTFChars(value_, nullptr);
        }
    }

    ~UtfChars() {
        if (env_ != nullptr && value_ != nullptr && chars_ != nullptr) {
            env_->ReleaseStringUTFChars(value_, chars_);
        }
    }

    UtfChars(const UtfChars&) = delete;
    UtfChars& operator=(const UtfChars&) = delete;

    [[nodiscard]] const char* c_str() const { return chars_ != nullptr ? chars_ : ""; }
    [[nodiscard]] std::string_view view() const { return std::string_view(c_str()); }
    [[nodiscard]] std::string str() const { return std::string(c_str()); }
    [[nodiscard]] explicit operator bool() const { return chars_ != nullptr; }

private:
    JNIEnv* env_ = nullptr;
    jstring value_ = nullptr;
    const char* chars_ = nullptr;
};

inline bool CheckAndClearJniException(JNIEnv* env, const char* where) {
    if (env == nullptr || !env->ExceptionCheck()) {
        return false;
    }
    PRISM_LOGE("JNI exception at %s", where != nullptr ? where : "<unknown>");
    env->ExceptionDescribe();
    env->ExceptionClear();
    return true;
}

inline jclass FindClassGlobal(JNIEnv* env, const char* class_name) {
    if (env == nullptr || class_name == nullptr) {
        return nullptr;
    }
    LocalRef<jclass> local(env, env->FindClass(class_name));
    if (!local || CheckAndClearJniException(env, class_name)) {
        return nullptr;
    }
    return static_cast<jclass>(env->NewGlobalRef(local.get()));
}

inline std::string ToUtf8(JNIEnv* env, jstring value) {
    UtfChars chars(env, value);
    return chars.str();
}

inline jstring ToJString(JNIEnv* env, std::string_view value) {
    if (env == nullptr) {
        return nullptr;
    }
    return env->NewStringUTF(std::string(value).c_str());
}

inline jmethodID FindOptionalStaticMethod(JNIEnv* env, jclass clazz, const char* name, const char* sig) {
    if (env == nullptr || clazz == nullptr || name == nullptr || sig == nullptr) {
        return nullptr;
    }
    const jmethodID method = env->GetStaticMethodID(clazz, name, sig);
    if (CheckAndClearJniException(env, name)) {
        return nullptr;
    }
    return method;
}

inline jmethodID FindOptionalInstanceMethod(JNIEnv* env, jclass clazz, const char* name, const char* sig) {
    if (env == nullptr || clazz == nullptr || name == nullptr || sig == nullptr) {
        return nullptr;
    }
    const jmethodID method = env->GetMethodID(clazz, name, sig);
    if (CheckAndClearJniException(env, name)) {
        return nullptr;
    }
    return method;
}

inline bool RegisterNatives(JNIEnv* env,
                            jclass clazz,
                            const JNINativeMethod* methods,
                            jint method_count,
                            const char* where) {
    if (env == nullptr || clazz == nullptr || methods == nullptr || method_count <= 0) {
        return false;
    }
    if (env->RegisterNatives(clazz, methods, method_count) != JNI_OK) {
        CheckAndClearJniException(env, where);
        return false;
    }
    return true;
}

}  // namespace prism::jni
