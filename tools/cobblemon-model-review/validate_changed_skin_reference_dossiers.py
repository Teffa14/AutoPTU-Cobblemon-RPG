#!/usr/bin/env python3
"""Validate changed skin reference research and enforce production gates."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

# Ouros currently ships custom Pokemon presentation assets in both namespaces:
# - autoptu-owned Bedrock model/texture folders used by newer slices; and
# - cobblemon-namespace resource-pack overrides used by legacy/current species
#   integrations such as Gengar Rift Warden.
# The hard reference gate must cover both or a blocked species could bypass CI.
PRODUCTION_PATTERNS = (
    re.compile(r"^fabric-adapter/src/main/resources/assets/(?:autoptu|cobblemon)/bedrock/pokemon/models/(?P<slug>\d{4}_[^/]+)/"),
    re.compile(r"^fabric-adapter/src/main/resources/assets/autoptu/bedrock/pokemon/textures/(?P<slug>\d{4}_[^/]+)/"),
    re.compile(r"^fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/(?P<slug>\d{4}_[^/]+)/"),
    re.compile(r"^src/main/resources/data/autoptu/cobblemon/skins/(?P<slug>\d{4}_[^/]+)/"),
    re.compile(r"^docs/cobblemon-skins/(?P<slug>\d{4}_[^/]+)/"),
)
DOSSIER_PATTERN = re.compile(
    r"^docs/cobblemon-skin-reference-dossiers/(?P<slug>\d{4}_[^/]+)\.json$"
)


def changed_files(base: str, head: str) -> list[str]:
    result = subprocess.run(
        ["git", "diff", "--name-only", f"{base}...{head}"],
        check=True,
        capture_output=True,
        text=True,
    )
    return [line.strip() for line in result.stdout.splitlines() if line.strip()]


def run_validator(validator: Path, slug: str, *, allow_blocked: bool) -> int:
    dex, species = slug.split("_", 1)
    dossier = Path("docs/cobblemon-skin-reference-dossiers") / f"{dex}_{species}.json"
    mode = "research-staging" if allow_blocked else "STRICT production"
    print(f"Validating {mode} dossier for {slug}: {dossier}")
    command = [
        sys.executable,
        str(validator),
        str(dossier),
        "--expected-species",
        species,
    ]
    if allow_blocked:
        command.append("--allow-blocked")
    return subprocess.run(command, text=True).returncode


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True)
    parser.add_argument("--head", required=True)
    args = parser.parse_args()

    files = changed_files(args.base, args.head)
    production_slugs: set[str] = set()
    changed_dossier_slugs: set[str] = set()

    for path in files:
        dossier_match = DOSSIER_PATTERN.match(path)
        if dossier_match:
            changed_dossier_slugs.add(dossier_match.group("slug"))
        for pattern in PRODUCTION_PATTERNS:
            match = pattern.match(path)
            if match:
                production_slugs.add(match.group("slug"))
                break

    validator = Path(__file__).with_name("validate_species_reference_dossier.py")
    if not validator.is_file():
        raise SystemExit(f"missing validator: {validator}")

    failures = 0

    # Research-only dossier edits may remain REFERENCE_BLOCKED, but their JSON,
    # candidate staging and any already-counted references must still be valid.
    for slug in sorted(changed_dossier_slugs):
        if run_validator(validator, slug, allow_blocked=True) != 0:
            failures += 1

    # Any production/species asset edit keeps the hard gate. A staged candidate
    # never opens production; three COMPLETE counted references do.
    for slug in sorted(production_slugs):
        if run_validator(validator, slug, allow_blocked=False) != 0:
            failures += 1

    if failures:
        raise SystemExit(
            f"REFERENCE BLOCKED: {failures} reference validation step(s) failed"
        )

    if not production_slugs:
        if changed_dossier_slugs:
            print(
                "Research dossiers are structurally valid; no production skin/species assets changed. "
                "A REFERENCE_BLOCKED dossier remains blocked for modeling."
            )
        else:
            print("No production skin/species assets or reference dossiers changed.")


if __name__ == "__main__":
    main()
