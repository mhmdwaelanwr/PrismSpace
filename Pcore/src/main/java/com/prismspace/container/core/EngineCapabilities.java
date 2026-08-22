package com.prismspace.container.core;

import android.os.Build;
import android.system.Os;
import android.system.OsConstants;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime source of truth for PrismSpace engine capabilities.
 *
 * A component being present in the source tree does not mean it is active on the current device.
 * Every subsystem should publish its observed runtime state here so diagnostics and policy decisions
 * can reason about facts instead of assumptions.
 */
public final class EngineCapabilities {

    public enum Component {
        CORE_SERVICES,
        ACTIVITY_MANAGER,
        PACKAGE_MANAGER,
        ACTIVITY_TASK_MANAGER,
        H_CALLBACK,
        HIDDEN_API,
        NATIVE_BOOTSTRAP,
        UNIX_FS,
        RUNTIME_LOAD,
        DEX_LOAD,
        VM_CLASS_LOADER,
        BINDER,
        POLICY_ENGINE,
        POLICY_TRANSPORT
    }

    public enum State {
        UNKNOWN,
        SUPPORTED,
        ACTIVE,
        DEGRADED,
        FAILED,
        DISABLED
    }

    public static final class Status {
        public final State state;
        public final String detail;
        public final long updatedAtMillis;

        Status(State state, String detail, long updatedAtMillis) {
            this.state = state;
            this.detail = detail == null ? "" : detail;
            this.updatedAtMillis = updatedAtMillis;
        }
    }

    private static final EngineCapabilities INSTANCE = new EngineCapabilities();

    private final Object lock = new Object();
    private final EnumMap<Component, Status> statuses = new EnumMap<>(Component.class);

    private EngineCapabilities() {
        long now = System.currentTimeMillis();
        for (Component component : Component.values()) {
            statuses.put(component, new Status(State.UNKNOWN, "not probed", now));
        }
    }

    public static EngineCapabilities get() {
        return INSTANCE;
    }

    public void mark(Component component, State state, String detail) {
        if (component == null || state == null) return;
        synchronized (lock) {
            statuses.put(component, new Status(state, detail, System.currentTimeMillis()));
        }
    }

    public Status getStatus(Component component) {
        synchronized (lock) {
            Status status = statuses.get(component);
            return status == null ? new Status(State.UNKNOWN, "not probed", 0L) : status;
        }
    }

    public Map<Component, Status> snapshot() {
        synchronized (lock) {
            return Collections.unmodifiableMap(new EnumMap<>(statuses));
        }
    }

    public Map<String, String> deviceFacts() {
        Map<String, String> facts = new LinkedHashMap<>();
        facts.put("sdk", String.valueOf(Build.VERSION.SDK_INT));
        facts.put("previewSdk", String.valueOf(Build.VERSION.PREVIEW_SDK_INT));
        facts.put("release", Build.VERSION.RELEASE == null ? "unknown" : Build.VERSION.RELEASE);
        facts.put("manufacturer", Build.MANUFACTURER == null ? "unknown" : Build.MANUFACTURER);
        facts.put("model", Build.MODEL == null ? "unknown" : Build.MODEL);
        facts.put("abi", Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0
                ? Build.SUPPORTED_ABIS[0] : "unknown");
        facts.put("pageSize", String.valueOf(readPageSize()));
        return Collections.unmodifiableMap(facts);
    }

    private long readPageSize() {
        try {
            return Os.sysconf(OsConstants._SC_PAGESIZE);
        } catch (Throwable ignored) {
            return -1L;
        }
    }

    public String toDiagnosticText() {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> fact : deviceFacts().entrySet()) {
            out.append(fact.getKey()).append('=').append(fact.getValue()).append('\n');
        }
        for (Map.Entry<Component, Status> entry : snapshot().entrySet()) {
            Status status = entry.getValue();
            out.append(entry.getKey().name())
                    .append('=')
                    .append(status.state.name());
            if (!status.detail.isEmpty()) {
                out.append(" (").append(status.detail).append(')');
            }
            out.append('\n');
        }
        return out.toString();
    }
}
