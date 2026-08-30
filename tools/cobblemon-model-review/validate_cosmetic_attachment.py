#!/usr/bin/env python3
"""Reject obviously detached Ouros cosmetic geometry.

This is a presentation-only structural gate. It does not model battle state or
animation authority. The official Cobblemon geometry remains the body source of
truth; appended `ouros_*` bones must inherit from that hierarchy and must not be
free-floating islands in bind pose.

The gate is intentionally reusable across species. It complements Blockbench
motion review; it does not replace visual inspection of official idle/battle/
locomotion clips.
"""
from __future__ import annotations

import argparse
import json
import math
from pathlib import Path


def load_geometry(path: Path) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    geos = data.get("minecraft:geometry")
    if not isinstance(geos, list) or len(geos) != 1:
        raise SystemExit(f"{path}: expected exactly one minecraft:geometry entry")
    geo = geos[0]
    if not isinstance(geo.get("bones"), list):
        raise SystemExit(f"{path}: missing bones")
    return geo


def aabb(cube: dict) -> tuple[tuple[float, float, float], tuple[float, float, float]]:
    origin = cube.get("origin")
    size = cube.get("size")
    if not (isinstance(origin, list) and isinstance(size, list) and len(origin) == len(size) == 3):
        raise ValueError("cube missing numeric origin/size")
    lo = tuple(float(v) for v in origin)
    hi = tuple(float(origin[i]) + float(size[i]) for i in range(3))
    return tuple(min(lo[i], hi[i]) for i in range(3)), tuple(max(lo[i], hi[i]) for i in range(3))


def gap(a: dict, b: dict) -> float:
    alo, ahi = aabb(a)
    blo, bhi = aabb(b)
    sq = 0.0
    for axis in range(3):
        if ahi[axis] < blo[axis]:
            d = blo[axis] - ahi[axis]
        elif bhi[axis] < alo[axis]:
            d = alo[axis] - bhi[axis]
        else:
            d = 0.0
        sq += d * d
    return math.sqrt(sq)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--official", type=Path, required=True)
    parser.add_argument("--candidate", type=Path, required=True)
    parser.add_argument("--anchor-gap", type=float, default=1.50)
    parser.add_argument("--piece-gap", type=float, default=1.00)
    args = parser.parse_args()

    official = load_geometry(args.official)
    candidate = load_geometry(args.candidate)
    official_bones = official["bones"]
    candidate_bones = candidate["bones"]
    original_count = len(official_bones)
    if candidate_bones[:original_count] != official_bones:
        raise SystemExit("candidate original-bone prefix is not JSON-equivalent to official source")

    original_names = {str(b.get("name")) for b in official_bones}
    all_names = {str(b.get("name")) for b in candidate_bones}
    extras = candidate_bones[original_count:]
    if not extras:
        raise SystemExit("candidate has no Ouros cosmetic bones")

    official_cubes = [cube for bone in official_bones for cube in bone.get("cubes", [])]
    if not official_cubes:
        raise SystemExit("official model has no cubes")

    reports = []
    for bone in extras:
        name = str(bone.get("name", ""))
        if not name.startswith("ouros_"):
            raise SystemExit(f"appended bone is not Ouros-owned: {name!r}")
        parent = bone.get("parent")
        if not isinstance(parent, str) or parent not in all_names:
            raise SystemExit(f"{name}: missing/unknown parent {parent!r}")

        # The parent chain must terminate in the immutable official hierarchy.
        seen = {name}
        cursor = parent
        while cursor not in original_names:
            if cursor in seen:
                raise SystemExit(f"{name}: cosmetic parent cycle at {cursor!r}")
            seen.add(cursor)
            parent_bone = next((b for b in extras if b.get("name") == cursor), None)
            if parent_bone is None:
                raise SystemExit(f"{name}: parent chain does not terminate in an official bone")
            cursor = parent_bone.get("parent")
            if not isinstance(cursor, str):
                raise SystemExit(f"{name}: parent chain terminates without an official bone")

        cubes = bone.get("cubes", [])
        if not cubes:
            raise SystemExit(f"{name}: cosmetic bone has no cubes")

        anchor_gap = min(gap(c, o) for c in cubes for o in official_cubes)
        if anchor_gap > args.anchor_gap:
            raise SystemExit(
                f"{name}: entire cosmetic group is detached from official body; "
                f"nearest bind-pose gap={anchor_gap:.3f} > {args.anchor_gap:.3f}"
            )

        floating = []
        for index, cube in enumerate(cubes):
            body_gap = min(gap(cube, o) for o in official_cubes)
            sibling_gaps = [gap(cube, other) for j, other in enumerate(cubes) if j != index]
            sibling_gap = min(sibling_gaps) if sibling_gaps else math.inf
            if body_gap > args.anchor_gap and sibling_gap > args.piece_gap:
                floating.append({"cube": index, "bodyGap": round(body_gap, 3), "nearestSiblingGap": round(sibling_gap, 3)})
        if floating:
            raise SystemExit(f"{name}: floating cosmetic cubes detected: {json.dumps(floating)}")

        reports.append({
            "bone": name,
            "parent": parent,
            "cubeCount": len(cubes),
            "nearestOfficialGap": round(anchor_gap, 3),
        })

    print(json.dumps({
        "officialBoneCount": original_count,
        "cosmeticBoneCount": len(extras),
        "anchorGapLimit": args.anchor_gap,
        "pieceGapLimit": args.piece_gap,
        "attachmentGate": "PASS",
        "groups": reports,
        "note": "Bind-pose geometry gate only; Blockbench official-animation review remains mandatory.",
    }, indent=2))


if __name__ == "__main__":
    main()
