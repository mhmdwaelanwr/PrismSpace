package com.prismspace.container.core;

import android.os.Build;
import android.system.Os;
import android.system.OsConstants;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.CRC32;

/**
 * Builds an immutable binary policy blob for the PRISM native engine.
 * Implements a path-compressed radix tree serialization with CRC32, 16KB Page Alignment,
 * RadixEdgeV2 (Inline Prefix Optimization), and H5 Binder Identity Spoofing.
 */
public class PolicySnapshotBuilder {
    public static class RedirectionRule {
        public final String srcPath;
        public final String dstPath;
        public final int kind; // 1: ALLOW_PREFIX, 2: ALLOW_EXACT, 3: REDIRECT_PREFIX, 4: REDIRECT_EXACT
        public final int precedence;

        public RedirectionRule(String srcPath, String dstPath, int kind, int precedence) {
            this.srcPath = srcPath;
            this.dstPath = dstPath;
            this.kind = kind;
            this.precedence = precedence;
        }
    }

    private static final int K_POLICY_MAGIC = 0x5052534D; // 'PRSM'
    private static final short K_POLICY_VERSION = 1;
    private static final int K_INVALID_INDEX = 0xFFFFFFFF;

    private static final int FLAG_HAS_RADIX = 1 << 0;
    private static final int FLAG_HAS_BUCKETS = 1 << 1;
    private static final int FLAG_LITTLE_ENDIAN = 1 << 2;
    private static final int FLAG_HAS_BINDER = 1 << 3;

    private final List<RedirectionRule> rules = new ArrayList<>();
    
    // H5: Binder Uid Map (AppId range 10000-19999 -> Entry)
    private final short[] binderUidMap = new short[10000];
    private int binderMode = 0; // 0: SHADOW, 1: ENFORCE

    private static long sPageSize = -1;

    private static long getPageSize() {
        if (sPageSize > 0) return sPageSize;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                sPageSize = Os.sysconf(OsConstants._SC_PAGESIZE);
            } catch (Exception ignored) {}
        }
        if (sPageSize <= 0) sPageSize = 4096; // Fallback
        return sPageSize;
    }

    public void addRule(String src, String dst, boolean exact, int precedence) {
        int kind;
        if (dst == null || dst.isEmpty()) {
            kind = exact ? 2 : 1;
        } else {
            kind = exact ? 4 : 3;
        }
        rules.add(new RedirectionRule(src, dst, kind, precedence));
    }

    /**
     * Adds a Binder UID spoofing rule.
     * Only App IDs (10000-19999) are accepted to ensure O(1) lookup safety.
     */
    public void addBinderSpoof(int originalAppId, int spoofAppId) {
        if (originalAppId >= 10000 && originalAppId < 20000) {
            if (spoofAppId >= 10000 && spoofAppId < 20000) {
                binderUidMap[originalAppId - 10000] = (short) spoofAppId;
            }
        }
    }

    public void setBinderMode(boolean enforce) {
        this.binderMode = enforce ? 1 : 0;
    }

    private static class Node {
        int index;
        int prefixRuleIndex = K_INVALID_INDEX;
        int exactRuleIndex = K_INVALID_INDEX;
        final TreeMap<Integer, Edge> edges = new TreeMap<>();
    }

    private static class Edge {
        byte[] label;
        Node child;
    }

    public ByteBuffer build(long generation) {
        Node root = buildRadixTree();
        
        List<Node> allNodes = new ArrayList<>();
        collectNodes(root, allNodes);

        List<Edge> allEdges = new ArrayList<>();
        for (Node n : allNodes) {
            allEdges.addAll(n.edges.values());
        }

        Map<String, Integer> stringPool = new HashMap<>();
        ByteArrayOutputStream stringsOut = new ByteArrayOutputStream();
        
        // Layout: Header(96) -> Buckets(256*8) -> Nodes(N*16) -> Edges(E*24) -> Rules(R*32) -> BinderHeader(16) -> BinderMap(10000*4) -> Strings
        int offsetHeader = 0;
        int offsetBuckets = 96;
        int offsetNodes = offsetBuckets + 256 * 8;
        int offsetEdges = offsetNodes + allNodes.size() * 16;
        int offsetRules = offsetEdges + allEdges.size() * 24;
        int offsetBinderHeader = offsetRules + rules.size() * 32;
        int offsetBinderMap = offsetBinderHeader + 16;
        int offsetStringsStart = offsetBinderMap + 10000 * 4;
        
        stringPool.put("", 0);
        stringsOut.write(0);
        
        for (RedirectionRule r : rules) {
            getStringOffset(r.srcPath, stringPool, stringsOut);
            if (r.dstPath != null) getStringOffset(r.dstPath, stringPool, stringsOut);
        }
        for (Edge e : allEdges) {
            getStringOffsetFromBytes(e.label, stringPool, stringsOut);
        }

        byte[] stringData = stringsOut.toByteArray();
        int offsetStrings = (offsetStringsStart + 7) & ~7;
        int dataSize = offsetStrings + stringData.length;
        
        long pageSize = getPageSize();
        int totalSize = (int) ((dataSize + (pageSize - 1)) & ~(pageSize - 1));

        ByteBuffer buf = ByteBuffer.allocateDirect(totalSize);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        // 1. Write Header
        buf.position(offsetHeader);
        buf.putLong(generation);         // 0
        buf.putInt(K_POLICY_MAGIC);      // 8
        buf.putShort(K_POLICY_VERSION);  // 12
        buf.putShort((short) 96);        // 14
        buf.putInt(totalSize);           // 16
        buf.putInt(0);                   // 20: CRC32 Placeholder
        buf.putInt(FLAG_HAS_RADIX | FLAG_HAS_BUCKETS | FLAG_LITTLE_ENDIAN | FLAG_HAS_BINDER); // 24
        buf.putInt(0);                   // 28: default_action
        buf.putInt(root.index);          // 32
        buf.putInt(offsetBuckets);       // 36
        buf.putInt(256);                 // 40: bucket_count
        buf.putInt(offsetNodes);         // 44
        buf.putInt(allNodes.size());     // 48
        buf.putInt(offsetEdges);         // 52
        buf.putInt(allEdges.size());     // 56
        buf.putInt(offsetRules);         // 60
        buf.putInt(rules.size());        // 64
        buf.putInt(offsetStrings);       // 68
        buf.putInt(stringData.length);   // 72
        buf.putInt(offsetBinderHeader);  // 76: binder_policy_offset
        for (int i = 0; i < 4; i++) buf.putInt(0); // 80-95 reserved

        // 2. Write Radix Nodes and Edges
        int currentEdgeGlobalIdx = 0;
        for (Node n : allNodes) {
            buf.position(offsetNodes + n.index * 16);
            buf.putInt(currentEdgeGlobalIdx);
            buf.putShort((short) n.edges.size());
            buf.putShort((short) 0);
            buf.putInt(n.prefixRuleIndex);
            buf.putInt(n.exactRuleIndex);

            int startEdgeIdx = currentEdgeGlobalIdx;
            for (Edge e : n.edges.values()) {
                buf.position(offsetEdges + currentEdgeGlobalIdx * 24);
                buf.putInt(getStringOffsetFromBytes(e.label, stringPool, stringsOut)); 
                buf.putShort((short) e.label.length);                                  
                int inlineSize = Math.min(8, e.label.length);
                buf.put((byte) inlineSize);                                            
                buf.put((byte) (e.label[0] & 0xFF));                                   
                for (int i = 0; i < 8; i++) {
                    if (i < inlineSize) buf.put(e.label[i]);
                    else buf.put((byte) 0);
                }
                buf.putInt(e.child.index);                                             
                buf.putInt(0);                                                         
                currentEdgeGlobalIdx++;
            }

            if (n == root) {
                for (int b = 0; b < 256; b++) {
                    buf.position(offsetBuckets + b * 8);
                    Edge rootEdge = root.edges.get(b);
                    if (rootEdge != null) {
                        int idx = 0;
                        for (Edge re : root.edges.values()) {
                            if (re == rootEdge) {
                                buf.putInt(startEdgeIdx + idx);
                                buf.putShort((short) 1);
                                break;
                            }
                            idx++;
                        }
                    } else {
                        buf.putInt(K_INVALID_INDEX);
                        buf.putShort((short) 0);
                    }
                }
            }
        }

        // 3. Write Rules
        buf.position(offsetRules);
        for (RedirectionRule r : rules) {
            buf.putInt(stringPool.get(r.srcPath));
            buf.putInt(r.srcPath.length());
            if (r.dstPath != null) {
                buf.putInt(stringPool.get(r.dstPath));
                buf.putInt(r.dstPath.length());
            } else {
                buf.putInt(0);
                buf.putInt(0);
            }
            buf.putShort((short) r.kind);
            buf.putShort((short) 0);
            buf.putShort((short) r.precedence);
            buf.putShort((short) 0);
            buf.putInt(0);
        }

        // 4. Write Binder Policy (H5)
        buf.position(offsetBinderHeader);
        buf.putInt(binderMode);
        buf.putInt(10000); // entry_count
        buf.putInt(offsetBinderMap);
        buf.putInt(0); // reserved

        buf.position(offsetBinderMap);
        for (int i = 0; i < 10000; i++) {
            buf.putShort(binderUidMap[i]); // spoof_app_id
            buf.putShort((short) 0); // reserved
        }

        // 5. Write Strings
        buf.position(offsetStrings);
        buf.put(stringData);

        // 6. Calculate and fill CRC32
        long crc = calculateCRC32(buf, totalSize);
        buf.position(20);
        buf.putInt((int) crc);

        buf.clear();
        return buf;
    }

    private long calculateCRC32(ByteBuffer buf, int totalSize) {
        CRC32 crc = new CRC32();
        byte[] data = new byte[totalSize];
        int pos = buf.position();
        buf.position(0);
        buf.get(data);
        buf.position(pos);
        data[20] = 0; data[21] = 0; data[22] = 0; data[23] = 0;
        crc.update(data);
        return crc.getValue();
    }

    private Node buildRadixTree() {
        Node root = new Node();
        for (int i = 0; i < rules.size(); i++) {
            insertRule(root, i);
        }
        return root;
    }

    private void insertRule(Node root, int ruleIdx) {
        RedirectionRule r = rules.get(ruleIdx);
        byte[] path = r.srcPath.getBytes();
        Node curr = root;
        int i = 0;
        while (i < path.length) {
            int b = path[i] & 0xFF;
            Edge e = curr.edges.get(b);
            if (e == null) {
                Edge newEdge = new Edge();
                byte[] label = new byte[path.length - i];
                System.arraycopy(path, i, label, 0, label.length);
                newEdge.label = label;
                newEdge.child = new Node();
                curr.edges.put(b, newEdge);
                curr = newEdge.child;
                break;
            } else {
                int commonLen = 0;
                while (commonLen < e.label.length && i + commonLen < path.length && e.label[commonLen] == path[i + commonLen]) {
                    commonLen++;
                }
                if (commonLen < e.label.length) {
                    Node splitNode = new Node();
                    Edge splitEdge = new Edge();
                    splitEdge.label = new byte[e.label.length - commonLen];
                    System.arraycopy(e.label, commonLen, splitEdge.label, 0, splitEdge.label.length);
                    splitEdge.child = e.child;
                    
                    splitNode.edges.put(splitEdge.label[0] & 0xFF, splitEdge);

                    e.label = new byte[commonLen];
                    System.arraycopy(path, i, e.label, 0, commonLen);
                    e.child = splitNode;
                }
                i += commonLen;
                curr = e.child;
            }
        }
        if (r.kind == 2 || r.kind == 4) curr.exactRuleIndex = ruleIdx;
        else curr.prefixRuleIndex = ruleIdx;
    }

    private void collectNodes(Node n, List<Node> list) {
        n.index = list.size();
        list.add(n);
        for (Edge e : n.edges.values()) {
            collectNodes(e.child, list);
        }
    }

    private int getStringOffset(String s, Map<String, Integer> pool, ByteArrayOutputStream out) {
        if (s == null) return 0;
        Integer off = pool.get(s);
        if (off != null) return off;
        int current = out.size();
        try {
            byte[] b = s.getBytes("UTF-8");
            out.write(b);
            out.write(0);
            pool.put(s, current);
            return current;
        } catch (IOException e) {
            return 0;
        }
    }

    private int getStringOffsetFromBytes(byte[] b, Map<String, Integer> pool, ByteArrayOutputStream out) {
        String s = new String(b);
        Integer off = pool.get(s);
        if (off != null) return off;
        int current = out.size();
        out.write(b, 0, b.length);
        out.write(0);
        pool.put(s, current);
        return current;
    }
}
