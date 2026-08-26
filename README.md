# PrismSpace

<p align="center">
  <strong>Experimental Android application virtualization and multi-instance runtime.</strong><br>
  Binder interception · I/O redirection · per-clone policy controls · native runtime hooks
</p>

<p align="center">
  <img alt="Android" src="https://img.shields.io/badge/Android-Runtime-3DDC84?style=flat-square&logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Compose-7F52FF?style=flat-square&logo=kotlin&logoColor=white">
  <img alt="Native" src="https://img.shields.io/badge/Native-ARM64%20%7C%20ARMv7-555?style=flat-square">
  <img alt="Status" src="https://img.shields.io/badge/status-active%20research-orange?style=flat-square">
</p>

PrismSpace explores how an Android host application can run and manage virtualized application instances while presenting each clone with its own runtime-facing package, service, storage, location, and policy state.

The project combines Java/Kotlin framework interception with native I/O policy enforcement and focuses on **measurable runtime capability** rather than assuming a subsystem works because its hook code exists.

> **Status:** active research and engineering. PrismSpace should not be treated as a security boundary until its isolation model has been independently reviewed and validated.

## Core capabilities

- Virtualized application instances managed inside a host application.
- Package, activity, service, storage, notification, location, and related virtual system layers.
- Binder and Android framework service interception.
- Filesystem and Unix I/O redirection through native hooks.
- Per-clone policy snapshots and runtime policy publishing.
- Native policy engine for path-based decisions on performance-sensitive paths.
- Virtual location and device-profile controls.
- ARM64 and ARMv7 native builds.
- Jetpack Compose host interface.

## Architecture

```text
Jetpack Compose UI
        ↓
PrismEngineFacade
        ↓
Clone / Runtime controllers
        ↓
Virtual system services + Binder proxies
        ↓
IOCore / PolicyPublisher
        ↓
Native policy engine + native hooks
```

Important Prism-specific components include:

- `PrismEngineFacade`
- `AppCloneManager`
- `CloneRuntimeController`
- `ClonePolicyBuilder`
- `PolicySnapshotBuilder`
- `PolicyPublisher`
- `ClonePolicyService`
- native `PolicyEngine` / `PrismCore`

## Runtime capability model

PrismSpace is moving toward capability-driven compatibility. Critical Java and native subsystems can report runtime capability state alongside device facts such as API level, ABI, and runtime page size.

That distinction matters for virtualization work: successfully compiling or loading a hook is not the same as proving that it behaves correctly on a real runtime.

## Native bring-up strategy

Native dependency bring-up remains conservative by default.

CI probes three stages independently:

- `A0` — baseline native path.
- `A1` — xDL bring-up probe.
- `A2` — Dobby bring-up probe.

The probe jobs force real symbols from staged static dependencies into the final link so an unused archive cannot create a false-positive integration result. Passing these probes demonstrates build/link readiness; it does **not** automatically enable xDL or Dobby in the default runtime path.

For ARM64:

```bash
bash scripts/native-bringup-probe.sh A0 B9 arm64-v8a
bash scripts/native-bringup-probe.sh A1 B9 arm64-v8a
bash scripts/native-bringup-probe.sh A2 B9 arm64-v8a
```

Each stage uses an isolated output directory and verifies the generated `libprismspace.so` LOAD-segment alignment for 16 KB page-size readiness.

## Repository structure

```text
PrismSpace/
├── app/                # Host application and Compose UI
├── Pcore/              # Virtualization/runtime engine
├── black-reflection/   # Reflection helpers
├── compiler/           # Annotation/code generation support
├── scripts/            # Native and engine diagnostic probes
└── .github/workflows/  # CI pipelines
```

## Build

Requirements:

- JDK 17
- Android SDK 35
- Android NDK `29.0.13846066`

Build the debug application with:

```bash
./gradlew :app:assembleDebug
```

The project currently compiles against SDK 35 while retaining a lower target SDK for virtualization compatibility. Target-SDK changes should therefore be treated as runtime compatibility work rather than a cosmetic version update.

## CI

GitHub Actions currently exercises:

- engine compatibility unit tests,
- staged ARM64 native bring-up probes,
- debug APK builds,
- `Pcore` debug AAR builds.

These checks run on pushes and pull requests targeting `main`.

## Upstream and attribution

PrismSpace is derived from and inspired by work across the Android virtualization and hooking ecosystem, including **NewBlackbox / BlackBox**, **VirtualApp**, and related open-source projects.

The codebase started from **ALEX5402/NewBlackbox** and has since gained PrismSpace-specific runtime, policy, native-engine, capability-reporting, and UI work. Upstream attribution is intentionally retained in this README and in source history where applicable.

Review the licenses and notices of upstream and third-party components before redistributing modified binaries or incorporating additional code.

## Responsible use

PrismSpace is intended for legitimate Android research, compatibility testing, application-isolation experiments, and multi-instance engineering. Users are responsible for complying with applicable laws, platform policies, application terms, and software licenses.

## License

The repository includes an Apache License 2.0 `LICENSE` file. Individual bundled or third-party components may carry additional license or notice requirements, which remain applicable to their respective code.
