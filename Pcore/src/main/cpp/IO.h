#pragma once

#include <shared_mutex>
#include <string>
#include <string_view>
#include <vector>

namespace prism::io {

struct RedirectRule final {
    std::string from;
    std::string to;
};

class RedirectTable final {
public:
    static RedirectTable& Instance();

    void AddRule(std::string from, std::string to);
    void ClearRules();

    [[nodiscard]] std::string Redirect(std::string_view path) const;
    [[nodiscard]] bool IsBlockedResourcePath(std::string_view path) const;

    void AddBlockedPrefix(std::string prefix);
    void ClearBlockedPrefixes();

private:
    RedirectTable() = default;

    [[nodiscard]] const RedirectRule* FindBestRuleLocked(std::string_view path) const;
    static bool StartsWith(std::string_view value, std::string_view prefix);

    mutable std::shared_mutex mutex_;
    std::vector<RedirectRule> rules_;
    std::vector<std::string> blocked_prefixes_;
};

[[nodiscard]] std::string RedirectPath(std::string_view path);

// Optimized redirect that uses PolicyEngine and avoids std::string allocation if possible.
// Returns true if the path was redirected.
bool RedirectPath(std::string_view path, char* out_buf, size_t out_size, size_t* out_len = nullptr);

void AddRule(std::string from, std::string to);
void ClearRules();

}  // namespace prism::io
