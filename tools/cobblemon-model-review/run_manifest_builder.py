#!/usr/bin/env python3
"""Re-run a skin builder from its professional manifest and prove byte reproducibility."""

from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import sys
from pathlib import Path


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def safe(root: Path, raw: str) -> Path:
    p = Path(raw)
    if p.is_absolute() or ".." in p.parts:
        raise SystemExit(f"unsafe repository path: {raw!r}")
    out = (root / p).resolve()
    if root.resolve() not in out.parents and out != root.resolve():
        raise SystemExit(f"path escapes repository root: {raw!r}")
    return out


def expected_output_hashes(data: dict) -> dict[str, str]:
    production = data["production"]
    result = {production["modelPath"]: production["modelSha256"]}
    for texture in production["textures"]:
        result[texture["path"]] = texture["sha256"]
    return result


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("manifest", type=Path)
    parser.add_argument("--repo-root", type=Path, default=Path("."))
    args = parser.parse_args()

    root = args.repo_root.resolve()
    manifest = args.manifest if args.manifest.is_absolute() else root / args.manifest
    data = json.loads(manifest.read_text(encoding="utf-8"))
    builder = data.get("builder")
    if not isinstance(builder, dict) or builder.get("deterministic") is not True:
        raise SystemExit("manifest builder is missing or not declared deterministic")
    command = builder.get("command")
    if not isinstance(command, list) or not command or not all(isinstance(x, str) and x for x in command):
        raise SystemExit("manifest.builder.command must be a non-empty argv list")
    if command[0] in {"python", "python3"}:
        command = [sys.executable, *command[1:]]
    elif command[0] != sys.executable:
        raise SystemExit("builder command must execute Python directly")

    outputs = builder.get("outputs")
    if not isinstance(outputs, list) or not outputs:
        raise SystemExit("manifest.builder.outputs must be non-empty")
    for raw in outputs:
        if not isinstance(raw, str):
            raise SystemExit("builder outputs must be repository-relative strings")
        safe(root, raw)

    print(json.dumps({"builderCommand": command, "declaredOutputs": outputs}, indent=2))
    subprocess.run(command, cwd=root, check=True)

    expected = expected_output_hashes(data)
    mismatches = []
    actual_report = {}
    for raw, expected_sha in expected.items():
        path = safe(root, raw)
        if not path.is_file():
            mismatches.append({"path": raw, "error": "missing after builder"})
            continue
        actual = sha256(path)
        actual_report[raw] = actual
        if actual != expected_sha:
            mismatches.append({"path": raw, "expected": expected_sha, "actual": actual})

    undeclared = sorted(set(expected) - set(outputs))
    if undeclared:
        mismatches.append({"error": "production outputs absent from builder.outputs", "paths": undeclared})

    if mismatches:
        raise SystemExit("BUILDER REPRODUCIBILITY FAIL: " + json.dumps(mismatches, indent=2))

    diff = subprocess.run(
        ["git", "diff", "--exit-code", "--", *outputs],
        cwd=root,
        text=True,
        capture_output=True,
    )
    if diff.returncode:
        print(diff.stdout)
        print(diff.stderr, file=sys.stderr)
        raise SystemExit(
            "BUILDER REPRODUCIBILITY FAIL: builder changed committed outputs even though manifest hashes were expected to match"
        )

    print(json.dumps({
        "status": "PASS",
        "species": data.get("species"),
        "concept": data.get("concept"),
        "outputs": actual_report,
        "gitDiffCleanForOutputs": True,
    }, indent=2))


if __name__ == "__main__":
    main()
