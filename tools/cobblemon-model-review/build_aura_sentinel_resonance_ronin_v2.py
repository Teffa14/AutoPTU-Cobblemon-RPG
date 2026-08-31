#!/usr/bin/env python3
"""Refine Aura Sentinel Resonance Ronin after first exact Blockbench review.

V1 proved the technical pipeline but the left dorsal signature read as a cluster of
small blocks from the front. This pass keeps the same new whole-body concept and
replaces only that authored Ouros group with one continuous tapered sashimono-like
resonance pennant. Official Lucario bones and biological textures remain untouched.
"""

from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V1_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin.py"

spec = importlib.util.spec_from_file_location("resonance_v1", V1_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V1 builder")
v1 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v1)


def refined_banner() -> dict:
    # A single connected taper from the left scapula upward. The overlapping
    # masses intentionally form one readable pennant rather than isolated plates.
    return v1.bone("ouros_resonance_banner", "torso3", [-4.5, 31.2, 3.3], [
        v1.cube((-6.7, 29.4, 2.65), (3.7, 4.25, 1.75), 80,
                pivot=(-4.85, 31.5, 3.52), rotation=(0, 0, -7)),
        v1.cube((-7.75, 32.7, 2.78), (3.55, 4.65, 1.55), 81,
                pivot=(-5.98, 35.0, 3.55), rotation=(0, 0, -10)),
        v1.cube((-8.7, 36.45, 2.9), (3.25, 4.45, 1.35), 86,
                pivot=(-7.08, 38.65, 3.57), rotation=(0, 0, -13)),
        v1.cube((-9.45, 40.05, 3.02), (2.85, 3.65, 1.15), 82,
                pivot=(-8.02, 41.85, 3.60), rotation=(0, 0, -16)),
        v1.cube((-9.9, 42.85, 3.12), (2.35, 2.65, 0.95), 83,
                pivot=(-8.72, 44.15, 3.59), rotation=(0, 0, -19)),
        v1.cube((-7.25, 32.95, 4.18), (2.2, 0.42, 0.26), 84,
                pivot=(-6.15, 33.16, 4.31), rotation=(0, 0, -10)),
        v1.cube((-8.05, 36.8, 4.12), (1.9, 0.40, 0.24), 85,
                pivot=(-7.1, 37.0, 4.24), rotation=(0, 0, -13)),
        v1.cube((-8.7, 40.45, 4.04), (1.55, 0.36, 0.22), 84,
                pivot=(-7.92, 40.63, 4.15), rotation=(0, 0, -16)),
        v1.cube((-9.02, 43.15, 3.94), (1.15, 0.32, 0.20), 85,
                pivot=(-8.45, 43.31, 4.04), rotation=(0, 0, -19)),
    ])


def build_model() -> int:
    _, _ = v1.build_model()
    data = json.loads(v1.MODEL.read_text(encoding="utf-8"))
    bones = data["minecraft:geometry"][0]["bones"]
    found = False
    for index, entry in enumerate(bones):
        if entry.get("name") == "ouros_resonance_banner":
            bones[index] = refined_banner()
            found = True
            break
    if not found:
        raise SystemExit("generated V1 model has no ouros_resonance_banner")
    v1.MODEL.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    extras = bones[v1.OFFICIAL_BONES:]
    return sum(len(entry.get("cubes", [])) for entry in extras)


def patch_manifest(cube_count: int) -> None:
    data = json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["production"]["modelSha256"] = v1.sha256(v1.MODEL)
    data["production"]["cosmeticCubeCount"] = cube_count
    data["builder"]["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v2.py"
    data["builder"]["command"] = ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v2.py"]
    data["qualityIntent"]["signaturePieces"][1] = "One continuous tapered dorsal resonance pennant rising from the left scapula"
    data["qualityIntent"]["macroFormPlan"] = (
        "A low open circlet, broad asymmetric shoulder shawl, tapered chest shell, one continuous tapered "
        "left dorsal pennant, split coat tails, plated forearms and plated shins create a continuous top-to-bottom "
        "silhouette without a rectangular portal, detached cluster or backpack frame."
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

    cube_count = build_model()
    v1.write_overlay(v1.OVERLAY)
    v1.build_resolver()
    if args.bootstrap:
        v1.build_manifest(cube_count)
        patch_manifest(cube_count)

    print(json.dumps({
        "status": "BUILT",
        "concept": "Aura Sentinel — Resonance Ronin V2",
        "officialBones": v1.OFFICIAL_BONES,
        "cosmeticBones": 10,
        "cosmeticCubes": cube_count,
        "modelSha256": v1.sha256(v1.MODEL),
        "overlaySha256": v1.sha256(v1.OVERLAY),
        "resolverSha256": v1.sha256(v1.RESOLVER),
        "normalBodySha256": v1.sha256(v1.BODY),
        "shinyBodySha256": v1.sha256(v1.SHINY),
        "bodyTexelRework": "NONE",
        "visualChange": "replaced detached-looking dorsal block cluster with one continuous tapered pennant"
    }, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
