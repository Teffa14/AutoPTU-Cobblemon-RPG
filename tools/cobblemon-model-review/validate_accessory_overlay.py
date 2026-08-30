#!/usr/bin/env python3
"""Prove that an Ouros accessory overlay only occupies transparent official texels.

The official Pokemon texture is immutable. This gate permits accessory material
swatches only where the pinned official texture is fully transparent. It does
not alter UVs or infer runtime battle state.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from PIL import Image


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--official", type=Path, required=True)
    parser.add_argument("--overlay", type=Path, required=True)
    parser.add_argument("--expected-official-sha256", required=True)
    args = parser.parse_args()

    official_hash = sha256(args.official)
    if official_hash != args.expected_official_sha256:
        raise SystemExit(
            f"official texture hash mismatch: expected={args.expected_official_sha256} actual={official_hash}"
        )

    official = Image.open(args.official).convert("RGBA")
    overlay = Image.open(args.overlay).convert("RGBA")
    if overlay.size != official.size:
        raise SystemExit(f"overlay dimensions {overlay.size} != official {official.size}")

    occupied = []
    used = []
    for y in range(official.height):
        for x in range(official.width):
            oa = overlay.getpixel((x, y))[3]
            if oa == 0:
                continue
            used.append((x, y, oa))
            official_alpha = official.getpixel((x, y))[3]
            if official_alpha != 0:
                occupied.append((x, y, official_alpha, oa))

    if not used:
        raise SystemExit("accessory overlay has no non-transparent texels")
    if occupied:
        preview = occupied[:20]
        raise SystemExit(
            "accessory overlay paints occupied biological texels; first conflicts="
            + json.dumps(preview)
        )

    xs = [p[0] for p in used]
    ys = [p[1] for p in used]
    print(json.dumps({
        "overlaySha256": sha256(args.overlay),
        "officialTextureSha256": official_hash,
        "dimensions": list(official.size),
        "overlayNonTransparentTexels": len(used),
        "occupiedOfficialTexelConflicts": 0,
        "usedBounds": [min(xs), min(ys), max(xs), max(ys)],
        "freeTexelOverlayGate": "PASS",
    }, indent=2))


if __name__ == "__main__":
    main()
