#!/usr/bin/env python3
"""Reject obviously detached Ouros cosmetic geometry.

This is a presentation-only structural gate. It does not model battle state or
animation authority. The official Cobblemon geometry remains the body source of
truth; appended `ouros_*` bones must inherit from that hierarchy and must not be
free-floating islands in bind pose.

The gate validates both Bedrock cubes and experimental `poly_mesh` geometry.
For Ouros poly meshes it additionally requires explicit indexed polygons and a
single connected vertex component so a distant island cannot hide inside one
large mesh AABB.

The gate complements Blockbench motion review; it does not replace visual
inspection of official idle/battle/locomotion clips.
"""
from __future__ import annotations

import argparse
import json
import math
from pathlib import Path

Bounds = tuple[tuple[float, float, float], tuple[float, float, float]]


def load_geometry(path: Path) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    geos = data.get("minecraft:geometry")
    if not isinstance(geos, list) or len(geos) != 1:
        raise SystemExit(f"{path}: expected exactly one minecraft:geometry entry")
    geo = geos[0]
    if not isinstance(geo.get("bones"), list):
        raise SystemExit(f"{path}: missing bones")
    return geo


def cube_bounds(cube: dict) -> Bounds:
    origin = cube.get("origin")
    size = cube.get("size")
    if not (isinstance(origin, list) and isinstance(size, list) and len(origin) == len(size) == 3):
        raise ValueError("cube missing numeric origin/size")
    lo = tuple(float(v) for v in origin)
    hi = tuple(float(origin[i]) + float(size[i]) for i in range(3))
    return tuple(min(lo[i], hi[i]) for i in range(3)), tuple(max(lo[i], hi[i]) for i in range(3))


def mesh_bounds(mesh: dict) -> Bounds:
    positions = mesh.get("positions")
    if not isinstance(positions, list) or len(positions) < 3:
        raise ValueError("poly_mesh requires at least 3 positions")
    parsed = []
    for index, pos in enumerate(positions):
        if not isinstance(pos, list) or len(pos) != 3:
            raise ValueError(f"poly_mesh position {index} must contain 3 numbers")
        parsed.append(tuple(float(v) for v in pos))
    return (
        tuple(min(pos[axis] for pos in parsed) for axis in range(3)),
        tuple(max(pos[axis] for pos in parsed) for axis in range(3)),
    )


def bounds_gap(a: Bounds, b: Bounds) -> float:
    alo, ahi = a
    blo, bhi = b
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


def validate_mesh_topology(name: str, mesh: dict) -> None:
    positions = mesh.get("positions")
    polys = mesh.get("polys")
    if not isinstance(positions, list) or len(positions) < 3:
        raise SystemExit(f"{name}: poly_mesh requires at least 3 positions")
    if not isinstance(polys, list) or not polys:
        raise SystemExit(f"{name}: Ouros poly_mesh must use explicit indexed polys")

    used: set[int] = set()
    adjacency: dict[int, set[int]] = {}
    for face_index, face in enumerate(polys):
        if not isinstance(face, list) or len(face) not in (3, 4):
            raise SystemExit(f"{name}: poly_mesh face {face_index} must be a triangle or quad")
        face_positions: list[int] = []
        for vertex in face:
            if not isinstance(vertex, list) or len(vertex) != 3:
                raise SystemExit(f"{name}: face {face_index} vertex must index position/normal/uv")
            pos_index = vertex[0]
            if not isinstance(pos_index, int) or not 0 <= pos_index < len(positions):
                raise SystemExit(f"{name}: face {face_index} has invalid position index {pos_index!r}")
            face_positions.append(pos_index)
            used.add(pos_index)
            adjacency.setdefault(pos_index, set())
        for left in face_positions:
            adjacency[left].update(right for right in face_positions if right != left)

    if not used:
        raise SystemExit(f"{name}: poly_mesh has no referenced positions")
    start = next(iter(used))
    seen = {start}
    stack = [start]
    while stack:
        cursor = stack.pop()
        for nxt in adjacency.get(cursor, set()):
            if nxt not in seen:
                seen.add(nxt)
                stack.append(nxt)
    if seen != used:
        raise SystemExit(f"{name}: poly_mesh contains disconnected vertex components")


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

    official_bounds = [cube_bounds(cube) for bone in official_bones for cube in bone.get("cubes", [])]
    if not official_bounds:
        raise SystemExit("official model has no cubes")

    reports = []
    for bone in extras:
        name = str(bone.get("name", ""))
        if not name.startswith("ouros_"):
            raise SystemExit(f"appended bone is not Ouros-owned: {name!r}")
        parent = bone.get("parent")
        if not isinstance(parent, str) or parent not in all_names:
            raise SystemExit(f"{name}: missing/unknown parent {parent!r}")

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
        if cubes is None:
            cubes = []
        if not isinstance(cubes, list):
            raise SystemExit(f"{name}: cubes must be a list")
        pieces: list[tuple[str, int, Bounds]] = [
            ("cube", index, cube_bounds(cube)) for index, cube in enumerate(cubes)
        ]

        mesh = bone.get("poly_mesh")
        mesh_present = mesh is not None
        if mesh_present:
            if not isinstance(mesh, dict):
                raise SystemExit(f"{name}: poly_mesh must be an object")
            validate_mesh_topology(name, mesh)
            pieces.append(("poly_mesh", 0, mesh_bounds(mesh)))

        if not pieces:
            raise SystemExit(f"{name}: cosmetic bone has no cubes or poly_mesh")

        anchor_gap = min(bounds_gap(piece[2], official) for piece in pieces for official in official_bounds)
        if anchor_gap > args.anchor_gap:
            raise SystemExit(
                f"{name}: entire cosmetic group is detached from official body; "
                f"nearest bind-pose gap={anchor_gap:.3f} > {args.anchor_gap:.3f}"
            )

        floating = []
        for index, (kind, piece_index, piece) in enumerate(pieces):
            body_gap = min(bounds_gap(piece, official) for official in official_bounds)
            sibling_gaps = [bounds_gap(piece, other[2]) for j, other in enumerate(pieces) if j != index]
            sibling_gap = min(sibling_gaps) if sibling_gaps else math.inf
            if body_gap > args.anchor_gap and sibling_gap > args.piece_gap:
                floating.append({
                    "kind": kind,
                    "piece": piece_index,
                    "bodyGap": round(body_gap, 3),
                    "nearestSiblingGap": round(sibling_gap, 3),
                })
        if floating:
            raise SystemExit(f"{name}: floating cosmetic geometry detected: {json.dumps(floating)}")

        reports.append({
            "bone": name,
            "parent": parent,
            "cubeCount": len(cubes),
            "polyMesh": mesh_present,
            "nearestOfficialGap": round(anchor_gap, 3),
        })

    print(json.dumps({
        "officialBoneCount": original_count,
        "cosmeticBoneCount": len(extras),
        "anchorGapLimit": args.anchor_gap,
        "pieceGapLimit": args.piece_gap,
        "attachmentGate": "PASS",
        "groups": reports,
        "note": "Bind-pose geometry gate for cubes/poly_mesh only; Blockbench official-animation review remains mandatory.",
    }, indent=2))


if __name__ == "__main__":
    main()
