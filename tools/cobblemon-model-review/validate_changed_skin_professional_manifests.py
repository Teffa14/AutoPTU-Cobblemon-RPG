#!/usr/bin/env python3
"""Require and validate a professional review manifest for every surviving production skin change."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

PRODUCTION_PATTERNS = (
    re.compile(r"^fabric-adapter/src/main/resources/assets/(?:autoptu|cobblemon)/bedrock/pokemon/models/(?P<slug>\d{4}_[^/]+)/"),
    re.compile(r"^fabric-adapter/src/main/resources/assets/(?:autoptu|cobblemon)/bedrock/pokemon/resolvers/(?P<slug>\d{4}_[^/]+)/"),
    re.compile(r"^fabric-adapter/src/main/resources/assets/autoptu/bedrock/pokemon/textures/(?P<slug>\d{4}_[^/]+)/"),
    re.compile(r"^fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/(?P<slug>\d{4}_[^/]+)/"),
    re.compile(r"^src/main/resources/data/autoptu/cobblemon/skins/(?P<slug>\d{4}_[^/]+)/"),
)
MANIFEST_PATTERN = re.compile(
    r"^docs/cobblemon-skin-review-manifests/(?P<slug>\d{4}_[^/]+)\.json$"
)
REGISTRY_PATH = "docs/cobblemon-skin-registry.json"
PROFESSIONAL_LIFECYCLES = {"PROFESSIONAL_CANDIDATE", "OWNER_APPROVED_RELEASE"}


def changed_files(base: str, head: str) -> list[tuple[str, str]]:
    result = subprocess.run(
        ["git", "diff", "--name-status", "--find-renames", f"{base}...{head}"],
        check=True,
        capture_output=True,
        text=True,
    )
    changes: list[tuple[str, str]] = []
    for line in result.stdout.splitlines():
        if not line.strip():
            continue
        parts = line.split("\t")
        status = parts[0]
        if status.startswith(("R", "C")):
            if len(parts) < 3:
                raise SystemExit(f"malformed git diff rename/copy record: {line!r}")
            path = parts[-1]
        else:
            if len(parts) < 2:
                raise SystemExit(f"malformed git diff record: {line!r}")
            path = parts[1]
        changes.append((status, path))
    return changes


def registry_entries_at(revision: str) -> dict[str, dict]:
    result = subprocess.run(
        ["git", "show", f"{revision}:{REGISTRY_PATH}"],
        capture_output=True,
        text=True,
    )
    if result.returncode:
        return {}
    payload = json.loads(result.stdout)
    return {entry["slug"]: entry for entry in payload.get("entries", [])}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True)
    parser.add_argument("--head", required=True)
    args = parser.parse_args()

    file_changes = changed_files(args.base, args.head)
    changed = {path for _, path in file_changes}
    production_slugs: set[str] = set()
    changed_manifest_slugs: set[str] = set()
    removed_production_slugs: set[str] = set()

    for status, path in file_changes:
        manifest_match = MANIFEST_PATTERN.match(path)
        if manifest_match and not status.startswith("D"):
            changed_manifest_slugs.add(manifest_match.group("slug"))
        for pattern in PRODUCTION_PATTERNS:
            match = pattern.match(path)
            if match:
                slug = match.group("slug")
                if status.startswith("D"):
                    removed_production_slugs.add(slug)
                else:
                    production_slugs.add(slug)
                break

    registry_validator = Path(__file__).with_name("validate_skin_registry.py")
    registry_result = subprocess.run(
        [sys.executable, str(registry_validator)],
        text=True,
    )
    if registry_result.returncode:
        raise SystemExit("PROFESSIONAL SKIN GATE: registry validation failed")

    registry = json.loads(Path(REGISTRY_PATH).read_text(encoding="utf-8"))
    entries = {entry["slug"]: entry for entry in registry["entries"]}
    base_entries = registry_entries_at(args.base)

    validator = Path(__file__).with_name("validate_professional_skin_manifest.py")
    if not validator.is_file():
        raise SystemExit(f"missing validator: {validator}")

    failures = 0
    to_validate = changed_manifest_slugs | production_slugs
    for slug in sorted(to_validate):
        entry = entries.get(slug)
        if entry is None:
            print(f"PROFESSIONAL SKIN GATE FAIL: {slug} is not present in {REGISTRY_PATH}")
            failures += 1
            continue
        lifecycle = entry["lifecycle"]
        manifest_raw = entry.get("manifest")
        if slug in production_slugs:
            if lifecycle not in PROFESSIONAL_LIFECYCLES:
                print(
                    f"PROFESSIONAL SKIN GATE FAIL: production asset {slug} is locked as {lifecycle}. "
                    "Complete the three-reference gate, add a valid professional manifest, and promote "
                    "the registry entry before changing production bytes."
                )
                failures += 1
                continue
            base_lifecycle = base_entries.get(slug, {}).get("lifecycle")
            if base_lifecycle not in PROFESSIONAL_LIFECYCLES and REGISTRY_PATH not in changed:
                print(
                    f"PROFESSIONAL SKIN GATE FAIL: production changed for {slug}, but its base lifecycle "
                    f"was {base_lifecycle or 'UNREGISTERED'} and {REGISTRY_PATH} was not updated in the same PR."
                )
                failures += 1
                continue
            if manifest_raw not in changed:
                print(
                    f"PROFESSIONAL SKIN GATE FAIL: production changed for {slug}, but {manifest_raw} "
                    "was not updated in the same PR. Review metadata/hashes must describe the exact head."
                )
                failures += 1
                continue
        if lifecycle not in PROFESSIONAL_LIFECYCLES:
            print(
                f"PROFESSIONAL SKIN GATE FAIL: a professional manifest changed for locked skin "
                f"{slug} ({lifecycle}); promote it through {REGISTRY_PATH} in the same PR"
            )
            failures += 1
            continue
        if not isinstance(manifest_raw, str):
            print(f"PROFESSIONAL SKIN GATE FAIL: registry has no manifest for {slug}")
            failures += 1
            continue
        manifest = Path(manifest_raw)
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

    if removed_production_slugs:
        print(
            "Allowed production skin removals without professional manifests: "
            f"{sorted(removed_production_slugs)}"
        )
    if production_slugs:
        print(f"Professional manifest gate PASS for production species: {sorted(production_slugs)}")
    elif changed_manifest_slugs:
        print(f"Professional manifest-only validation PASS: {sorted(changed_manifest_slugs)}")
    elif not removed_production_slugs:
        print("No production skin assets or professional review manifests changed.")


if __name__ == "__main__":
    main()
