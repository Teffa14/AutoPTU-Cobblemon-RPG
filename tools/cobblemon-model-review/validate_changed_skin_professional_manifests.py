#!/usr/bin/env python3
"""Require and validate a professional review manifest for every production skin change."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

PRODUCTION_PATTERNS = (
    re.compile(r"^fabric-adapter/src/main/resources/assets/(?:autoptu|cobblemon)/bedrock/pokemon/models/(?P<slug>\d{4}_[^/]+)/"),
    re.compile(r"^fabric-adapter/src/main/resources/assets/autoptu/bedrock/pokemon/textures/(?P<slug>\d{4}_[^/]+)/"),
    re.compile(r"^fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/(?P<slug>\d{4}_[^/]+)/"),
    re.compile(r"^src/main/resources/data/autoptu/cobblemon/skins/(?P<slug>\d{4}_[^/]+)/"),
)
MANIFEST_PATTERN = re.compile(
    r"^docs/cobblemon-skin-review-manifests/(?P<slug>\d{4}_[^/]+)\.json$"
)


def changed_files(base: str, head: str) -> list[str]:
    result = subprocess.run(
        ["git", "diff", "--name-only", f"{base}...{head}"],
        check=True,
        capture_output=True,
        text=True,
    )
    return [line.strip() for line in result.stdout.splitlines() if line.strip()]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True)
    parser.add_argument("--head", required=True)
    args = parser.parse_args()

    files = changed_files(args.base, args.head)
    changed = set(files)
    production_slugs: set[str] = set()
    changed_manifest_slugs: set[str] = set()

    for path in files:
        manifest_match = MANIFEST_PATTERN.match(path)
        if manifest_match:
            changed_manifest_slugs.add(manifest_match.group("slug"))
        for pattern in PRODUCTION_PATTERNS:
            match = pattern.match(path)
            if match:
                production_slugs.add(match.group("slug"))
                break

    validator = Path(__file__).with_name("validate_professional_skin_manifest.py")
    if not validator.is_file():
        raise SystemExit(f"missing validator: {validator}")

    failures = 0
    to_validate = changed_manifest_slugs | production_slugs
    for slug in sorted(to_validate):
        manifest = Path("docs/cobblemon-skin-review-manifests") / f"{slug}.json"
        if slug in production_slugs:
            manifest_raw = manifest.as_posix()
            if manifest_raw not in changed:
                print(
                    f"PROFESSIONAL SKIN GATE FAIL: production changed for {slug}, but {manifest_raw} "
                    "was not updated in the same PR. Review metadata/hashes must describe the exact head."
                )
                failures += 1
                continue
        if not manifest.is_file():
            print(f"PROFESSIONAL SKIN GATE FAIL: missing manifest for {slug}: {manifest}")
            failures += 1
            continue
        _, species = slug.split("_", 1)
        print(f"Validating professional production contract for {slug}: {manifest}")
        result = subprocess.run(
            [sys.executable, str(validator), str(manifest)],
            text=True,
        )
        if result.returncode:
            failures += 1

    if failures:
        raise SystemExit(f"PROFESSIONAL SKIN GATE: {failures} validation step(s) failed")

    if production_slugs:
        print(f"Professional manifest gate PASS for production species: {sorted(production_slugs)}")
    elif changed_manifest_slugs:
        print(f"Professional manifest-only validation PASS: {sorted(changed_manifest_slugs)}")
    else:
        print("No production skin assets or professional review manifests changed.")


if __name__ == "__main__":
    main()
