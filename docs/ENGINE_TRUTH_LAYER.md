# PrismSpace Engine Truth Layer

The Engine Truth Layer is PrismSpace's capability-driven compatibility contract.

A component is not considered healthy merely because its code exists or an injector was registered. Runtime state must be observed and reported explicitly.

## Capability states

- `UNKNOWN` — no runtime evidence yet.
- `SUPPORTED` — platform/backend appears compatible, but activation is not proven.
- `ACTIVE` — activation succeeded and the runtime health check is satisfied.
- `DEGRADED` — a documented fallback/observe-only mode is active.
- `FAILED` — activation was attempted and failed.
- `DISABLED` — intentionally disabled by policy/build configuration.

## Native staged bring-up

The default diagnostic dependency stage remains `A0`.

CI validates:

- `A0`: Prism native runtime without xDL/Dobby dependency linking.
- `A1`: xDL is compiled and forced into the final link through real symbol references.
- `A2`: Dobby is also forced into the final link through a real side-effect-free symbol reference.

Each ARM64 probe uses the exact configured NDK (`29.0.13846066`) and verifies that all ELF `LOAD` segments are aligned to `0x4000` for 16 KB page-size readiness.

Passing A1/A2 is a build/link readiness result. It does not automatically enable those dependencies in the default runtime path.

## Current critical surfaces

Java/framework truth reporting covers the critical Activity Manager, Package Manager, Activity Task Manager, and ActivityThread handler callback surfaces.

Native truth reporting covers Unix filesystem, runtime native-library loading, DEX loading, VM class loading, Binder, and native bootstrap state. Observe-only Runtime/DEX modes are reported as degraded rather than active.

## Next validation layer

The next step is on-device diagnostics. A device report should capture at minimum:

- Android release / SDK / preview SDK
- ABI
- runtime page size
- critical Java hook states
- native subsystem states
- policy-engine generation / transport state
- initialization failures with reason strings

This report is intended for compatibility engineering and debugging, not for defeating application security controls.
