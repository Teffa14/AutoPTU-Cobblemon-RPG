#!/usr/bin/env python3
"""Resonance Ronin V14c: layered mantle arc with neck, chest and hip continuity.

V14 removed V13's oversized rectangular shoulder mass. V14b improved continuity but
its exact matched-camera Blockbench evidence reached pixelDifferenceRatio=0.0790,
still below the unchanged 0.0800 floor. V14c keeps the same mantle silhouette and
turns the almost hairline chest ribbon into a three-segment wrapped sash with tapered
visual cadence around the official chest spike. No threshold is relaxed and no broad
slab is introduced. Official Lucario anatomy remains exact and ordered.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V13_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v13.py"
spec = importlib.util.spec_from_file_location("resonance_v13", V13_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V13 builder")
v13 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v13)
v1 = v13.v1
mcube = v13.mcube
write_overlay = v13.write_overlay


def v14_bones() -> list[dict]:
    cowl = v1.bone("ouros_resonance_cowl", "head_angle", [0, 37.0, -1.2], [
        mcube((-4.45, 35.55, -3.86), (1.45, 3.75, .34), 82, light=85, dark=80,
              pivot=(-3.55, 37.35, -3.69), rotation=(-6, -11, 21)),
        mcube((-3.45, 38.45, -2.35), (2.55, .62, 1.35), 84, light=85, dark=80,
              pivot=(-2.2, 38.75, -1.68), rotation=(-4, -8, 14)),
        mcube((3.0, 35.85, -3.82), (1.25, 3.25, .32), 82, light=85, dark=80,
              pivot=(3.55, 37.35, -3.66), rotation=(-5, 9, -17)),
        mcube((1.0, 38.45, -2.30), (2.35, .56, 1.25), 84, light=85, dark=80,
              pivot=(2.15, 38.72, -1.67), rotation=(-4, 7, -11)),
    ])

    high_collar = v1.bone("ouros_resonance_high_collar", "neck", [0, 32.2, .45], [
        mcube((-4.05, 30.85, .55), (2.25, 3.55, .52), 84, light=85, dark=80,
              pivot=(-2.95, 32.35, .80), rotation=(-9, -13, 24)),
        mcube((1.65, 31.25, .72), (1.85, 2.95, .48), 84, light=85, dark=80,
              pivot=(2.45, 32.55, .95), rotation=(-8, 11, -18)),
    ])

    shoulder_root = v1.bone("ouros_resonance_shoulder_root", "shoulder_left", [7.5, 30.4, .7], [
        mcube((5.15, 29.2, -.15), (3.55, 2.35, 2.75), 82, light=83, dark=81,
              pivot=(6.95, 30.35, 1.15), rotation=(8, -15, -22)),
        mcube((6.65, 27.55, 1.35), (3.35, 3.4, .58), 81, light=83, dark=88,
              pivot=(7.55, 29.7, 1.65), rotation=(-13, -18, -27)),
    ])

    mantle_arc = v1.bone("ouros_resonance_mantle_arc", "torso3", [0, 26.8, 2.1], [
        mcube((3.45, 25.3, 2.0), (4.9, 5.35, .50), 81, light=83, dark=88,
              pivot=(5.6, 28.2, 2.25), rotation=(-17, -16, -24)),
        mcube((1.0, 21.8, 2.35), (5.35, 5.1, .56), 82, light=83, dark=81,
              pivot=(3.9, 25.3, 2.63), rotation=(-21, -5, -17)),
        mcube((-1.35, 18.2, 2.50), (5.4, 4.9, .66), 81, light=83, dark=88,
              pivot=(1.6, 21.65, 2.82), rotation=(-22, 9, -6)),
        mcube((-3.55, 15.05, 2.15), (5.15, 4.25, .82), 82, light=83, dark=81,
              pivot=(-.75, 18.2, 2.55), rotation=(-17, 16, 9)),
        mcube((-4.75, 12.65, 1.75), (4.4, 3.35, .90), 81, light=85, dark=88,
              pivot=(-2.25, 15.1, 2.20), rotation=(-12, 19, 18)),
    ])

    hip_knot = v1.bone("ouros_resonance_hip_knot", "torso", [0, 15.0, 1.4], [
        mcube((-5.05, 13.95, .05), (3.15, 1.25, 1.85), 82, light=83, dark=81,
              pivot=(-3.45, 14.65, .95), rotation=(7, 14, 18)),
        mcube((-4.15, 11.45, .55), (1.35, 3.05, .48), 84, light=85, dark=80,
              pivot=(-3.45, 13.25, .78), rotation=(-13, 12, 21)),
    ])

    tails = v1.bone("ouros_resonance_tails", "torso", [0, 15.0, 2.7], [
        mcube((-5.6, 7.0, 2.65), (3.25, 7.7, .42), 82, light=85, dark=80,
              pivot=(-3.6, 13.3, 2.86), rotation=(-26, 11, 17)),
        mcube((.55, 9.8, 2.78), (2.35, 4.75, .34), 84, light=85, dark=80,
              pivot=(1.65, 13.5, 2.95), rotation=(-22, -8, -13)),
    ])

    # V14c front continuity. Three short overlapping segments form one diagonal
    # sash. Their changing width/angle creates a wrap rather than a straight bar,
    # while the centre remains offset from Lucario's chest spike.
    chest_ribbon = v1.bone("ouros_resonance_chest_ribbon", "torso3", [0, 27.0, -3.65], [
        mcube((-4.35, 27.1, -4.16), (3.75, .82, .24), 84, light=85, dark=80,
              pivot=(-2.45, 27.50, -4.04), rotation=(0, 0, -34)),
        mcube((-1.55, 25.25, -4.10), (3.45, .76, .24), 82, light=85, dark=81,
              pivot=(.10, 25.62, -3.98), rotation=(0, 0, -27)),
        mcube((1.10, 23.70, -4.00), (3.05, .66, .22), 84, light=85, dark=80,
              pivot=(2.55, 24.02, -3.89), rotation=(0, 0, -20)),
    ])

    left_vambrace = v1.bone("ouros_resonance_left_vambrace", "arm_left2", [10.3, 29.0, -.4], [
        mcube((9.05, 27.7, -2.08), (2.25, 2.25, .23), 80, light=82, dark=88,
              pivot=(10.2, 28.8, -1.96), rotation=(2, -8, -12)),
    ])
    right_vambrace = v1.bone("ouros_resonance_right_vambrace", "arm_right2", [-10.3, 29.0, -.4], [
        mcube((-11.15, 28.15, -2.05), (1.75, 1.7, .21), 80, light=82, dark=88,
              pivot=(-10.28, 29.0, -1.95), rotation=(2, 4, 8)),
    ])
    left_greave = v1.bone("ouros_resonance_left_greave", "leg_left4", [3.45, 5.2, -1.45], [
        mcube((2.15, -.15, -1.96), (2.3, 4.25, .24), 80, light=82, dark=88,
              pivot=(3.3, 2.0, -1.84), rotation=(-10, 2, -7)),
    ])
    right_greave = v1.bone("ouros_resonance_right_greave", "leg_right4", [-3.45, 5.2, -1.45], [
        mcube((-4.45, 0.0, -1.94), (2.15, 4.05, .23), 80, light=82, dark=88,
              pivot=(-3.35, 2.0, -1.83), rotation=(-9, -2, 6)),
    ])

    return [cowl, high_collar, shoulder_root, mantle_arc, hip_knot, tails,
            chest_ribbon, left_vambrace, right_vambrace, left_greave, right_greave]


def build_model() -> int:
    v13.build_model()
    data = json.loads(v1.MODEL.read_text(encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    official = geo["bones"][:v1.OFFICIAL_BONES]
    if len(official) != v1.OFFICIAL_BONES:
        raise SystemExit("official Lucario bone prefix missing")
    extras = v14_bones()
    geo["bones"] = official + extras
    v1.MODEL.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    return sum(len(b.get("cubes", [])) for b in extras)


def patch_manifest(cubes: int) -> None:
    data = json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["artStatus"] = "ARTISTIC FAIL"
    data["ownerApproval"] = {"required": True, "approved": False, "approvedHeadSha": None,
                              "evidenceSetSha256": None, "approvalRecord": None}
    data["production"]["modelSha256"] = v1.sha256(v1.MODEL)
    data["production"]["productionBoneCount"] = v1.OFFICIAL_BONES + 11
    data["production"]["cosmeticBoneCount"] = 11
    data["production"]["cosmeticCubeCount"] = cubes
    overlay = next(t for t in data["production"]["textures"] if t["role"] == "OVERLAY")
    overlay["sha256"] = v1.sha256(v1.OVERLAY)
    for asset in data["production"].get("runtimeAssets", []):
        if asset.get("role") == "RESOLVER": asset["sha256"] = v1.sha256(v1.RESOLVER)
    data["builder"]["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v14.py"
    data["builder"]["command"] = ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v14.py"]
    data["qualityIntent"]["signaturePieces"] = [
        "Layered shoulder-to-hip mantle arc built from narrow overlapping contact/drape forms rather than one broad pauldron",
        "High asymmetrical collar petals that hand the open cowl into the shoulder arc without enclosing Lucario's face",
        "Three-segment diagonal chest sash and compact hip knot completing a continuous front-to-back ceremonial wrap",
    ]
    data["qualityIntent"]["macroFormPlan"] = (
        "V14c retains the V14b neck-to-hip mantle architecture. The only geometry change is that the prior near-hairline two-piece chest ribbon becomes a three-segment wrapped sash with changing widths and angles around the chest spike. This strengthens front/3-quarter continuity without enlarging the shoulder silhouette or changing any technical threshold."
    )
    data["qualityIntent"]["gameplayReadGoal"] = (
        "At 160 px the first read should be one continuous asymmetric ceremonial wrap from cowl/collar through shoulder/back and visibly across the front as a diagonal sash before terminating at the hip and unequal tails. Biological landmarks remain unobstructed."
    )
    data["qualityIntent"]["iterationNote"] = (
        "Exact V14b review on head c46f9e6bdafeb6c2236d65cdb8a6973fcbdbb0a7 improved the visual delta to 0.0790 but still failed the unchanged 0.0800 floor. V14c strengthens the authored front sash rather than lowering the floor or adding slab volume. Owner approval remains absent."
    )
    v1.MANIFEST.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(); parser.add_argument("--bootstrap", action="store_true"); args = parser.parse_args()
    if v1.sha256(v1.BODY) != v1.OFFICIAL_NORMAL_SHA256: raise SystemExit("normal body texture drifted from official Lucario")
    if v1.sha256(v1.SHINY) != v1.OFFICIAL_SHINY_SHA256: raise SystemExit("shiny body texture drifted from official Lucario")
    cubes = build_model(); write_overlay(v1.OVERLAY); v1.build_resolver()
    if args.bootstrap: patch_manifest(cubes)
    print(json.dumps({"status":"BUILT","concept":"Aura Sentinel — Resonance Ronin V14c",
        "officialBones":v1.OFFICIAL_BONES,"cosmeticBones":11,"cosmeticCubes":cubes,
        "modelSha256":v1.sha256(v1.MODEL),"overlaySha256":v1.sha256(v1.OVERLAY),
        "resolverSha256":v1.sha256(v1.RESOLVER),"normalBodySha256":v1.sha256(v1.BODY),
        "shinyBodySha256":v1.sha256(v1.SHINY),"bodyTexelRework":"NONE",
        "visualChange":"layered mantle arc plus three-segment wrapped chest sash; unchanged thresholds"}, indent=2))

if __name__ == "__main__": main()
