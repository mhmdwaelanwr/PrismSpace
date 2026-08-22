# PrismSpace

PrismSpace is an experimental Android application virtualization and cloning environment built around a virtualized Android runtime, Binder/service interception, I/O redirection, per-clone policy controls, and a native policy engine.

> **Status:** active development. PrismSpace is a research/engineering project and should not be treated as a security boundary until its isolation model has been independently reviewed.

## Highlights

- Run and manage virtualized application instances inside a host app.
- Virtual package, activity, service, storage, notification, location, and related Android service layers.
- Binder and framework service interception.
- Native filesystem / Unix I/O hooks and redirection.
- Per-clone policy snapshots and runtime policy publishing.
- Native policy engine for fast path-based decisions.
- Virtual location and device-profile controls.
- ARM64 and ARMv7 native builds.
- Modern host UI built with Jetpack Compose.

## Project structure

```text
PrismSpace/
├── app/                # Host application and UI
├── Pcore/              # Virtualization/runtime engine
├── black-reflection/   # Reflection helpers
├── compiler/           # Annotation/code generation support
└── .github/workflows/  # CI
```

## Architecture

At a high level:

```text
Compose UI
   ↓
PrismEngineFacade
   ↓
Clone / Runtime controllers
   ↓
Virtual system services + Binder proxies
   ↓
IOCore / PolicyPublisher
   ↓
Native Prism policy engine + native hooks
```

Notable Prism-specific components include `PrismEngineFacade`, `AppCloneManager`, `CloneRuntimeController`, `ClonePolicyBuilder`, `PolicySnapshotBuilder`, `PolicyPublisher`, `ClonePolicyService`, and the native `PolicyEngine` / `PrismCore` layer.

## Build

Requirements:

- JDK 17
- Android SDK 35
- Android NDK `29.0.13846066`

Then run:

```bash
./gradlew :app:assembleDebug
```

The project currently compiles with SDK 35 while retaining a lower target SDK for virtualization compatibility. Treat target-SDK changes as runtime/compatibility work, not a cosmetic version bump.

## CI

GitHub Actions builds the debug APK and the `Pcore` debug AAR on pushes and pull requests targeting `main`.

## Upstream and credits

PrismSpace is derived from and inspired by work in the Android virtualization and hooking ecosystem, including **NewBlackbox / BlackBox**, **VirtualApp**, and related open-source projects.

The current codebase started from **ALEX5402/NewBlackbox** and has since introduced PrismSpace-specific runtime, policy, native-engine, and UI work. Upstream attribution is intentionally retained here and in source history where applicable.

Please review the licenses of upstream and third-party components before redistributing modified binaries or incorporating code from additional projects.

## License

This repository includes an Apache License 2.0 `LICENSE` file. Individual bundled or third-party components may have their own notices or license requirements; those remain applicable to their respective code.

## Responsible use

PrismSpace is intended for legitimate Android research, compatibility testing, application isolation experiments, and multi-instance use. Users are responsible for complying with applicable laws, platform rules, application terms, and software licenses.
