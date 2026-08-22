#pragma once

#include <cstdint>
#include <type_traits>

namespace prism::io {

// All offsets are relative to the base address of PolicyHeader.
using u8  = uint8_t;
using u16 = uint16_t;
using u32 = uint32_t;
using u64 = uint64_t;

static constexpr u32 kPolicyMagic   = 0x5052534Du; // 'PRSM'
static constexpr u16 kPolicyVersion = 1;
static constexpr u32 kInvalidIndex  = 0xFFFFFFFFu;

enum : u32 {
    POLICY_FLAG_HAS_RADIX        = 1u << 0,
    POLICY_FLAG_HAS_BUCKETS      = 1u << 1,
    POLICY_FLAG_LITTLE_ENDIAN    = 1u << 2,
    POLICY_FLAG_HAS_BINDER       = 1u << 3,
};

enum RuleKind : u16 {
    RULE_ALLOW_PREFIX   = 1,
    RULE_ALLOW_EXACT    = 2,
    RULE_REDIRECT_PREFIX= 3,
    RULE_REDIRECT_EXACT = 4,
};

enum BinderMode : u32 {
    BINDER_MODE_SHADOW = 0,  // Calculate but return original
    BINDER_MODE_ENFORCE = 1, // Return spoofed UID
};

struct alignas(8) PolicyHeader {
    u64 generation;        // 0: monotonically increasing publish id
    u32 magic;             // 8: kPolicyMagic
    u16 version;           // 12: kPolicyVersion
    u16 header_size;       // 14: sizeof(PolicyHeader)

    u32 total_size;        // 16: full mapped blob size
    u32 checksum32;        // 20: CRC32 validated only at install time
    u32 flags;             // 24: POLICY_FLAG_*

    u32 default_action;    // 28: DefaultAction
    u32 root_node_index;   // 32: usually 0

    u32 bucket_offset;     // 36: -> RootBucket[256]
    u32 bucket_count;      // 40: 256

    u32 node_offset;       // 44: -> RadixNode[]
    u32 node_count;        // 48:

    u32 edge_offset;       // 52: -> RadixEdge[]
    u32 edge_count;        // 56:

    u32 rule_offset;       // 60: -> PolicyRule[]
    u32 rule_count;        // 64:

    u32 string_offset;     // 68: -> char string pool
    u32 string_size;       // 72:

    u32 binder_policy_offset; // 76: -> BinderPolicyHeader
    u32 reserved[4];       // 80: Padding to 96 bytes
};

struct alignas(8) BinderPolicyHeader {
    u32 reserved_mode;     // 0: (Deprecated: Now controlled via global atomic)
    u32 entry_count;       // 4: Usually 10000 (app ID range)
    u32 entry_offset;      // 8: -> BinderUidMapEntry[]
    u32 reserved;          // 12:
};

struct BinderUidMapEntry {
    u16 spoof_app_id;      // Spoofed App ID (10000-19999) or 0 to passthrough
    u16 reserved;
};

struct alignas(8) RootBucket {
    u32 first_edge;        // index into RadixEdge[]
    u16 edge_count;
    u16 reserved;
};

struct alignas(8) RadixNode {
    u32 first_edge;        // index into RadixEdge[]
    u16 edge_count;
    u16 flags;

    u32 prefix_rule_index; // applies if boundary passes; else kInvalidIndex
    u32 exact_rule_index;  // applies only if full path consumed
};

struct alignas(8) RadixEdge {
    u32 label_off;         // 0: offset into string pool
    u16 label_len;         // 4: full label length in bytes
    u8  inline_size;       // 6: actual bytes in inline_prefix (min(8, label_len))
    u8  first_byte;        // 7: cached first byte for root dispatch

    u8  inline_prefix[8];  // 8: first 8 bytes of the label string

    u32 child_node_index;  // 16: index into RadixNode[]
    u32 reserved0;         // 20: 8-byte alignment padding
};

struct alignas(8) PolicyRule {
    u32 src_off;           // original source prefix/path
    u32 src_len;

    u32 dst_off;           // destination prefix/path; 0 if allow rule
    u32 dst_len;           // 0 if allow rule

    u16 kind;              // RuleKind
    u16 flags;             // RuleFlags
    u16 precedence;        // higher wins on equal specificity
    u16 reserved0;

    u32 reserved1;
};

enum LookupAction : u8 {
    LOOKUP_MISS = 0,
    LOOKUP_ALLOW_ORIGINAL,
    LOOKUP_REDIRECT,
};

struct LookupResult {
    LookupAction action;
    u32 rule_index;        // kInvalidIndex on miss
    u32 matched_len;       // bytes matched in original path
    u32 dst_off;           // into string pool if redirect
    u32 dst_len;           // redirect prefix len
};

// --- STATIC ASSERTIONS ---
static_assert(sizeof(PolicyHeader) == 96);
static_assert(sizeof(BinderPolicyHeader) == 16);
static_assert(sizeof(BinderUidMapEntry) == 4);
static_assert(sizeof(RadixEdge) == 24);

static_assert(offsetof(PolicyHeader, binder_policy_offset) == 76);

}  // namespace prism::io
