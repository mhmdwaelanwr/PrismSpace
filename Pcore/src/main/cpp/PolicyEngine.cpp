#include "PolicyEngine.h"

#include <sys/mman.h>
#include <unistd.h>
#include <cstring>
#include <algorithm>
#include <fstream>
#include <chrono>
#include <zlib.h>

#include "Log.h"

namespace prism::io {

namespace {
constexpr size_t kMaxRetiredMemoryBudget = 32 * 1024 * 1024;
constexpr size_t kMaxRetiredCount = 8;
}

PolicyView::~PolicyView() {
    if (base && size > 0) {
        munmap(base, size);
    }
    if (fd >= 0) {
        close(fd);
    }
}

PolicyView* PolicyView::CreateFromFd(int fd, size_t size) {
    if (fd < 0 || size < sizeof(PolicyHeader)) {
        return nullptr;
    }

    void* base = mmap(nullptr, size, PROT_READ, MAP_SHARED, fd, 0);
    close(fd);

    if (base == MAP_FAILED) {
        PRISM_LOGE("PolicyView: mmap failed");
        return nullptr;
    }

    if (!PolicyEngine::ValidatePolicy(base, size)) {
        PRISM_LOGE("PolicyView: Policy validation failed");
        munmap(base, size);
        return nullptr;
    }

    auto* hdr = static_cast<const PolicyHeader*>(base);
    auto* view = new PolicyView();
    view->base = base;
    view->size = size;
    view->fd = -1;
    view->hdr = hdr;

    auto ptr_at = [base](u32 off) -> const void* {
        return static_cast<const u8*>(base) + off;
    };

    view->buckets = static_cast<const RootBucket*>(ptr_at(hdr->bucket_offset));
    view->nodes = static_cast<const RadixNode*>(ptr_at(hdr->node_offset));
    view->edges = static_cast<const RadixEdge*>(ptr_at(hdr->edge_offset));
    view->rules = static_cast<const PolicyRule*>(ptr_at(hdr->rule_offset));
    view->strings = static_cast<const char*>(ptr_at(hdr->string_offset));

    if (hdr->flags & POLICY_FLAG_HAS_BINDER) {
        view->binder_hdr = static_cast<const BinderPolicyHeader*>(ptr_at(hdr->binder_policy_offset));
        view->binder_entries = static_cast<const BinderUidMapEntry*>(ptr_at(view->binder_hdr->entry_offset));
    }

    return view;
}

PolicyEngine& PolicyEngine::Instance() {
    static PolicyEngine engine;
    return engine;
}

PolicyEngine::PolicyEngine() {
    last_mode_change_time_ = std::chrono::steady_clock::now();
    logger_thread_ = std::thread(&PolicyEngine::LoggerThreadLoop, this);
}

PolicyEngine::~PolicyEngine() {
    {
        std::lock_guard lock(logger_mutex_);
        logger_running_ = false;
    }
    logger_cv_.notify_all();
    if (logger_thread_.joinable()) {
        logger_thread_.join();
    }

    PolicyView* p = active_policy_.exchange(nullptr);
    delete p;
    std::lock_guard lock(retire_mutex_);
    for (auto* old : retired_policies_) {
        delete old;
    }
}

bool PolicyEngine::ValidatePolicy(const void* base, size_t size) {
    if (size < sizeof(PolicyHeader)) return false;

    auto* hdr = static_cast<const PolicyHeader*>(base);

    if (hdr->magic != kPolicyMagic) return false;
    if (hdr->version != kPolicyVersion) return false;
    if (hdr->total_size != size) return false;

    PolicyView* current = PolicyEngine::Instance().active_policy_.load(std::memory_order_relaxed);
    if (current && hdr->generation <= current->hdr->generation) {
        PRISM_LOGE("PolicyEngine: Rejecting stale generation %llu", hdr->generation);
        return false;
    }

    u32 actual_crc = CalculateChecksum(base, size);
    if (hdr->checksum32 != actual_crc) {
        PRISM_LOGE("PolicyEngine: CRC32 mismatch 0x%08X vs 0x%08X", hdr->checksum32, actual_crc);
        return false;
    }

    auto check_bounds = [&](u32 offset, u32 count, size_t elem_size) {
        if (count == 0) return true;
        u64 end = static_cast<u64>(offset) + (static_cast<u64>(count) * elem_size);
        return end <= size;
    };

    if (!check_bounds(hdr->bucket_offset, hdr->bucket_count, sizeof(RootBucket))) return false;
    if (!check_bounds(hdr->node_offset, hdr->node_count, sizeof(RadixNode))) return false;
    if (!check_bounds(hdr->edge_offset, hdr->edge_count, sizeof(RadixEdge))) return false;
    if (!check_bounds(hdr->rule_offset, hdr->rule_count, sizeof(PolicyRule))) return false;
    if (!check_bounds(hdr->string_offset, hdr->string_size, 1)) return false;

    if (hdr->flags & POLICY_FLAG_HAS_BINDER) {
        if (!check_bounds(hdr->binder_policy_offset, 1, sizeof(BinderPolicyHeader))) return false;
        auto* b_hdr = reinterpret_cast<const BinderPolicyHeader*>(static_cast<const u8*>(base) + hdr->binder_policy_offset);
        if (!check_bounds(b_hdr->entry_offset, b_hdr->entry_count, sizeof(BinderUidMapEntry))) return false;
    }

    return true;
}

u32 PolicyEngine::CalculateChecksum(const void* base, size_t size) {
    u32 crc = crc32(0L, Z_NULL, 0);
    const u8* data = static_cast<const u8*>(base);
    crc = crc32(crc, data, 20);
    crc = crc32(crc, data + 24, size - 24);
    return crc;
}

bool PolicyEngine::InstallPolicy(int fd, size_t size, u64 generation) {
    (void)generation;
    PolicyView* next = PolicyView::CreateFromFd(fd, size);
    if (!next) {
        validation_failures_.fetch_add(1, std::memory_order_relaxed);
        return false;
    }

    PolicyView* old = active_policy_.exchange(next, std::memory_order_release);

    if (old) {
        std::lock_guard lock(retire_mutex_);
        retired_policies_.push_back(old);
        total_retired_bytes_ += old->size;
        RetireOldPoliciesLocked();
    }

    AuditLog("policy_installed gen=" + std::to_string(next->hdr->generation));
    return true;
}

void PolicyEngine::RetireOldPoliciesLocked(bool force_all) {
    while (!retired_policies_.empty() &&
           (force_all || total_retired_bytes_ > kMaxRetiredMemoryBudget || retired_policies_.size() > kMaxRetiredCount)) {
        PolicyView* p = retired_policies_.front();
        retired_policies_.erase(retired_policies_.begin());
        total_retired_bytes_ -= p->size;
        delete p;
    }
}

void PolicyEngine::HandleMemoryPressure(int level) {
    std::lock_guard lock(retire_mutex_);
    RetireOldPoliciesLocked(true);
    AuditLog("memory_pressure_handled level=" + std::to_string(level));
}

u32 PolicyEngine::GetCallingUid(u32 original_uid, uint8_t scope) const {
    total_calls_.fetch_add(1, std::memory_order_relaxed);

    PolicyView* p_ptr = active_policy_.load(std::memory_order_acquire);
    if (!p_ptr || !(p_ptr->hdr->flags & POLICY_FLAG_HAS_BINDER)) {
        return original_uid;
    }
    const PolicyView& p = *p_ptr;

    // Monitor original UID types
    if (original_uid < 10000) {
        system_uid_calls_.fetch_add(1, std::memory_order_relaxed);
    } else {
        normal_app_uid_calls_.fetch_add(1, std::memory_order_relaxed);
    }

    // Safety: No spoofing for system/root UIDs
    if (original_uid < 10000) {
        return original_uid;
    }

    u32 app_id = original_uid % 100000;

    // AppId spoofing range (10000-19999)
    if (app_id >= 10000 && app_id < 20000) {
        u32 index = app_id - 10000;
        if (index < p.binder_hdr->entry_count) {
            u16 spoofed_app_id = p.binder_entries[index].spoof_app_id;
            if (spoofed_app_id != 0) {
                u32 user_id = original_uid / 100000;
                u32 spoofed_uid = (user_id * 100000) + spoofed_app_id;

                uint32_t current_mode = binder_runtime_mode_.load(std::memory_order_relaxed);

                if (current_mode == BINDER_MODE_SHADOW) {
                    shadow_candidates_.fetch_add(1, std::memory_order_relaxed);
                    if (original_uid < 10000) would_spoof_system_uid_.fetch_add(1, std::memory_order_relaxed);
                    else would_spoof_self_uid_.fetch_add(1, std::memory_order_relaxed);

                    std::string log = "binder_shadow_match scope=" + std::to_string(static_cast<int>(scope)) +
                                      ": " + std::to_string(original_uid) + " -> " + std::to_string(spoofed_uid);
                    const_cast<PolicyEngine*>(this)->AuditLog(log);
                    return original_uid;
                }

                // Enforcement requires correct scope
                if (scope == 1) { // BINDER_SCOPE_APP_SERVER_TXN
                    self_uid_calls_.fetch_add(1, std::memory_order_relaxed);
                    return spoofed_uid;
                } else {
                    would_spoof_outside_scope_.fetch_add(1, std::memory_order_relaxed);
                }
            }
        }
    }

    return original_uid;
}

void PolicyEngine::SetBinderMode(uint32_t mode) {
    binder_runtime_mode_.store(mode, std::memory_order_relaxed);
    last_mode_change_time_ = std::chrono::steady_clock::now();
    AuditLog("binder_mode_updated: " + std::to_string(mode));
}

void PolicyEngine::IncrementMetric(int index) {
    switch (index) {
        case 5: validation_failures_.fetch_add(1, std::memory_order_relaxed); break;
        case 6: rollback_rejections_.fetch_add(1, std::memory_order_relaxed); break;
        case 7: security_exception_correlated_.fetch_add(1, std::memory_order_relaxed); break;
        case 8: remote_exception_correlated_.fetch_add(1, std::memory_order_relaxed); break;
        case 9: crash_correlated_.fetch_add(1, std::memory_order_relaxed); break;
        default: break;
    }
}

BinderMetrics PolicyEngine::GetBinderMetrics() const {
    auto now = std::chrono::steady_clock::now();
    auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(now - last_mode_change_time_).count();

    PolicyView* current = active_policy_.load(std::memory_order_relaxed);
    uint64_t gen = current ? current->hdr->generation : 0;

    return {
        total_calls_.load(std::memory_order_relaxed),                   // [0]
        shadow_candidates_.load(std::memory_order_relaxed),             // [1]
        would_spoof_outside_scope_.load(std::memory_order_relaxed),     // [2]
        would_spoof_system_uid_.load(std::memory_order_relaxed),        // [3]
        would_spoof_self_uid_.load(std::memory_order_relaxed),          // [4]
        validation_failures_.load(std::memory_order_relaxed),           // [5]
        rollback_rejections_.load(std::memory_order_relaxed),           // [6]
        security_exception_correlated_.load(std::memory_order_relaxed), // [7]
        remote_exception_correlated_.load(std::memory_order_relaxed),   // [8]
        crash_correlated_.load(std::memory_order_relaxed),              // [9]
        system_uid_calls_.load(std::memory_order_relaxed),              // [10]
        normal_app_uid_calls_.load(std::memory_order_relaxed),          // [11]
        self_uid_calls_.load(std::memory_order_relaxed),                // [12]
        gen,                                                            // [13]
        static_cast<uint64_t>(elapsed)                                  // [14]
    };
}

LookupResult PolicyEngine::Lookup(std::string_view path) const {
    PolicyView* p_ptr = active_policy_.load(std::memory_order_acquire);
    if (!p_ptr) {
        return {LOOKUP_MISS, kInvalidIndex, 0, 0, 0};
    }
    const PolicyView& p = *p_ptr;

    const char* data = path.data();
    size_t len = path.size();
    u32 node_idx = p.hdr->root_node_index;
    u32 best_rule = kInvalidIndex;
    size_t pos = 0;

    while (true) {
        const RadixNode& node = p.nodes[node_idx];

        if (node.prefix_rule_index != kInvalidIndex) {
            if (pos == len || data[pos] == '/') {
                best_rule = ChooseBetter(p, best_rule, node.prefix_rule_index, pos, len);
            }
        }

        if (pos == len && node.exact_rule_index != kInvalidIndex) {
            best_rule = ChooseBetter(p, best_rule, node.exact_rule_index, pos, len);
            break;
        }

        if (pos >= len) break;

        const RadixEdge* edge = FindMatchingEdge(p, node, static_cast<u8>(data[pos]));
        if (!edge) break;

        size_t remaining_path = len - pos;
        if (remaining_path < edge->label_len) break;

        if (std::memcmp(data + pos, edge->inline_prefix, edge->inline_size) != 0) {
            break;
        }

        if (edge->label_len > edge->inline_size) {
            const char* full_label = p.strings + edge->label_off;
            if (std::memcmp(data + pos + edge->inline_size,
                           full_label + edge->inline_size,
                           edge->label_len - edge->inline_size) != 0) {
                break;
            }
        }

        pos += edge->label_len;
        node_idx = edge->child_node_index;
    }

    if (best_rule == kInvalidIndex) {
        return {LOOKUP_MISS, kInvalidIndex, 0, 0, 0};
    }

    const PolicyRule& r = p.rules[best_rule];
    if (r.kind == RULE_ALLOW_PREFIX || r.kind == RULE_ALLOW_EXACT) {
        return {LOOKUP_ALLOW_ORIGINAL, best_rule, r.src_len, 0, 0};
    }

    return {LOOKUP_REDIRECT, best_rule, r.src_len, r.dst_off, r.dst_len};
}

bool PolicyEngine::AssembleRedirect(const LookupResult& result, std::string_view path, char* out_buf, size_t out_size, size_t* out_len) const {
    if (result.action != LOOKUP_REDIRECT) return false;

    PolicyView* p_ptr = active_policy_.load(std::memory_order_acquire);
    if (!p_ptr) return false;
    const PolicyView& p = *p_ptr;

    const char* dst_prefix = p.strings + result.dst_off;
    size_t dst_prefix_len = result.dst_len;

    std::string_view suffix = path.substr(result.matched_len);
    size_t total_len = dst_prefix_len + suffix.size();

    if (total_len >= out_size) {
        return false;
    }

    std::memcpy(out_buf, dst_prefix, dst_prefix_len);
    std::memcpy(out_buf + dst_prefix_len, suffix.data(), suffix.size());
    out_buf[total_len] = '\0';

    if (out_len) *out_len = total_len;
    return true;
}

void PolicyEngine::AuditLog(std::string_view event) {
    if (!logger_running_) return;
    std::lock_guard lock(logger_mutex_);
    if (logger_queue_.size() < 1000) {
        logger_queue_.emplace(event);
        logger_cv_.notify_one();
    }
}

void PolicyEngine::SetAuditLogPath(std::string path) {
    std::lock_guard lock(logger_mutex_);
    audit_log_path_ = std::move(path);
}

void PolicyEngine::LoggerThreadLoop() {
    while (true) {
        std::string event;
        std::string current_path;
        {
            std::unique_lock lock(logger_mutex_);
            logger_cv_.wait(lock, [this] { return !logger_queue_.empty() || !logger_running_; });

            if (logger_queue_.empty() && !logger_running_) break;

            if (!logger_queue_.empty()) {
                event = std::move(logger_queue_.front());
                logger_queue_.pop();
            }
            current_path = audit_log_path_;
        }

        if (event.empty() || current_path.empty()) continue;

        std::ofstream fs(current_path, std::ios::app);
        if (fs.is_open()) {
            auto now = std::chrono::system_clock::now().time_since_epoch().count();
            fs << "[" << now << "] " << event << std::endl;
        }
    }
}

u32 PolicyEngine::ChooseBetter(const PolicyView& p, u32 current_best_idx, u32 new_rule_idx, size_t matched_len, size_t path_len) {
    (void)matched_len;
    (void)path_len;
    if (current_best_idx == kInvalidIndex) return new_rule_idx;

    const auto& r1 = p.rules[current_best_idx];
    const auto& r2 = p.rules[new_rule_idx];

    bool r1_exact = (r1.kind == RULE_ALLOW_EXACT || r1.kind == RULE_REDIRECT_EXACT);
    bool r2_exact = (r2.kind == RULE_ALLOW_EXACT || r2.kind == RULE_REDIRECT_EXACT);

    if (r2_exact && !r1_exact) return new_rule_idx;
    if (r1_exact && !r2_exact) return current_best_idx;

    if (r2.src_len > r1.src_len) return new_rule_idx;
    if (r1.src_len > r2.src_len) return current_best_idx;

    if (r2.precedence > r1.precedence) return new_rule_idx;
    if (r1.precedence > r2.precedence) return current_best_idx;

    bool r2_allow = (r2.kind == RULE_ALLOW_PREFIX || r2.kind == RULE_ALLOW_EXACT);
    bool r1_allow = (r1.kind == RULE_ALLOW_PREFIX || r1.kind == RULE_ALLOW_EXACT);
    if (r2_allow && !r1_allow) return new_rule_idx;

    return current_best_idx;
}

const RadixEdge* PolicyEngine::FindMatchingEdge(const PolicyView& p, const RadixNode& node, u8 first_byte) {
    if (&node == &p.nodes[p.hdr->root_node_index] && (p.hdr->flags & POLICY_FLAG_HAS_BUCKETS)) {
        const RootBucket& b = p.buckets[first_byte];
        if (b.edge_count == 0) return nullptr;

        for (u32 i = 0; i < b.edge_count; ++i) {
            const RadixEdge& e = p.edges[b.first_edge + i];
            if (e.first_byte == first_byte) return &e;
        }
        return nullptr;
    }

    for (u32 i = 0; i < node.edge_count; ++i) {
        const RadixEdge& e = p.edges[node.first_edge + i];
        if (e.first_byte == first_byte) return &e;
    }
    return nullptr;
}

}  // namespace prism::io
