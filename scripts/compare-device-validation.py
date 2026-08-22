#!/usr/bin/env python3
"""Compare PrismSpace Engine Truth reports from real Android devices.

The first report is the baseline. Every later report is compared against it.
The tool intentionally compares observed runtime states, not claimed SDK support.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple

STATE_NAMES = {"UNKNOWN", "SUPPORTED", "ACTIVE", "DEGRADED", "FAILED", "DISABLED"}
STATE_SCORE = {
    "FAILED": 0,
    "UNKNOWN": 1,
    "DEGRADED": 2,
    "SUPPORTED": 3,
    "ACTIVE": 4,
}
STATE_RE = re.compile(r"^(?P<state>[A-Z_]+)(?: \((?P<detail>.*)\))?$")


@dataclass(frozen=True)
class ComponentStatus:
    state: str
    detail: str = ""


@dataclass
class Report:
    path: str
    facts: Dict[str, str]
    components: Dict[str, ComponentStatus]

    def label(self) -> str:
        sdk = self.facts.get("sdk", "?")
        manufacturer = self.facts.get("manufacturer", "?")
        model = self.facts.get("model", "?")
        abi = self.facts.get("abi", "?")
        page = format_page_size(self.facts.get("pageSize"))
        return f"{Path(self.path).name}: API {sdk}, {manufacturer} {model}, {abi}, {page}"


@dataclass(frozen=True)
class Change:
    component: str
    baseline: ComponentStatus
    candidate: ComponentStatus
    classification: str


def parse_report(path: Path) -> Report:
    text = path.read_text(encoding="utf-8")
    return parse_report_text(text, str(path))


def parse_report_text(text: str, source: str = "<memory>") -> Report:
    facts: Dict[str, str] = {}
    components: Dict[str, ComponentStatus] = {}

    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line or line.startswith("---") or line.startswith("PRISM_DEVICE_VALIDATION_"):
            continue
        if "=" not in line:
            continue

        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip()

        match = STATE_RE.match(value)
        if key.isupper() and match and match.group("state") in STATE_NAMES:
            components[key] = ComponentStatus(
                state=match.group("state"),
                detail=match.group("detail") or "",
            )
        else:
            facts[key] = value

    if not components:
        raise ValueError(f"{source}: no EngineCapabilities component states found")
    if "sdk" not in facts:
        raise ValueError(f"{source}: missing sdk fact")

    return Report(path=source, facts=facts, components=components)


def format_page_size(raw: Optional[str]) -> str:
    try:
        value = int(raw or "")
    except ValueError:
        return raw or "unknown"
    if value > 0 and value % 1024 == 0:
        return f"{value // 1024} KB"
    return f"{value} B" if value > 0 else "unknown"


def classify_change(baseline: ComponentStatus, candidate: ComponentStatus) -> str:
    if baseline == candidate:
        return "SAME"
    if baseline.state == candidate.state:
        return "DETAIL_CHANGE"

    if candidate.state == "FAILED":
        return "REGRESSION"
    if candidate.state == "UNKNOWN" and baseline.state not in {"UNKNOWN", "DISABLED"}:
        return "REGRESSION"
    if baseline.state == "UNKNOWN" and candidate.state not in {"UNKNOWN", "FAILED"}:
        return "EVIDENCE_GAINED"
    if baseline.state == "FAILED" and candidate.state != "FAILED":
        return "IMPROVEMENT"

    if "DISABLED" in {baseline.state, candidate.state}:
        return "CHANGE"

    old_score = STATE_SCORE.get(baseline.state)
    new_score = STATE_SCORE.get(candidate.state)
    if old_score is not None and new_score is not None:
        if new_score < old_score:
            return "REGRESSION"
        if new_score > old_score:
            return "IMPROVEMENT"
    return "CHANGE"


def compare_reports(baseline: Report, candidate: Report) -> List[Change]:
    names = sorted(set(baseline.components) | set(candidate.components))
    changes: List[Change] = []
    missing = ComponentStatus("UNKNOWN", "missing from report")
    for name in names:
        base = baseline.components.get(name, missing)
        cand = candidate.components.get(name, missing)
        changes.append(Change(name, base, cand, classify_change(base, cand)))
    return changes


def candidate_is_bad(changes: Iterable[Change]) -> bool:
    return any(change.classification == "REGRESSION" for change in changes)


def print_text_comparison(baseline: Report, candidate: Report, changes: List[Change]) -> None:
    print(f"Baseline : {baseline.label()}")
    print(f"Candidate: {candidate.label()}")

    fact_keys = ["sdk", "previewSdk", "release", "manufacturer", "model", "abi", "pageSize"]
    fact_changes: List[Tuple[str, str, str]] = []
    for key in fact_keys:
        old = baseline.facts.get(key, "-")
        new = candidate.facts.get(key, "-")
        if old != new:
            if key == "pageSize":
                old, new = format_page_size(old), format_page_size(new)
            fact_changes.append((key, old, new))

    if fact_changes:
        print("\nDevice/fact differences:")
        for key, old, new in fact_changes:
            print(f"  {key}: {old} -> {new}")

    interesting = [c for c in changes if c.classification != "SAME"]
    print("\nComponent differences:")
    if not interesting:
        print("  none")
    else:
        width = max(len(c.component) for c in interesting)
        for change in interesting:
            base_detail = f" ({change.baseline.detail})" if change.baseline.detail else ""
            cand_detail = f" ({change.candidate.detail})" if change.candidate.detail else ""
            print(
                f"  {change.component:<{width}}  "
                f"{change.baseline.state}{base_detail} -> "
                f"{change.candidate.state}{cand_detail}  "
                f"[{change.classification}]"
            )

    regressions = sum(c.classification == "REGRESSION" for c in changes)
    improvements = sum(c.classification in {"IMPROVEMENT", "EVIDENCE_GAINED"} for c in changes)
    detail_changes = sum(c.classification == "DETAIL_CHANGE" for c in changes)
    failed = sum(c.candidate.state == "FAILED" for c in changes)
    unknown = sum(c.candidate.state == "UNKNOWN" for c in changes)
    print(
        "\nSummary: "
        f"regressions={regressions}, improvements={improvements}, "
        f"detail_changes={detail_changes}, failed={failed}, unknown={unknown}"
    )


def build_json_result(baseline: Report, candidate: Report, changes: List[Change]) -> dict:
    return {
        "baseline": {
            "path": baseline.path,
            "facts": baseline.facts,
        },
        "candidate": {
            "path": candidate.path,
            "facts": candidate.facts,
        },
        "changes": [
            {
                "component": c.component,
                "baseline": {"state": c.baseline.state, "detail": c.baseline.detail},
                "candidate": {"state": c.candidate.state, "detail": c.candidate.detail},
                "classification": c.classification,
            }
            for c in changes
        ],
        "hasRegression": candidate_is_bad(changes),
    }


def self_test() -> None:
    baseline_text = """\
validationSchema=1
sdk=35
manufacturer=Example
model=Baseline
abi=arm64-v8a
pageSize=4096
CORE_SERVICES=ACTIVE (ready)
NATIVE_BOOTSTRAP=DEGRADED (mask=0x21)
BINDER=ACTIVE (intercept active)
DEX_LOAD=UNKNOWN (not probed)
"""
    candidate_text = """\
validationSchema=1
sdk=36
manufacturer=Example
model=Candidate
abi=arm64-v8a
pageSize=16384
CORE_SERVICES=DEGRADED (slow startup)
NATIVE_BOOTSTRAP=FAILED (bootstrap error)
BINDER=ACTIVE (intercept active)
DEX_LOAD=ACTIVE (intercept active)
"""
    baseline = parse_report_text(baseline_text, "baseline")
    candidate = parse_report_text(candidate_text, "candidate")
    changes = {c.component: c for c in compare_reports(baseline, candidate)}

    assert changes["CORE_SERVICES"].classification == "REGRESSION"
    assert changes["NATIVE_BOOTSTRAP"].classification == "REGRESSION"
    assert changes["BINDER"].classification == "SAME"
    assert changes["DEX_LOAD"].classification == "EVIDENCE_GAINED"
    assert candidate_is_bad(changes.values())
    assert format_page_size("16384") == "16 KB"

    with tempfile.TemporaryDirectory() as tmp:
        path = Path(tmp) / "report.txt"
        path.write_text(candidate_text, encoding="utf-8")
        parsed = parse_report(path)
        assert parsed.facts["sdk"] == "36"
        assert parsed.components["NATIVE_BOOTSTRAP"].state == "FAILED"

    print("device validation comparator self-test: PASS")


def parse_args(argv: Optional[List[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Compare PrismSpace on-device Engine Truth reports."
    )
    parser.add_argument("reports", nargs="*", type=Path, help="baseline followed by one or more candidate reports")
    parser.add_argument("--json", action="store_true", help="emit JSON instead of human-readable text")
    parser.add_argument(
        "--allow-regressions",
        action="store_true",
        help="return exit code 0 even when regressions are detected",
    )
    parser.add_argument("--self-test", action="store_true", help="run built-in parser/classifier tests")
    return parser.parse_args(argv)


def main(argv: Optional[List[str]] = None) -> int:
    args = parse_args(argv)
    if args.self_test:
        self_test()
        return 0

    if len(args.reports) < 2:
        print("error: provide a baseline report and at least one candidate report", file=sys.stderr)
        return 64

    try:
        baseline = parse_report(args.reports[0])
        candidates = [parse_report(path) for path in args.reports[1:]]
    except (OSError, ValueError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 65

    any_regression = False
    json_results = []
    for index, candidate in enumerate(candidates):
        changes = compare_reports(baseline, candidate)
        any_regression = any_regression or candidate_is_bad(changes)
        if args.json:
            json_results.append(build_json_result(baseline, candidate, changes))
        else:
            if index:
                print("\n" + "=" * 88 + "\n")
            print_text_comparison(baseline, candidate, changes)

    if args.json:
        print(json.dumps(json_results, indent=2, sort_keys=True))

    if any_regression and not args.allow_regressions:
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
