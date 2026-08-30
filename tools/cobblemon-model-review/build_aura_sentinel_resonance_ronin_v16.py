#!/usr/bin/env python3
"""Resonance Ronin V16: replace the legacy panel/ribbon stack with one mantle flow.

V15c remained ARTISTIC FAIL because it retained V14c's five-panel rear mantle and
only changed the exterior ribbons. V16 removes that inherited architecture. It
keeps the useful face/neck/extremity accents, then authors three overlapping,
compound-rotated mantle facets that travel continuously from a real shoulder
contact across the back into one asymmetric hip fold. The system deliberately
uses fewer, differently proportioned surfaces with stronger overlap and taper.

This builder is presentation-only. AutoPTU/Ouros remains authoritative for
combatants, legality, HP/status, positions, RNG, damage and tactical outcomes.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V15_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v15.py"
spec = importlib.util.spec_from_file_location("resonance_v15", V15_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load V15c builder")
v15 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v15)
v14 = v15.v14
v1 = v15.v1
mcube = v15.mcube
write_overlay = v15.write_overlay

RETAINED = {
    "ouros_resonance_cowl",
    "ouros_resonance_high_collar",
    "ouros_resonance_chest_ribbon",
    "ouros_resonance_left_vambrace",
    "ouros_resonance_right_vambrace",
    "ouros_resonance_left_greave",
    "ouros_resonance_right_greave",
}


def v16_bones() -> list[dict]:
    retained = [b for b in v14.v14_bones() if b["name"] in RETAINED]
    if {b["name"] for b in retained} != RETAINED:
        raise SystemExit("V14c retained-bone contract drifted")

    mantle_root = v1.bone("ouros_resonance_mantle_root", "shoulder_left", [4.9, 30.2, 0.5], [
        mcube((3.15, 28.45, 0.25), (3.25, 3.05, 1.25), 81, light=83, dark=80,
              pivot=(4.55, 30.0, .82), rotation=(-12, -18, -24)),
        mcube((4.85, 27.25, 1.0), (3.15, 3.75, .62), 82, light=85, dark=81,
              pivot=(5.65, 29.25, 1.3), rotation=(-19, -25, -31)),
    ])

    mantle_spine = v1.bone("ouros_resonance_mantle_spine", "torso3", [0, 25.4, 2.25], [
        mcube((2.45, 24.15, 1.55), (5.35, 6.35, .72), 82, light=83, dark=81,
              pivot=(4.8, 27.45, 1.9), rotation=(-23, -18, -28)),
        mcube((-1.05, 18.75, 2.0), (6.45, 8.15, .66), 81, light=83, dark=80,
              pivot=(2.15, 23.0, 2.35), rotation=(-28, 7, -13)),
        mcube((-4.45, 13.35, 1.72), (5.25, 7.15, .48), 86, light=85, dark=81,
              pivot=(-1.35, 17.6, 1.96), rotation=(-22, 19, 11)),
    ])

    hip_fold = v1.bone("ouros_resonance_hip_fold", "torso", [0, 15.1, 1.35], [
        mcube((-5.15, 13.25, .15), (3.45, 1.55, 1.65), 84, light=85, dark=80,
              pivot=(-3.35, 14.15, .95), rotation=(8, 16, 19)),
        mcube((-5.35, 6.65, 1.35), (2.55, 7.45, .38), 82, light=85, dark=81,
              pivot=(-3.95, 12.25, 1.55), rotation=(-31, 16, 23)),
    ])
    return retained + [mantle_root, mantle_spine, hip_fold]


def build_model() -> int:
    v14.build_model()
    data = json.loads(v1.MODEL.read_text(encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    official = geo["bones"][:v1.OFFICIAL_BONES]
    if len(official) != v1.OFFICIAL_BONES:
        raise SystemExit("official Lucario bone prefix missing")
    extras = v16_bones()
    geo["bones"] = official + extras
    v1.MODEL.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    return sum(len(b.get("cubes", [])) for b in extras)


def patch_manifest(cubes: int) -> None:
    data = json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["artStatus"] = "ARTISTIC FAIL"
    data["ownerApproval"] = {
        "required": True,
        "approved": False,
        "approvedHeadSha": None,
        "evidenceSetSha256": None,
        "approvalRecord": None,
    }
    data["production"]["modelSha256"] = v1.sha256(v1.MODEL)
    data["production"]["productionBoneCount"] = v1.OFFICIAL_BONES + 10
    data["production"]["cosmeticBoneCount"] = 10
    data["production"]["cosmeticCubeCount"] = cubes
    next(t for t in data["production"]["textures"] if t["role"] == "OVERLAY")["sha256"] = v1.sha256(v1.OVERLAY)
    for asset in data["production"].get("runtimeAssets", []):
        if asset.get("role") == "RESOLVER":
            asset["sha256"] = v1.sha256(v1.RESOLVER)
    data["builder"]["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v16.py"
    data["builder"]["command"] = ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v16.py"]
    data["qualityIntent"]["signaturePieces"] = [
        "Open cowl and high collar preserving Lucario's face, ears, aura sensors and chest spike",
        "Single left-shoulder-to-back mantle flow built from three deeply overlapping compound-rotated facets with changing proportions and taper",
        "Compact asymmetric hip landing with one long fold and explicit negative space around the biological tail",
    ]
    data["qualityIntent"]["macroFormPlan"] = (
        "V16 deletes V14c's five-panel mantle plus V15c's shoulder and hip ribbon add-ons. One compact official-shoulder contact feeds three differently proportioned, deeply overlapping facets that turn diagonally across the back and taper into one hip fold. The silhouette is earned through one continuous directional mass and negative space, not repeated bars, broad slabs, cages or extra cube count."
    )
    data["qualityIntent"]["gameplayReadGoal"] = (
        "At 160 px the first read should be Lucario crossed by one coherent asymmetric shoulder-back-hip mantle gesture. The outer contour must remain visible from matched 3/4 while the face, chest spike, hands, feet and biological tail stay unmistakable. Rear view must read as a tapered diagonal cloth mass rather than a stack of rectangular panels."
    )
    data["qualityIntent"]["iterationNote"] = (
        "V15c exact-head evidence remained ARTISTIC FAIL and also missed the unchanged silhouette floor at 0.0294 versus 0.0400. Code inspection showed the deeper cause: V15c inherited V14c's five-panel rear mantle and only replaced exterior ribbons. V16 removes that inherited architecture entirely while retaining the same technical floors and owner-only approval contract."
    )
    v1.MANIFEST.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--bootstrap", action="store_true")
    args = parser.parse_args()
    if v1.sha256(v1.BODY) != v1.OFFICIAL_NORMAL_SHA256:
        raise SystemExit("normal body texture drifted")
    if v1.sha256(v1.SHINY) != v1.OFFICIAL_SHINY_SHA256:
        raise SystemExit("shiny body texture drifted")
    cubes = build_model()
    write_overlay(v1.OVERLAY)
    v1.build_resolver()
    if args.bootstrap:
        patch_manifest(cubes)
    print(json.dumps({
        "status": "BUILT",
        "concept": "Aura Sentinel — Resonance Ronin V16",
        "officialBones": v1.OFFICIAL_BONES,
        "cosmeticBones": 10,
        "cosmeticCubes": cubes,
        "modelSha256": v1.sha256(v1.MODEL),
        "overlaySha256": v1.sha256(v1.OVERLAY),
        "bodyTexelRework": "NONE",
        "visualChange": "replace inherited rear panel stack with one continuous shoulder-back-hip mantle flow",
    }, indent=2))


if __name__ == "__main__":
    main()
