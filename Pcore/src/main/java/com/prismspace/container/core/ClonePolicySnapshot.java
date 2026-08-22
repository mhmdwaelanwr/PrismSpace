package com.prismspace.container.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Data container for a complete PRISM policy state (UnixFS + Binder).
 */
public class ClonePolicySnapshot {
    public static class IoRule {
        public final String src;
        public final String dst;
        public final boolean exact;
        public final int precedence;

        public IoRule(String src, String dst, boolean exact, int precedence) {
            this.src = src;
            this.dst = dst;
            this.exact = exact;
            this.precedence = precedence;
        }
    }

    public static class BinderRule {
        public final int originalAppId;
        public final int spoofAppId;

        public BinderRule(int originalAppId, int spoofAppId) {
            this.originalAppId = originalAppId;
            this.spoofAppId = spoofAppId;
        }
    }

    public final long generation;
    public final List<IoRule> ioRules = new ArrayList<>();
    public final List<BinderRule> binderRules = new ArrayList<>();

    public ClonePolicySnapshot(long generation) {
        this.generation = generation;
    }
}
