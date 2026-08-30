#!/usr/bin/env python3
"""Require a production skin texture to remain byte-identical to the official source.

Ouros cosmetic materials live on validated free texels / overlay space. This gate
prevents a skin slice from disguising model drift by repainting biological body
texels. It is presentation-only and independent of battle authority.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--official", type=Path, required=True)
    parser.add_argument("--candidate", type=Path, required=True)
    parser.add_argument("--metadata", type=Path, required=True)
    parser.add_argument("--expected-official-sha256", required=True)
    args = parser.parse_args()

    official_hash = sha256(args.official)
    candidate_hash = sha256(args.candidate)
    if official_hash != args.expected_official_sha256:
        raise SystemExit(
            f"official texture hash mismatch: expected={args.expected_official_sha256} actual={official_hash}"
        )
    if candidate_hash != official_hash:
        raise SystemExit(
            "biological texture drift detected: production texture must be byte-identical "
            f"to official baseline; official={official_hash} candidate={candidate_hash}"
        )

    metadata = json.loads(args.metadata.read_text(encoding="utf-8"))
    if metadata.get("officialTextureBaselineSha256") != official_hash:
        raise SystemExit("metadata officialTextureBaselineSha256 does not match official texture")
    if metadata.get("derivedTextureSha256") != candidate_hash:
        raise SystemExit("metadata derivedTextureSha256 does not match production texture")
    if metadata.get("bodyTexelRework") not in ("NONE", "none", "None"):
        raise SystemExit("metadata.bodyTexelRework must be NONE under the preserved-body contract")

    print(json.dumps({
        "officialTextureBaselineSha256": official_hash,
        "productionTextureSha256": candidate_hash,
        "bodyTexelRework": "NONE",
        "officialBodyTexturePreserved": True,
    }, indent=2))


if __name__ == "__main__":
    main()
