#!/usr/bin/env python3
"""Rework Resonance Ronin V2 after exact Blockbench QA.

V2 removed the old shrine-frame language but its tall dorsal pennant still read
as an isolated vertical prop in front/battle views. V3 replaces only that Ouros
signature system with a connected diagonal mantle rooted at the left scapula and
sweeping toward the hip. The exact 87-bone official Lucario prefix remains
untouched. This builder is presentation-only; AutoPTU/Ouros remains authoritative
for battle facts.
"""

from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V2_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v2.py"

spec = importlib.util.spec_from_file_location("resonance_v2", V2_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V2 builder")
v2 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v2)
v1 = v2.v1


def resonance_mantle() -> dict:
    """One attached shoulder-to-hip macro-form with progressive taper and overlap."""
    return v1.bone("ouros_resonance_mantle", "torso3", [-4.0, 30.4, 2.7], [
        # Contact root: broad enough to visibly disappear into the shawl/scapula.
        v1.cube((-7.0, 28.75, 1.95), (6.2, 2.75, 1.55), 80,
                pivot=(-3.9, 30.15, 2.72), rotation=(2, -8, -9)),
        # Overlapping cloth/lamellar planes sweep down and outward. Dimensions
        # shrink progressively so the silhouette tapers instead of forming a slab.
        v1.cube((-8.8, 26.25, 2.62), (6.0, 3.85, 1.05), 81,
                pivot=(-5.7, 28.65, 3.12), rotation=(7, -12, -16)),
        v1.cube((-10.25, 23.25, 2.85), (5.45, 4.15, 0.88), 82,
                pivot=(-7.25, 26.05, 3.28), rotation=(9, -14, -22)),
        v1.cube((-11.05, 20.25, 2.90), (4.75, 4.00, 0.72), 81,
                pivot=(-8.35, 23.15, 3.26), rotation=(10, -15, -27)),
        v1.cube((-11.05, 17.65, 2.82), (3.80, 3.45, 0.58), 80,
                pivot=(-8.85, 20.45, 3.10), rotation=(10, -16, -31)),
        # A small inset plate breaks material without creating another detached bar.
        v1.cube((-7.35, 27.25, 1.72), (2.25, 1.65, 0.42), 86,
                pivot=(-6.2, 28.1, 1.93), rotation=(3, -8, -14)),
        # Scapular clasp closes the contact root and carries the gold hierarchy.
        v1.cube((-5.85, 29.65, 1.40), (2.35, 1.15, 0.62), 84,
                pivot=(-4.7, 30.2, 1.72), rotation=(0, -7, -10)),
    ])


def build_model() -> int:
    v2.build_model()
    data = json.loads(v1.MODEL.read_text(encoding="utf-8"))
    bones = data["minecraft:geometry"][0]["bones"]
    found = False
    for index, entry in enumerate(bones):
        if entry.get("name") == "ouros_resonance_banner":
            bones[index] = resonance_mantle()
            found = True
            break
    if not found:
        raise SystemExit("generated V2 model has no ouros_resonance_banner")
    v1.MODEL.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    extras = bones[v1.OFFICIAL_BONES:]
    return sum(len(entry.get("cubes", [])) for entry in extras)


def patch_manifest(cube_count: int) -> None:
    data = json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["production"]["modelSha256"] = v1.sha256(v1.MODEL)
    data["production"]["cosmeticCubeCount"] = cube_count
    data["builder"]["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v3.py"
    data["builder"]["command"] = ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v3.py"]
    data["qualityIntent"]["signaturePieces"] = [
        "Asymmetric resonance shawl wrapping both shoulders into the torso silhouette",
        "Connected diagonal resonance mantle rooted at the left scapula and tapering toward the hip",
        "Split battle coat that transforms the lower body in motion"
    ]
    data["qualityIntent"]["macroFormPlan"] = (
        "A low open circlet, broad asymmetric shoulder shawl, tapered chest shell, connected shoulder-to-hip "
        "resonance mantle, split coat tails, plated forearms and plated shins create a diagonal full-body "
        "composition. The mantle uses overlapping progressively smaller planes and a visible scapular contact "
        "root rather than a pole, portal, backpack frame or detached dorsal island."
    )
    data["qualityIntent"]["gameplayReadGoal"] = (
        "At 160 px the left-heavy shawl and shoulder-to-hip mantle must form one diagonal silhouette, while the "
        "split coat and cyan/gold material accents preserve the ronin read without relying on micro-detail."
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
        patch_manifest(cube_count)

    print(json.dumps({
        "status": "BUILT",
        "concept": "Aura Sentinel — Resonance Ronin V3",
        "officialBones": v1.OFFICIAL_BONES,
        "cosmeticBones": 10,
        "cosmeticCubes": cube_count,
        "modelSha256": v1.sha256(v1.MODEL),
        "overlaySha256": v1.sha256(v1.OVERLAY),
        "resolverSha256": v1.sha256(v1.RESOLVER),
        "normalBodySha256": v1.sha256(v1.BODY),
        "shinyBodySha256": v1.sha256(v1.SHINY),
        "bodyTexelRework": "NONE",
        "visualChange": "replaced vertical dorsal pennant with connected tapered shoulder-to-hip resonance mantle"
    }, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
