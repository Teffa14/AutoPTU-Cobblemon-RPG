#!/usr/bin/env python3
"""Resonance Ronin V4: whole-character macroform cleanup after V3 Blockbench QA.

V3 fixed the detached vertical dorsal prop, but exact-head renders still showed
boxy shoulder slabs, parallel chest bars, skirt-like rectangular coat masses and
large straight greaves. V4 keeps the exact official Lucario biology and the same
presentation-only authority boundary while replacing those Ouros groups with a
lower-density diagonal wrap language: shoulder-to-chest sash, open chest arcs,
separated coat tails and tapered shin plates.
"""

from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V3_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v3.py"
spec = importlib.util.spec_from_file_location("resonance_v3", V3_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V3 builder")
v3 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v3)
v2 = v3.v2
v1 = v3.v1


def v4_shawl() -> dict:
    return v1.bone("ouros_resonance_shawl", "torso3", [0, 30.0, 0.0], [
        # Left shoulder root and two overlapping tapered steps. No full-width slab.
        v1.cube((-7.4, 29.0, -2.15), (5.4, 2.35, 4.45), 80,
                pivot=(-4.7, 30.15, 0.05), rotation=(2, -5, -13)),
        v1.cube((-9.0, 29.75, -1.55), (3.7, 2.5, 3.65), 81,
                pivot=(-7.1, 31.0, 0.25), rotation=(3, -8, -20)),
        v1.cube((-9.9, 30.8, -0.9), (2.75, 2.05, 2.8), 86,
                pivot=(-8.5, 31.8, 0.45), rotation=(4, -10, -27)),
        # Narrow counterweight on the right shoulder keeps asymmetry deliberate.
        v1.cube((3.6, 29.3, -1.7), (3.4, 1.55, 3.5), 82,
                pivot=(5.3, 30.1, 0.05), rotation=(1, 5, 10)),
        # Front diagonal sash bridges the dominant shoulder into the torso.
        v1.cube((-4.8, 27.5, -4.05), (8.2, 1.05, 0.38), 81,
                pivot=(-0.7, 28.0, -3.86), rotation=(0, 0, -24)),
        v1.cube((-3.55, 25.95, -4.12), (6.3, 0.46, 0.26), 84,
                pivot=(-0.4, 26.18, -3.99), rotation=(0, 0, -24)),
        # Back collar contact keeps the mantle visually rooted to the body.
        v1.cube((-4.9, 30.55, 2.1), (7.8, 1.15, 1.45), 80,
                pivot=(-1.0, 31.1, 2.8), rotation=(-5, 0, -5)),
    ])


def v4_cuirass() -> dict:
    return v1.bone("ouros_resonance_cuirass", "torso3", [0, 27.6, -3.2], [
        # Open arcs keep Lucario's chest spike and cream torso visible.
        v1.cube((-4.25, 25.0, -4.1), (2.6, 4.7, 0.42), 80,
                pivot=(-2.95, 27.3, -3.9), rotation=(0, 0, -13)),
        v1.cube((1.65, 25.0, -4.1), (2.6, 4.7, 0.42), 82,
                pivot=(2.95, 27.3, -3.9), rotation=(0, 0, 13)),
        # Lower diagonals converge without forming a rectangular chest frame.
        v1.cube((-3.7, 24.15, -4.18), (3.1, 0.7, 0.34), 81,
                pivot=(-2.15, 24.5, -4.0), rotation=(0, 0, -16)),
        v1.cube((0.6, 24.15, -4.18), (3.1, 0.7, 0.34), 81,
                pivot=(2.15, 24.5, -4.0), rotation=(0, 0, 16)),
        # One asymmetric lit accent establishes material hierarchy.
        v1.cube((-3.85, 28.85, -4.36), (2.4, 0.4, 0.24), 85,
                pivot=(-2.65, 29.05, -4.24), rotation=(0, 0, -13)),
    ])


def v4_coat() -> dict:
    return v1.bone("ouros_resonance_coat", "torso", [0, 20.2, 1.0], [
        # Broken waist wrap leaves negative space instead of a solid belt box.
        v1.cube((-5.6, 19.7, -3.45), (5.0, 0.72, 1.1), 80,
                pivot=(-3.1, 20.05, -2.9), rotation=(0, 0, -5)),
        v1.cube((0.45, 19.7, -3.45), (4.65, 0.72, 1.1), 82,
                pivot=(2.8, 20.05, -2.9), rotation=(0, 0, 7)),
        v1.cube((-3.0, 19.55, -3.72), (5.3, 0.3, 0.22), 84,
                pivot=(-0.35, 19.7, -3.61), rotation=(0, 0, -7)),
        # Four independent tapered tails produce rhythm and air between masses.
        v1.cube((-5.15, 11.45, 2.95), (3.2, 8.35, 0.62), 80,
                pivot=(-3.35, 19.25, 3.25), rotation=(-8, -4, 9)),
        v1.cube((-1.9, 12.35, 3.15), (2.55, 7.45, 0.55), 81,
                pivot=(-0.65, 19.2, 3.42), rotation=(-10, -3, 3)),
        v1.cube((0.65, 12.75, 3.15), (2.45, 7.05, 0.52), 82,
                pivot=(1.85, 19.2, 3.4), rotation=(-9, 3, -4)),
        v1.cube((3.0, 13.5, 2.95), (2.65, 6.3, 0.58), 81,
                pivot=(4.25, 19.1, 3.22), rotation=(-7, 4, -10)),
        # One cyan hem on the dominant tail, not repeated bars everywhere.
        v1.cube((-4.75, 11.35, 3.5), (2.6, 0.38, 0.2), 85,
                pivot=(-3.45, 11.55, 3.6), rotation=(-8, -4, 9)),
    ])


def v4_left_greave() -> dict:
    return v1.bone("ouros_resonance_left_greave", "leg_left4", [3.5, 6.15, -1.5], [
        v1.cube((1.55, -1.9, -2.08), (3.5, 6.75, 0.7), 80,
                pivot=(3.3, 2.0, -1.72), rotation=(-5, 0, -4)),
        v1.cube((4.65, -0.8, -1.62), (0.75, 5.3, 2.45), 82,
                pivot=(5.0, 2.0, -0.4), rotation=(0, 4, -3)),
        v1.cube((1.85, 4.25, -2.2), (3.05, 1.25, 0.72), 84,
                pivot=(3.4, 4.9, -1.82), rotation=(-9, 0, -4)),
    ])


def v4_right_greave() -> dict:
    return v1.bone("ouros_resonance_right_greave", "leg_right4", [-3.5, 6.15, -1.5], [
        v1.cube((-5.05, -1.9, -2.08), (3.5, 6.75, 0.7), 80,
                pivot=(-3.3, 2.0, -1.72), rotation=(-5, 0, 4)),
        v1.cube((-5.4, -0.8, -1.62), (0.75, 5.3, 2.45), 81,
                pivot=(-5.0, 2.0, -0.4), rotation=(0, -4, 3)),
        v1.cube((-4.9, 4.25, -2.2), (3.05, 1.25, 0.72), 84,
                pivot=(-3.4, 4.9, -1.82), rotation=(-9, 0, 4)),
    ])


def replace_groups() -> int:
    v3.build_model()
    data = json.loads(v1.MODEL.read_text(encoding="utf-8"))
    bones = data["minecraft:geometry"][0]["bones"]
    replacements = {
        "ouros_resonance_shawl": v4_shawl(),
        "ouros_resonance_cuirass": v4_cuirass(),
        "ouros_resonance_coat": v4_coat(),
        "ouros_resonance_left_greave": v4_left_greave(),
        "ouros_resonance_right_greave": v4_right_greave(),
    }
    seen = set()
    for index, entry in enumerate(bones):
        name = entry.get("name")
        if name in replacements:
            bones[index] = replacements[name]
            seen.add(name)
    missing = set(replacements) - seen
    if missing:
        raise SystemExit(f"V3 model missing expected Ouros groups: {sorted(missing)}")
    v1.MODEL.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    extras = bones[v1.OFFICIAL_BONES:]
    return sum(len(entry.get("cubes", [])) for entry in extras)


def patch_manifest(cube_count: int) -> None:
    data = json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["production"]["modelSha256"] = v1.sha256(v1.MODEL)
    data["production"]["cosmeticCubeCount"] = cube_count
    data["builder"]["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v4.py"
    data["builder"]["command"] = ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v4.py"]
    data["qualityIntent"]["signaturePieces"] = [
        "Asymmetric left shoulder wrap flowing into a diagonal chest sash",
        "Connected shoulder-to-hip resonance mantle with progressive taper",
        "Four separated battle-coat tails carrying the diagonal rhythm into the lower body"
    ]
    data["qualityIntent"]["macroFormPlan"] = (
        "V4 removes the broad rectangular shoulder, chest-frame and skirt reads. A stepped left shoulder wrap "
        "feeds a diagonal chest sash; the open cuirass arcs around the biological chest spike; the attached mantle "
        "continues toward the hip; four narrow overlapping coat tails leave negative space; tapered three-piece "
        "greaves preserve the biological leg silhouette."
    )
    data["qualityIntent"]["gameplayReadGoal"] = (
        "At 160 px the eye should follow one diagonal from left shoulder through torso and mantle into the split "
        "lower silhouette. Large rectangles must no longer dominate the front or rear read."
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
        "concept": "Aura Sentinel — Resonance Ronin V4",
        "officialBones": v1.OFFICIAL_BONES,
        "cosmeticBones": 10,
        "cosmeticCubes": cube_count,
        "modelSha256": v1.sha256(v1.MODEL),
        "overlaySha256": v1.sha256(v1.OVERLAY),
        "resolverSha256": v1.sha256(v1.RESOLVER),
        "normalBodySha256": v1.sha256(v1.BODY),
        "shinyBodySha256": v1.sha256(v1.SHINY),
        "bodyTexelRework": "NONE",
        "visualChange": "lower-density diagonal wrap, open cuirass, separated coat tails and tapered greaves"
    }, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
