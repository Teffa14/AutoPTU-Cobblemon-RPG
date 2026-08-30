#!/usr/bin/env python3
"""Emit the canonical professional manifests that require heavy review for a diff."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
from pathlib import Path

MANIFEST_RE = re.compile(
    r"^docs/cobblemon-skin-review-manifests/(?P<slug>\d{4}_[a-z0-9_]+)\.json$"
)
PRODUCTION_RE = re.compile(
    r"^fabric-adapter/src/main/resources/assets/(?:autoptu|cobblemon)/(?:bedrock/pokemon/models|"
    r"bedrock/pokemon/resolvers|bedrock/pokemon/textures|textures/pokemon)/(?P<slug>\d{4}_[a-z0-9_]+)/"
)
PROFESSIONAL = {"PROFESSIONAL_CANDIDATE", "OWNER_APPROVED_RELEASE"}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True)
    parser.add_argument("--head", required=True)
    args = parser.parse_args()

    diff = subprocess.run(
        ["git", "diff", "--name-only", f"{args.base}...{args.head}"],
        check=True,
        capture_output=True,
        text=True,
    )
    registry = json.loads(Path("docs/cobblemon-skin-registry.json").read_text(encoding="utf-8"))
    entries = {entry["slug"]: entry for entry in registry["entries"]}
    slugs: set[str] = set()
    for raw in diff.stdout.splitlines():
        path = raw.strip().replace("\\", "/")
        manifest = MANIFEST_RE.match(path)
        production = PRODUCTION_RE.match(path)
        if manifest:
            slugs.add(manifest.group("slug"))
        if production:
            slugs.add(production.group("slug"))

    manifests: list[str] = []
    for slug in sorted(slugs):
        entry = entries.get(slug)
        if not entry or entry.get("lifecycle") not in PROFESSIONAL:
            continue
        manifest = entry.get("manifest")
        if not isinstance(manifest, str) or not MANIFEST_RE.fullmatch(manifest):
            raise SystemExit(f"registry contains unsafe professional manifest path for {slug}")
        if not Path(manifest).is_file():
            raise SystemExit(f"registry professional manifest is missing for {slug}: {manifest}")
        manifests.append(manifest)

    print(json.dumps(manifests, separators=(",", ":")))


if __name__ == "__main__":
    main()
