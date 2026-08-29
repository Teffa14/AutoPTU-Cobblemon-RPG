#!/usr/bin/env python3
"""Validate that an Ouros cosmetic preserves an official Cobblemon model exactly.

The official geometry is the anatomical source of truth. Existing bones must remain
JSON-equivalent and in the same order. A cosmetic may only append new `ouros_*`
bones unless a future documented exception deliberately relaxes this gate.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path


def load_geometry(path: Path) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    geometries = data.get("minecraft:geometry")
    if not isinstance(geometries, list) or len(geometries) != 1:
        raise SystemExit(f"{path}: expected exactly one minecraft:geometry entry")
    geometry = geometries[0]
    if not isinstance(geometry.get("bones"), list):
        raise SystemExit(f"{path}: geometry has no bones array")
    return geometry


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--official", type=Path, required=True)
    parser.add_argument("--candidate", type=Path, required=True)
    args = parser.parse_args()

    official = load_geometry(args.official)
    candidate = load_geometry(args.candidate)

    official_description = official.get("description", {})
    candidate_description = candidate.get("description", {})
    for key in (
        "texture_width",
        "texture_height",
        "visible_bounds_width",
        "visible_bounds_height",
        "visible_bounds_offset",
    ):
        if candidate_description.get(key) != official_description.get(key):
            raise SystemExit(
                f"description.{key} drifted: official={official_description.get(key)!r} "
                f"candidate={candidate_description.get(key)!r}"
            )

    official_bones = official["bones"]
    candidate_bones = candidate["bones"]
    if len(candidate_bones) < len(official_bones):
        raise SystemExit(
            f"candidate removed bones: official={len(official_bones)} candidate={len(candidate_bones)}"
        )

    for index, official_bone in enumerate(official_bones):
        candidate_bone = candidate_bones[index]
        name = official_bone.get("name", f"index-{index}")
        if candidate_bone != official_bone:
            raise SystemExit(
                f"original bone drift at index {index} ({name!r}); "
                "cosmetics must not rewrite original Cobblemon bones/cubes/pivots/UVs"
            )

    extras = candidate_bones[len(official_bones) :]
    bad_names = [bone.get("name") for bone in extras if not str(bone.get("name", "")).startswith("ouros_")]
    if bad_names:
        raise SystemExit(f"non-Ouros appended bones are forbidden: {bad_names}")

    print(
        json.dumps(
            {
                "officialBoneCount": len(official_bones),
                "candidateBoneCount": len(candidate_bones),
                "appendedOurosBones": [bone.get("name") for bone in extras],
                "preserved": True,
            },
            indent=2,
        )
    )


if __name__ == "__main__":
    main()
