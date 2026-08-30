#!/usr/bin/env python3
"""Validate same-species reference dossiers for changed production skin assets."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

PATTERNS = (
    re.compile(r"^fabric-adapter/src/main/resources/assets/autoptu/bedrock/pokemon/models/(?P<slug>\d{4}_[^/]+)/"),
    re.compile(r"^fabric-adapter/src/main/resources/assets/autoptu/bedrock/pokemon/textures/(?P<slug>\d{4}_[^/]+)/"),
    re.compile(r"^src/main/resources/data/autoptu/cobblemon/skins/(?P<slug>\d{4}_[^/]+)/"),
    re.compile(r"^docs/cobblemon-skins/(?P<slug>\d{4}_[^/]+)/"),
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

    slugs: set[str] = set()
    files = changed_files(args.base, args.head)
    for path in files:
        for pattern in PATTERNS:
            match = pattern.match(path)
            if match:
                slugs.add(match.group("slug"))
                break

    if not slugs:
        print("No production skin/species assets changed; same-species reference gate not required for this diff.")
        return

    validator = Path(__file__).with_name("validate_species_reference_dossier.py")
    if not validator.is_file():
        raise SystemExit(f"missing validator: {validator}")

    failures = 0
    for slug in sorted(slugs):
        dex, species = slug.split("_", 1)
        dossier = Path("docs/cobblemon-skin-reference-dossiers") / f"{dex}_{species}.json"
        print(f"Validating reference dossier for changed species {slug}: {dossier}")
        proc = subprocess.run(
            [sys.executable, str(validator), str(dossier), "--expected-species", species],
            text=True,
        )
        if proc.returncode != 0:
            failures += 1

    if failures:
        raise SystemExit(
            f"REFERENCE BLOCKED: {failures} changed species failed the mandatory three-reference dossier gate"
        )


if __name__ == "__main__":
    main()
