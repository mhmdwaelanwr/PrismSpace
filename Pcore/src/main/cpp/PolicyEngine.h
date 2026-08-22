#pragma once

#include <atomic>
#include <cstddef>
#include <string_view>
#include <vector>
#include <mutex>
#include <queue>
#include <thread>
#include <condition_variable>
#include <chrono>

#include "PolicyBlob.h"

namespace prism::io {

struct PolicyView {
    const PolicyHeader* hdr = nullptr;
    const RootBucket*   buckets = nullptr;
    const RadixNode*    nodes = nullptr;
    const RadixEdge*    edges = nullptr;
    const PolicyRule*   rules = nullptr;
    const char*         strings = nullptr;

    const BinderPolicyHeader* binder_hdr = nullptr;
    const BinderUidMapEntry*  binder_entries = nullptr;

    void*               base = nullptr;
    size_t              size = 0;
    int                 fd = -1;

    PolicyView() = default;
    ~PolicyView();

    PolicyView(const PolicyView&) = delete;
    PolicyView& operator=(const PolicyView&) = delete;

    static PolicyView* CreateFromFd(int fd, size_t size);
};

struct BinderMetrics {
    uint64_t total_calls;
    uint64_t shadow_candidates;
    uint64_t would_spoof_outside_scope;
    uint64_t would_spoof_system_uid;
    uint64_t would_spoof_self_uid;
    uint64_t validation_failures;
    uint64_t rollback_rejections;
    uint64_t security_exception_correlated;
    uint64_t remote_exception_correlated;
    uint64_t crash_correlated;
    uint64_t system_uid_calls;
    uint64_t normal_app_uid_calls;
    uint64_t self_uid_calls;
    uint64_t current_generation;
    uint64_t last_mode_change_elapsed_ms;
};

class PolicyEngine {
public:
    static PolicyEngine& Instance();

    bool InstallPolicy(int fd, size_t size, u64 generation);

    [[nodiscard]] LookupResult Lookup(std::string_view path) const;

    bool AssembleRedirect(const LookupResult& result, std::string_view path, char* out_buf, size_t out_size, size_t* out_len) const;

    [[nodiscard]] u32 GetCallingUid(u32 original_uid, uint8_t scope) const;

    void SetBinderMode(uint32_t mode);
    [[nodiscard]] BinderMetrics GetBinderMetrics() const;

    void IncrementMetric(int index);

    void AuditLog(std::string_view event);
    void SetAuditLogPath(std::string path);
    void HandleMemoryPressure(int level);

    static bool ValidatePolicy(const void* base, size_t size);
    static u32 CalculateChecksum(const void* base, size_t size);

private:
    PolicyEngine();
    ~PolicyEngine();

    static u32 ChooseBetter(const PolicyView& p, u32 current_best_idx, u32 new_rule_idx, size_t matched_len, size_t path_len);
    static const RadixEdge* FindMatchingEdge(const PolicyView& p, const RadixNode& node, u8 first_byte);

    std::atomic<PolicyView*> active_policy_{nullptr};

    mutable std::mutex retire_mutex_;
    std::vector<PolicyView*> retired_policies_;
    size_t total_retired_bytes_ = 0;

    void RetireOldPoliciesLocked(bool force_all = false);

    std::atomic<uint32_t> binder_runtime_mode_{BINDER_MODE_SHADOW};
    std::chrono::steady_clock::time_point last_mode_change_time_;

    mutable std::atomic<uint64_t> total_calls_{0};
    mutable std::atomic<uint64_t> shadow_candidates_{0};
    mutable std::atomic<uint64_t> would_spoof_outside_scope_{0};
    mutable std::atomic<uint64_t> would_spoof_system_uid_{0};
    mutable std::atomic<uint64_t> would_spoof_self_uid_{0};
    mutable std::atomic<uint64_t> validation_failures_{0};
    mutable std::atomic<uint64_t> rollback_rejections_{0};
    mutable std::atomic<uint64_t> security_exception_correlated_{0};
    mutable std::atomic<uint64_t> remote_exception_correlated_{0};
    mutable std::atomic<uint64_t> crash_correlated_{0};
    mutable std::atomic<uint64_t> system_uid_calls_{0};
    mutable std::atomic<uint64_t> normal_app_uid_calls_{0};
    mutable std::atomic<uint64_t> self_uid_calls_{0};

    void LoggerThreadLoop();
    std::thread logger_thread_;
    std::mutex logger_mutex_;
    std::condition_variable logger_cv_;
    std::queue<std::string> logger_queue_;
    std::atomic<bool> logger_running_{true};
    std::string audit_log_path_;
};

}  // namespace prism::io
