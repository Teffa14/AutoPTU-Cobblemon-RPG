#!/usr/bin/env python3
"""Validate an Ouros full-surface texture derived from an official Cobblemon texture.

This gate deliberately allows occupied body texels to change. It protects the
immutable official baseline, texture dimensions, declared provenance and review
metadata. Anatomy/UV-layout preservation remains the responsibility of
validate_original_model.py because the model, not this texture, owns the UV map.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image

REQUIRED_METADATA_FIELDS = (
    "officialTextureBaselineSha256",
    "derivedTexture",
    "derivedTextureSha256",
    "bodyTexelRework",
    "paletteIntent",
    "materialIntent",
)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def require_text(metadata: dict, key: str) -> str:
    value = metadata.get(key)
    if not isinstance(value, str) or not value.strip():
        raise SystemExit(f"metadata.{key} must be a non-empty string")
    return value.strip()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--official", type=Path, required=True)
    parser.add_argument("--derived", type=Path, required=True)
    parser.add_argument("--metadata", type=Path, required=True)
    parser.add_argument("--expected-official-sha256", required=True)
    parser.add_argument("--expected-derived-sha256")
    args = parser.parse_args()

    for path in (args.official, args.derived, args.metadata):
        if not path.is_file():
            raise SystemExit(f"missing required file: {path}")

    official_hash = sha256(args.official)
    if official_hash != args.expected_official_sha256:
        raise SystemExit(
            "official texture hash mismatch: "
            f"expected={args.expected_official_sha256} actual={official_hash}"
        )

    derived_hash = sha256(args.derived)
    if args.expected_derived_sha256 and derived_hash != args.expected_derived_sha256:
        raise SystemExit(
            "derived texture hash mismatch: "
            f"expected={args.expected_derived_sha256} actual={derived_hash}"
        )

    metadata = json.loads(args.metadata.read_text(encoding="utf-8"))
    if not isinstance(metadata, dict):
        raise SystemExit("metadata root must be an object")

    missing = [key for key in REQUIRED_METADATA_FIELDS if key not in metadata]
    if missing:
        raise SystemExit(f"metadata missing required fields: {missing}")

    for key in ("bodyTexelRework", "paletteIntent", "materialIntent"):
        require_text(metadata, key)

    metadata_official = require_text(metadata, "officialTextureBaselineSha256")
    if metadata_official != official_hash:
        raise SystemExit(
            "metadata official baseline hash mismatch: "
            f"metadata={metadata_official} actual={official_hash}"
        )

    metadata_derived = require_text(metadata, "derivedTextureSha256")
    if metadata_derived != derived_hash:
        raise SystemExit(
            "metadata derived hash mismatch: "
            f"metadata={metadata_derived} actual={derived_hash}"
        )

    derived_declared = require_text(metadata, "derivedTexture")
    if Path(derived_declared).name != args.derived.name:
        raise SystemExit(
            "metadata derivedTexture filename does not match validated file: "
            f"metadata={derived_declared!r} actual={args.derived.name!r}"
        )

    official = Image.open(args.official).convert("RGBA")
    derived = Image.open(args.derived).convert("RGBA")
    if derived.size != official.size:
        raise SystemExit(
            f"texture dimensions drifted: official={official.size} derived={derived.size}"
        )

    official_pixels = list(official.getdata())
    derived_pixels = list(derived.getdata())

    changed = 0
    changed_opaque = 0
    changed_transparent = 0
    alpha_semantics_changed = 0
    for old, new in zip(official_pixels, derived_pixels, strict=True):
        if old == new:
            continue
        changed += 1
        if old[3] == 0:
            changed_transparent += 1
        else:
            changed_opaque += 1
        if (old[3] == 0) != (new[3] == 0):
            alpha_semantics_changed += 1

    if changed == 0:
        raise SystemExit("derived texture is byte-visually identical to the official baseline")
    if changed_opaque == 0:
        raise SystemExit(
            "derived texture changes no occupied/visible baseline pixels; "
            "full-surface rework must not collapse back to unused-texel-only painting"
        )

    allow_alpha_change = metadata.get("allowAlphaSemanticsChange", False)
    if alpha_semantics_changed and allow_alpha_change is not True:
        raise SystemExit(
            f"derived texture changes transparent/opaque semantics on {alpha_semantics_changed} pixels; "
            "set allowAlphaSemanticsChange=true only with an explicitly reviewed resolver/material need"
        )

    report = {
        "officialTexture": str(args.official),
        "officialTextureBaselineSha256": official_hash,
        "derivedTexture": str(args.derived),
        "derivedTextureSha256": derived_hash,
        "dimensions": [official.width, official.height],
        "changedPixels": changed,
        "changedOpaqueBaselinePixels": changed_opaque,
        "changedTransparentBaselinePixels": changed_transparent,
        "alphaSemanticsChangedPixels": alpha_semantics_changed,
        "bodyTexelRework": metadata["bodyTexelRework"],
        "paletteIntent": metadata["paletteIntent"],
        "materialIntent": metadata["materialIntent"],
        "fullSurfaceDerivationValidated": True,
    }
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
