#include "IO.h"

#include <algorithm>
#include <mutex>

#include "PolicyEngine.h"

namespace prism::io {

RedirectTable& RedirectTable::Instance() {
    static RedirectTable table;
    return table;
}

bool RedirectTable::StartsWith(std::string_view value, std::string_view prefix) {
    return value.size() >= prefix.size() && value.substr(0, prefix.size()) == prefix;
}

const RedirectRule* RedirectTable::FindBestRuleLocked(std::string_view path) const {
    const RedirectRule* best = nullptr;
    for (const auto& rule : rules_) {
        if (!StartsWith(path, rule.from)) {
            continue;
        }
        if (best == nullptr || rule.from.size() > best->from.size()) {
            best = &rule;
        }
    }
    return best;
}

void RedirectTable::AddRule(std::string from, std::string to) {
    if (from.empty() || to.empty() || from == to) {
        return;
    }

    std::unique_lock lock(mutex_);
    for (auto& rule : rules_) {
        if (rule.from == from) {
            rule.to = std::move(to);
            return;
        }
    }
    rules_.push_back(RedirectRule{std::move(from), std::move(to)});

    std::sort(rules_.begin(), rules_.end(), [](const RedirectRule& lhs, const RedirectRule& rhs) {
        return lhs.from.size() > rhs.from.size();
    });
}

void RedirectTable::ClearRules() {
    std::unique_lock lock(mutex_);
    rules_.clear();
}

std::string RedirectTable::Redirect(std::string_view path) const {
    // Try the new PolicyEngine first.
    LookupResult result = PolicyEngine::Instance().Lookup(path);
    if (result.action == LOOKUP_REDIRECT) {
        // We need the string pool to get the dst string.
        // Actually, LookupResult should probably return the dst string directly or we need a way to get it.
        // Let's refine PolicyEngine to provide the full redirected path if possible,
        // or let the caller do it.

        // For now, I'll keep the legacy RedirectTable as a fallback or for rules added via AddRule.
        // In the final version, AddRule might be removed or integrated.
    }

    std::shared_lock lock(mutex_);
    const RedirectRule* rule = FindBestRuleLocked(path);
    if (rule == nullptr) {
        return std::string(path);
    }

    std::string out;
    out.reserve(rule->to.size() + path.size() - rule->from.size());
    out.append(rule->to);
    out.append(path.substr(rule->from.size()));
    return out;
}

// Optimized redirect that uses PolicyEngine and avoids std::string allocation if possible
bool RedirectPath(std::string_view path, char* out_buf, size_t out_size, size_t* out_len) {
    LookupResult result = PolicyEngine::Instance().Lookup(path);
    if (result.action == LOOKUP_MISS) {
        // Fallback to legacy RedirectTable for now
        std::string redirected = RedirectTable::Instance().Redirect(path);
        if (redirected.size() >= out_size) return false;
        std::memcpy(out_buf, redirected.data(), redirected.size());
        out_buf[redirected.size()] = '\0';
        if (out_len) *out_len = redirected.size();
        return redirected != path;
    }

    if (result.action == LOOKUP_ALLOW_ORIGINAL) {
        if (path.size() >= out_size) return false;
        std::memcpy(out_buf, path.data(), path.size());
        out_buf[path.size()] = '\0';
        if (out_len) *out_len = path.size();
        return false;
    }

    if (result.action == LOOKUP_REDIRECT) {
        // This is tricky because we need the string pool from the active policy.
        // I should probably add a method to PolicyEngine to do the assembly.
        return PolicyEngine::Instance().AssembleRedirect(result, path, out_buf, out_size, out_len);
    }

    return false;
}

bool RedirectTable::IsBlockedResourcePath(std::string_view path) const {
    std::shared_lock lock(mutex_);
    for (const auto& prefix : blocked_prefixes_) {
        if (StartsWith(path, prefix)) {
            return true;
        }
    }
    return false;
}

void RedirectTable::AddBlockedPrefix(std::string prefix) {
    if (prefix.empty()) {
        return;
    }
    std::unique_lock lock(mutex_);
    if (std::find(blocked_prefixes_.begin(), blocked_prefixes_.end(), prefix) == blocked_prefixes_.end()) {
        blocked_prefixes_.push_back(std::move(prefix));
    }
}

void RedirectTable::ClearBlockedPrefixes() {
    std::unique_lock lock(mutex_);
    blocked_prefixes_.clear();
}

std::string RedirectPath(std::string_view path) {
    char buf[4096];
    size_t len = 0;
    if (RedirectPath(path, buf, sizeof(buf), &len)) {
        return std::string(buf, len);
    }
    return std::string(path);
}

void AddRule(std::string from, std::string to) {
    RedirectTable::Instance().AddRule(std::move(from), std::move(to));
}

void ClearRules() {
    RedirectTable::Instance().ClearRules();
}

}  // namespace prism::io
