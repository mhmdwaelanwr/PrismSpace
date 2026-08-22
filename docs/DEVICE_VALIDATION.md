# PrismSpace Device Engine Validation

This phase validates runtime truth on real Android devices. Build/link probes for xDL and Dobby are necessary, but they are not runtime evidence.

## What the harness validates

`EngineValidationTest` initializes PrismSpace, refreshes the Engine Truth Layer, waits for the asynchronous native bootstrap to publish a result, writes a diagnostic report, and enforces two hard observability contracts:

- runtime page size must be readable and greater than zero;
- `CORE_SERVICES` must produce `ACTIVE` or `DEGRADED` evidence;
- `NATIVE_BOOTSTRAP` must produce `ACTIVE` or `DEGRADED` evidence within the validation window.

Java hook states and individual native hook states are recorded in the report. They are not promoted to success merely because the corresponding source code or library exists.

## Run on a connected device

```bash
./scripts/run-device-validation.sh
```

To select one device when multiple devices are attached:

```bash
ANDROID_SERIAL=<serial> ./scripts/run-device-validation.sh
```

The script runs only `EngineValidationTest`, then uses `run-as` on the debuggable app to extract:

```text
files/diagnostics/latest.txt
```

Reports are copied to:

```text
artifacts/device-validation/
```

## Required device matrix

Validate at least one physical ARM64 device for each available platform generation:

| Platform | API | Required evidence |
| --- | ---: | --- |
| Android 14 | 34 | report + test result |
| Android 15 | 35 | report + test result |
| Android 16 | 36 | report + test result |
| Android 17 | 37 | report + test result when a suitable device/build is available |

Where possible, include both 4 KB and 16 KB page-size devices. The report records the runtime page size independently of the CI ELF alignment probe.

## Interpreting states

- `ACTIVE`: runtime health was observed.
- `DEGRADED`: a known fallback or partial/observe-only mode was observed.
- `FAILED`: runtime evidence says the subsystem failed.
- `UNKNOWN`: no evidence yet; this is never treated as success.
- `DISABLED`: intentionally disabled.

A successful A1/A2 CI probe means xDL/Dobby can build and link in the staged native configuration. It does **not** mean those stages are enabled or healthy in the default runtime build.

## Compare reports

Use the first report as the baseline and pass one or more later reports as candidates:

```bash
python3 scripts/compare-device-validation.py \
  artifacts/device-validation/android15.txt \
  artifacts/device-validation/android16.txt \
  artifacts/device-validation/android17.txt
```

Machine-readable output is available with:

```bash
python3 scripts/compare-device-validation.py --json baseline.txt candidate.txt
```

By default the comparator exits with status `2` when it finds a runtime regression. Use `--allow-regressions` only for exploratory comparison where a non-zero exit code is undesirable.

The comparator classifies changes such as:

- `ACTIVE -> DEGRADED/FAILED/UNKNOWN` as a regression;
- `DEGRADED -> FAILED/UNKNOWN` as a regression;
- `FAILED -> non-FAILED` as an improvement;
- `UNKNOWN -> ACTIVE/DEGRADED/SUPPORTED` as evidence gained;
- same state with changed detail as `DETAIL_CHANGE`.

It also prints API, OEM/model, ABI, and runtime page-size differences. Keep each raw report unchanged so compatibility work can be tied to observed evidence instead of an SDK-version assumption.
