#!/usr/bin/env python3
"""Resonance Ronin V18: camera-near crescent mantle and long coat tail.

V17 reproduced cleanly but failed the unchanged matched-camera visual floor at
pixelDifferenceRatio=0.0759 and direct Blockbench review still read as Lucario
plus small garment pieces. V18 removes the legacy chest ribbon and concentrates
visual mass into one shoulder-rooted asymmetric mantle with a continuous sloped
outer contour, layered back overlap, and one long tapered coat tail. The system
is intentionally visible in the official matched 3/4 camera without becoming a
rectangular frame or repeated-bar scaffold.

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
    "ouros_resonance_left_vambrace",
    "ouros_resonance_right_vambrace",
}


def v18_bones() -> list[dict]:
    retained = [b for b in v14.v14_bones() if b["name"] in RETAINED]
    if {b["name"] for b in retained} != RETAINED:
        raise SystemExit("V14c retained-bone contract drifted")

    # Signature shoulder crescent. Four nested shallow facets create one sloped
    # contour, not four parallel bars. The silhouette broadens near the shoulder
    # then narrows as it descends toward the ribcage.
    shoulder_crescent = v1.bone("ouros_resonance_shoulder_crescent", "shoulder_right", [-7.2, 30.0, -1.8], [
        mcube((-10.55, 28.40, -3.55), (5.15, 2.55, 1.05), 81, light=85, dark=80,
              pivot=(-7.25, 30.05, -2.85), rotation=(-10, 18, 38)),
        mcube((-11.55, 25.75, -3.35), (4.45, 3.65, .62), 82, light=84, dark=81,
              pivot=(-8.15, 28.65, -3.00), rotation=(-7, 24, 27)),
        mcube((-10.85, 22.80, -3.12), (3.65, 4.15, .48), 81, light=85, dark=80,
              pivot=(-8.25, 26.15, -2.90), rotation=(-3, 18, 16)),
        mcube((-9.20, 20.55, -2.92), (2.75, 3.55, .40), 84, light=85, dark=81,
              pivot=(-7.45, 23.45, -2.72), rotation=(0, 10, 7)),
    ])

    # Front lapel is a single diagonal mass that visually connects shoulder to
    # waist while keeping the chest spike and cream center exposed.
    front_lapel = v1.bone("ouros_resonance_front_lapel", "torso3", [-3.6, 23.0, -2.7], [
        mcube((-7.45, 20.05, -3.38), (4.55, 3.15, .44), 82, light=85, dark=81,
              pivot=(-5.05, 22.10, -3.16), rotation=(-2, 8, -19)),
        mcube((-6.25, 17.35, -3.18), (3.55, 3.45, .38), 81, light=84, dark=80,
              pivot=(-4.45, 19.65, -2.99), rotation=(1, 4, -10)),
    ])

    # Back mantle overlaps the shoulder crescent and shifts laterally as it falls.
    # Three facets create depth and a curved diagonal edge around the back rather
    # than a centered rectangular plate.
    back_mantle = v1.bone("ouros_resonance_back_mantle", "torso3", [-4.5, 23.0, 2.0], [
        mcube((-9.35, 20.80, 2.15), (5.45, 5.85, .52), 81, light=83, dark=80,
              pivot=(-6.25, 24.10, 2.45), rotation=(7, -14, 17)),
        mcube((-8.15, 16.10, 2.22), (4.35, 5.70, .44), 82, light=85, dark=81,
              pivot=(-5.65, 19.85, 2.48), rotation=(4, -9, 7)),
        mcube((-6.85, 12.35, 2.05), (3.25, 4.70, .36), 84, light=85, dark=80,
              pivot=(-5.05, 15.85, 2.25), rotation=(1, 2, -5)),
    ])

    # Long coat tail is the lower continuation of the same material envelope.
    # It stays on the near side and tapers downward; a shorter counter-tail keeps
    # negative space around the biological tail and both legs.
    coat_tails = v1.bone("ouros_resonance_coat_tails", "torso", [-3.8, 14.0, -1.6], [
        mcube((-8.10, 9.20, -2.95), (3.75, 6.45, .40), 82, light=85, dark=81,
              pivot=(-5.95, 14.10, -2.75), rotation=(-5, 6, -13)),
        mcube((-7.15, 4.75, -2.72), (2.85, 5.60, .34), 81, light=84, dark=80,
              pivot=(-5.45, 9.50, -2.55), rotation=(-3, 1, -7)),
        mcube((1.85, 10.60, -2.60), (2.15, 4.15, .32), 84, light=85, dark=80,
              pivot=(2.80, 13.65, -2.44), rotation=(-4, 5, 12)),
    ])

    # Compact clasp supplies an explicit contact transition into the waist.
    waist_clasp = v1.bone("ouros_resonance_waist_clasp", "torso", [-3.5, 14.8, -2.3], [
        mcube((-5.75, 13.55, -3.08), (2.45, 1.30, .84), 84, light=85, dark=80,
              pivot=(-4.45, 14.25, -2.64), rotation=(2, 6, -11)),
    ])

    return retained + [shoulder_crescent, front_lapel, back_mantle, coat_tails, waist_clasp]


def build_model() -> int:
    v14.build_model()
    data = json.loads(v1.MODEL.read_text(encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    official = geo["bones"][:v1.OFFICIAL_BONES]
    if len(official) != v1.OFFICIAL_BONES:
        raise SystemExit("official Lucario bone prefix missing")
    extras = v18_bones()
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
    data["production"]["productionBoneCount"] = v1.OFFICIAL_BONES + 9
    data["production"]["cosmeticBoneCount"] = 9
    data["production"]["cosmeticCubeCount"] = cubes
    next(t for t in data["production"]["textures"] if t["role"] == "OVERLAY")["sha256"] = v1.sha256(v1.OVERLAY)
    for asset in data["production"].get("runtimeAssets", []):
        if asset.get("role") == "RESOLVER":
            asset["sha256"] = v1.sha256(v1.RESOLVER)
    data["builder"]["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v18.py"
    data["builder"]["command"] = ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v18.py"]
    data["qualityIntent"]["signaturePieces"] = [
        "Broad-to-narrow four-facet shoulder crescent forming one sloped camera-near contour",
        "Connected diagonal front/back mantle envelope that frames rather than covers the chest spike",
        "One long tapered near-side coat tail plus short counter-tail preserving negative space around biological tail and legs",
    ]
    data["qualityIntent"]["macroFormPlan"] = (
        "V18 responds to V17's exact-head Blockbench and 0.0759 pixel-difference failure by removing the legacy chest ribbon and concentrating the transformation into one shoulder-rooted mantle envelope. The near-side crescent broadens at the shoulder, narrows through the ribcage, overlaps a diagonal back shell and terminates in an unequal long coat tail. There are no greaves, banners, portal frames, repeated bars or detached equipment islands."
    )
    data["qualityIntent"]["gameplayReadGoal"] = (
        "At 160 px the first read should be an unmistakable asymmetric ronin mantle with a sloped shoulder silhouette and long diagonal coat tail, while Lucario's face, ears, aura sensors, chest spike, hands, feet and biological tail remain immediately readable."
    )
    data["qualityIntent"]["iterationNote"] = (
        "V17 exact-head review on 65fb0ebbec05ac11eee085422fc87b76feea360d reproduced the exact 87 official bones plus 10 cosmetics and passed attachment, but failed the unchanged Blockbench pixel-difference floor at 0.0759 vs 0.0800. Direct contact-sheet review also remained too close to base Lucario. V18 changes silhouette architecture rather than lowering thresholds."
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
        "status":"BUILT", "concept":"Aura Sentinel — Resonance Ronin V18",
        "officialBones":v1.OFFICIAL_BONES, "cosmeticBones":9, "cosmeticCubes":cubes,
        "modelSha256":v1.sha256(v1.MODEL), "overlaySha256":v1.sha256(v1.OVERLAY),
        "bodyTexelRework":"NONE",
        "visualChange":"camera-near crescent mantle + long tapered coat tail; legacy chest ribbon removed; unchanged floors",
    }, indent=2))


if __name__ == "__main__": main()
