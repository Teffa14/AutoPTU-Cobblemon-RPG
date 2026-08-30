#!/usr/bin/env python3
"""Resonance Ronin V17: continuous shoulder-torso-hip contour mantle.

V16b cleared the technical visual floor but remained ARTISTIC FAIL after direct
Blockbench review: the change concentrated on one arm/flank and still read as
Lucario plus hanging equipment. V17 replaces that architecture with one coherent
asymmetric mantle envelope that wraps from the camera-near shoulder across chest,
back and hip. Large surfaces overlap with changing angle/width; isolated greaves
and long banner-like strips stay removed.

Presentation only. AutoPTU/Ouros remains authoritative for combatants, legality,
HP/status, positions, RNG, damage and tactical outcomes.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V14_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v14.py"
spec = importlib.util.spec_from_file_location("resonance_v14", V14_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load V14c builder")
v14 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v14)
v1 = v14.v1
mcube = v14.mcube
write_overlay = v14.write_overlay

RETAINED = {
    "ouros_resonance_cowl",
    "ouros_resonance_high_collar",
    "ouros_resonance_chest_ribbon",
    "ouros_resonance_left_vambrace",
    "ouros_resonance_right_vambrace",
}


def v17_bones() -> list[dict]:
    retained = [b for b in v14.v14_bones() if b["name"] in RETAINED]
    if {b["name"] for b in retained} != RETAINED:
        raise SystemExit("V14c retained-bone contract drifted")

    # Primary signature: three nested shoulder shells, each rotated differently,
    # forming one descending crescent instead of a rectangular pauldron stack.
    shoulder_crescent = v1.bone("ouros_resonance_shoulder_crescent", "shoulder_right", [-6.8, 30.0, -1.4], [
        mcube((-9.25, 28.85, -3.10), (4.25, 2.15, 1.05), 81, light=85, dark=80,
              pivot=(-7.05, 30.05, -2.45), rotation=(-12, 18, 34)),
        mcube((-10.05, 26.70, -2.96), (3.45, 3.05, .62), 82, light=84, dark=81,
              pivot=(-7.75, 28.85, -2.62), rotation=(-7, 23, 24)),
        mcube((-9.55, 24.95, -2.78), (2.65, 2.85, .46), 84, light=85, dark=80,
              pivot=(-7.75, 27.05, -2.55), rotation=(-2, 18, 15)),
    ])

    # Chest wrap crosses the biological torso diagonally but leaves the chest spike
    # and central cream anatomy open. Two shallow planes create front/back overlap.
    torso_wrap = v1.bone("ouros_resonance_torso_wrap", "torso3", [-2.8, 23.5, -2.4], [
        mcube((-7.15, 21.05, -3.18), (5.25, 3.05, .42), 82, light=85, dark=81,
              pivot=(-4.55, 22.75, -2.96), rotation=(-2, 8, -18)),
        mcube((-6.25, 18.65, -2.98), (4.25, 3.20, .36), 81, light=84, dark=80,
              pivot=(-4.15, 20.75, -2.80), rotation=(1, 4, -10)),
        mcube((-5.95, 19.45, 2.15), (3.85, 3.55, .34), 82, light=84, dark=81,
              pivot=(-4.15, 21.35, 2.32), rotation=(2, -5, -8)),
    ])

    # Back mantle bridges shoulder into hip with three overlapping facets that
    # narrow as they descend. Their offset creates negative space near the tail.
    back_mantle = v1.bone("ouros_resonance_back_mantle", "torso3", [-3.8, 23.5, 2.0], [
        mcube((-8.20, 21.20, 2.25), (4.65, 5.65, .48), 81, light=83, dark=80,
              pivot=(-5.55, 24.25, 2.50), rotation=(7, -13, 14)),
        mcube((-6.95, 16.70, 2.35), (3.75, 5.45, .42), 82, light=85, dark=81,
              pivot=(-4.85, 20.05, 2.58), rotation=(4, -8, 5)),
        mcube((-5.65, 13.10, 2.18), (2.75, 4.65, .36), 84, light=85, dark=80,
              pivot=(-4.15, 16.55, 2.38), rotation=(1, 4, -7)),
    ])

    # Hip petals complete the silhouette around the waist without forming shorts.
    # Unequal left/right lengths keep the composition asymmetric and tail-clear.
    hip_petals = v1.bone("ouros_resonance_hip_petals", "torso", [-2.8, 14.0, -1.0], [
        mcube((-6.75, 11.05, -2.72), (3.25, 4.55, .38), 82, light=85, dark=81,
              pivot=(-4.95, 14.20, -2.53), rotation=(-4, 4, -15)),
        mcube((-4.15, 9.30, -2.58), (2.35, 4.85, .34), 81, light=84, dark=80,
              pivot=(-3.00, 13.05, -2.40), rotation=(-2, -3, 8)),
        mcube((1.95, 11.25, -2.48), (2.05, 3.65, .32), 84, light=85, dark=80,
              pivot=(2.85, 13.75, -2.32), rotation=(-4, 5, 13)),
    ])

    # A compact waist clasp is the contact/root transition, not a floating ornament.
    waist_clasp = v1.bone("ouros_resonance_waist_clasp", "torso", [-3.2, 14.8, -2.25], [
        mcube((-5.35, 13.65, -3.05), (2.25, 1.25, .82), 84, light=85, dark=80,
              pivot=(-4.15, 14.30, -2.62), rotation=(2, 6, -10)),
    ])

    return retained + [shoulder_crescent, torso_wrap, back_mantle, hip_petals, waist_clasp]


def build_model() -> int:
    v14.build_model()
    data = json.loads(v1.MODEL.read_text(encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    official = geo["bones"][:v1.OFFICIAL_BONES]
    if len(official) != v1.OFFICIAL_BONES:
        raise SystemExit("official Lucario bone prefix missing")
    extras = v17_bones()
    geo["bones"] = official + extras
    v1.MODEL.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    return sum(len(b.get("cubes", [])) for b in extras)


def patch_manifest(cubes: int) -> None:
    data = json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["artStatus"] = "ARTISTIC FAIL"
    data["ownerApproval"] = {
        "required": True, "approved": False, "approvedHeadSha": None,
        "evidenceSetSha256": None, "approvalRecord": None,
    }
    data["production"]["modelSha256"] = v1.sha256(v1.MODEL)
    data["production"]["productionBoneCount"] = v1.OFFICIAL_BONES + 10
    data["production"]["cosmeticBoneCount"] = 10
    data["production"]["cosmeticCubeCount"] = cubes
    next(t for t in data["production"]["textures"] if t["role"] == "OVERLAY")["sha256"] = v1.sha256(v1.OVERLAY)
    for asset in data["production"].get("runtimeAssets", []):
        if asset.get("role") == "RESOLVER":
            asset["sha256"] = v1.sha256(v1.RESOLVER)
    data["builder"]["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v17.py"
    data["builder"]["command"] = ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v17.py"]
    data["qualityIntent"]["signaturePieces"] = [
        "Three-stage camera-near shoulder crescent descending into the torso rather than projecting as a slab",
        "Continuous diagonal front/back mantle wrap that leaves the chest spike and biological center open",
        "Asymmetric tapered hip petals completing the silhouette while preserving negative space around tail and legs",
    ]
    data["qualityIntent"]["macroFormPlan"] = (
        "V17 replaces V16b's arm/flank-dominant hanging equipment read with one connected shoulder-torso-back-hip envelope. Rotated overlapping facets change width and direction as they descend, while the front stays open around Lucario's chest spike. The lower silhouette uses three unequal hip petals rather than shorts, greaves, banners or repeated bars."
    )
    data["qualityIntent"]["gameplayReadGoal"] = (
        "At 160 px the first read should be one asymmetric ronin mantle wrapping Lucario's upper body and breaking into a tapered hip silhouette, with the face, ears, aura sensors, chest spike, hands, feet and tail unmistakable."
    )
    data["qualityIntent"]["iterationNote"] = (
        "V16b exact-head Blockbench review on 65b0eacf40791a03208f5cb84174ffd78329bbdd passed technical floors (pixelDifferenceRatio=0.118063, silhouetteDeltaRatio=0.066621) but failed internal art QA: the silhouette change was concentrated on one arm/flank and still read as Lucario plus hanging equipment. V17 changes macroform architecture without relaxing thresholds or modifying official biology."
    )
    v1.MANIFEST.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(); parser.add_argument("--bootstrap", action="store_true"); args = parser.parse_args()
    if v1.sha256(v1.BODY) != v1.OFFICIAL_NORMAL_SHA256:
        raise SystemExit("normal body texture drifted")
    if v1.sha256(v1.SHINY) != v1.OFFICIAL_SHINY_SHA256:
        raise SystemExit("shiny body texture drifted")
    cubes = build_model(); write_overlay(v1.OVERLAY); v1.build_resolver()
    if args.bootstrap: patch_manifest(cubes)
    print(json.dumps({
        "status":"BUILT", "concept":"Aura Sentinel — Resonance Ronin V17",
        "officialBones":v1.OFFICIAL_BONES, "cosmeticBones":10, "cosmeticCubes":cubes,
        "modelSha256":v1.sha256(v1.MODEL), "overlaySha256":v1.sha256(v1.OVERLAY),
        "bodyTexelRework":"NONE",
        "visualChange":"continuous shoulder-torso-back-hip contour mantle; tapered hip petals; unchanged floors",
    }, indent=2))


if __name__ == "__main__": main()
