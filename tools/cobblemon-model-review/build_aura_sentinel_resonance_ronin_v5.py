#!/usr/bin/env python3
"""Resonance Ronin V5: restore premium silhouette with one continuous mantle system.

V4 removed the procedural cage/armor read but became too quiet at gameplay scale.
V5 keeps the exact official 87-bone Lucario biology and presentation-only authority
boundary, then replaces only Ouros cosmetic groups with a stronger asymmetric
shoulder-to-back-to-hip macroform, a quieter open cuirass, and broader tapered coat
panels. No alternate body rig or Cobblemon battle-state authority is introduced.
"""

from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V4_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v4.py"
spec = importlib.util.spec_from_file_location("resonance_v4", V4_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V4 builder")
v4 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v4)
v3 = v4.v3
v2 = v4.v2
v1 = v4.v1


def v5_shawl() -> dict:
    """One readable crescent mantle, rooted at torso3 and wrapped toward the hip."""
    return v1.bone("ouros_resonance_shawl", "torso3", [0, 30.0, 0.0], [
        # Root collar: shallow and body-following, not a full-width shoulder slab.
        v1.cube((-5.9, 30.25, 1.65), (9.8, 1.35, 1.75), 80,
                pivot=(-1.0, 30.9, 2.5), rotation=(-8, 0, -5)),
        # Dominant left shoulder shell: three overlapping masses with progressive
        # rotation and decreasing size create a swept contour instead of bars.
        v1.cube((-7.4, 28.8, -2.1), (5.7, 2.75, 4.7), 81,
                pivot=(-4.6, 30.15, 0.2), rotation=(4, -8, -14)),
        v1.cube((-9.0, 29.65, -1.35), (4.5, 2.7, 3.8), 82,
                pivot=(-6.7, 31.0, 0.5), rotation=(5, -13, -23)),
        v1.cube((-10.1, 30.95, -0.45), (3.35, 2.35, 2.85), 86,
                pivot=(-8.35, 32.1, 0.95), rotation=(6, -18, -32)),
        # Rear continuation: descending shells overlap into one mantle sweep and
        # terminate near the left hip, giving the silhouette a real contact path.
        v1.cube((-7.1, 26.4, 2.2), (5.25, 4.9, 1.25), 80,
                pivot=(-4.55, 29.8, 2.8), rotation=(-12, 7, -13)),
        v1.cube((-6.7, 22.6, 2.45), (4.35, 4.75, 1.05), 81,
                pivot=(-4.45, 26.6, 2.95), rotation=(-15, 10, -8)),
        v1.cube((-5.75, 19.75, 2.55), (3.45, 3.65, 0.9), 82,
                pivot=(-4.0, 22.9, 3.0), rotation=(-16, 12, -3)),
        # Front diagonal fold connects the signature shoulder to the open cuirass.
        v1.cube((-5.0, 27.15, -4.02), (7.9, 1.25, 0.42), 81,
                pivot=(-1.05, 27.8, -3.82), rotation=(0, 0, -27)),
        v1.cube((-3.35, 25.85, -4.08), (5.7, 0.62, 0.3), 84,
                pivot=(-0.5, 26.15, -3.93), rotation=(0, 0, -27)),
        # Small right counter-mass balances the composition without symmetry.
        v1.cube((3.55, 29.55, -1.25), (3.0, 1.55, 3.0), 82,
                pivot=(5.05, 30.3, 0.25), rotation=(2, 7, 12)),
    ])


def v5_cuirass() -> dict:
    """Four broad diagonals frame the biological chest spike with less visual noise."""
    return v1.bone("ouros_resonance_cuirass", "torso3", [0, 27.4, -3.1], [
        v1.cube((-4.15, 24.75, -4.05), (2.8, 5.0, 0.48), 80,
                pivot=(-2.75, 27.2, -3.82), rotation=(0, 0, -14)),
        v1.cube((1.35, 24.75, -4.05), (2.8, 5.0, 0.48), 82,
                pivot=(2.75, 27.2, -3.82), rotation=(0, 0, 14)),
        v1.cube((-3.4, 24.15, -4.12), (3.0, 0.78, 0.36), 81,
                pivot=(-1.9, 24.5, -3.94), rotation=(0, 0, -19)),
        v1.cube((0.4, 24.15, -4.12), (3.0, 0.78, 0.36), 81,
                pivot=(1.9, 24.5, -3.94), rotation=(0, 0, 19)),
    ])


def v5_coat() -> dict:
    """Three large tapered-looking panels with deliberate air instead of four thin bars."""
    return v1.bone("ouros_resonance_coat", "torso", [0, 20.1, 1.0], [
        # Broken waist wrap follows the diagonal mantle and leaves the center open.
        v1.cube((-5.45, 19.65, -3.35), (5.2, 0.78, 1.15), 80,
                pivot=(-2.85, 20.05, -2.78), rotation=(0, 0, -8)),
        v1.cube((0.55, 19.7, -3.3), (4.55, 0.72, 1.1), 82,
                pivot=(2.8, 20.05, -2.75), rotation=(0, 0, 9)),
        # Dominant left rear panel; broad enough to survive gameplay scale.
        v1.cube((-5.4, 11.2, 2.85), (4.15, 8.65, 0.78), 81,
                pivot=(-3.15, 19.2, 3.25), rotation=(-10, -5, 11)),
        v1.cube((-4.95, 10.95, 3.52), (3.25, 0.55, 0.24), 85,
                pivot=(-3.32, 11.2, 3.64), rotation=(-10, -5, 11)),
        # Center panel is shorter and offset, preserving negative space.
        v1.cube((-1.0, 12.45, 3.05), (3.1, 7.3, 0.65), 80,
                pivot=(0.45, 19.1, 3.35), rotation=(-11, 2, -2)),
        # Right panel is shortest, completing a descending rhythm rather than skirt symmetry.
        v1.cube((2.35, 13.55, 2.8), (3.15, 6.15, 0.7), 82,
                pivot=(3.75, 19.0, 3.15), rotation=(-8, 5, -11)),
    ])


def v5_left_greave() -> dict:
    return v1.bone("ouros_resonance_left_greave", "leg_left4", [3.5, 6.15, -1.5], [
        v1.cube((1.7, -1.75, -2.02), (3.25, 6.35, 0.62), 80,
                pivot=(3.3, 1.9, -1.72), rotation=(-7, 0, -5)),
        v1.cube((4.55, -0.35, -1.52), (0.68, 4.45, 2.15), 82,
                pivot=(4.88, 2.0, -0.45), rotation=(0, 5, -4)),
    ])


def v5_right_greave() -> dict:
    return v1.bone("ouros_resonance_right_greave", "leg_right4", [-3.5, 6.15, -1.5], [
        v1.cube((-4.95, -1.75, -2.02), (3.25, 6.35, 0.62), 80,
                pivot=(-3.3, 1.9, -1.72), rotation=(-7, 0, 5)),
        v1.cube((-5.23, -0.35, -1.52), (0.68, 4.45, 2.15), 81,
                pivot=(-4.88, 2.0, -0.45), rotation=(0, -5, 4)),
    ])


def replace_groups() -> int:
    v4.replace_groups()
    data = json.loads(v1.MODEL.read_text(encoding="utf-8"))
    bones = data["minecraft:geometry"][0]["bones"]
    replacements = {
        "ouros_resonance_shawl": v5_shawl(),
        "ouros_resonance_cuirass": v5_cuirass(),
        "ouros_resonance_coat": v5_coat(),
        "ouros_resonance_left_greave": v5_left_greave(),
        "ouros_resonance_right_greave": v5_right_greave(),
    }
    seen = set()
    for index, entry in enumerate(bones):
        name = entry.get("name")
        if name in replacements:
            bones[index] = replacements[name]
            seen.add(name)
    missing = set(replacements) - seen
    if missing:
        raise SystemExit(f"V4 model missing expected Ouros groups: {sorted(missing)}")
    v1.MODEL.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    extras = bones[v1.OFFICIAL_BONES:]
    return sum(len(entry.get("cubes", [])) for entry in extras)


def patch_manifest(cube_count: int) -> None:
    data = json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["artStatus"] = "ARTISTIC FAIL"
    data["production"]["modelSha256"] = v1.sha256(v1.MODEL)
    data["production"]["cosmeticCubeCount"] = cube_count
    data["builder"]["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v5.py"
    data["builder"]["command"] = ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v5.py"]
    data["qualityIntent"]["signaturePieces"] = [
        "Continuous asymmetric crescent mantle from left shoulder through rear torso to left hip",
        "Open four-plane cuirass preserving the biological chest spike as deliberate negative space",
        "Three descending battle-coat panels extending the mantle rhythm into the lower silhouette"
    ]
    data["qualityIntent"]["macroFormPlan"] = (
        "V5 preserves V4's clean open anatomy but restores authored presence through one connected crescent mantle. "
        "Three overlapping shoulder shells taper into three rear shells that descend toward the left hip, while a front diagonal fold "
        "ties that mass into a simplified four-plane cuirass. Three broad coat panels continue the same descending rhythm below the waist."
    )
    data["qualityIntent"]["gameplayReadGoal"] = (
        "At 160 px the silhouette must retain one unmistakable left-heavy crescent from shoulder to hip plus a descending three-panel coat rhythm, "
        "without reverting to cage hardware, repeated straight bars or oversized rectangular armor."
    )
    v1.MANIFEST.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--bootstrap", action="store_true")
    args = parser.parse_args()
    if v1.sha256(v1.BODY) != v1.OFFICIAL_NORMAL_SHA256:
        raise SystemExit("normal body texture drifted from official Lucario")
    if v1.sha256(v1.SHINY) != v1.OFFICIAL_SHINY_SHA256:
        raise SystemExit("shiny body texture drifted from official Lucario")
    cube_count = replace_groups()
    v1.write_overlay(v1.OVERLAY)
    v1.build_resolver()
    if args.bootstrap:
        patch_manifest(cube_count)
    print(json.dumps({
        "status": "BUILT",
        "concept": "Aura Sentinel — Resonance Ronin V5",
        "officialBones": v1.OFFICIAL_BONES,
        "cosmeticBones": 10,
        "cosmeticCubes": cube_count,
        "modelSha256": v1.sha256(v1.MODEL),
        "overlaySha256": v1.sha256(v1.OVERLAY),
        "resolverSha256": v1.sha256(v1.RESOLVER),
        "normalBodySha256": v1.sha256(v1.BODY),
        "shinyBodySha256": v1.sha256(v1.SHINY),
        "bodyTexelRework": "NONE",
        "visualChange": "continuous crescent mantle, quiet open cuirass, three broad descending coat panels"
    }, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
