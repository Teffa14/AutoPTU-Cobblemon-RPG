#!/usr/bin/env python3
"""Resonance Ronin V12: vertical tapered half-cloak after V11 artistic fail.

V11 cleared technical floors but direct Blockbench review rejected its broad
horizontal shoulder slabs. V12 preserves the exact official Lucario prefix and
replaces that macro-form with a shoulder-rooted vertical cascade that wraps
back/down around the anatomy instead of projecting as a rigid wing.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V11_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v11.py"
spec = importlib.util.spec_from_file_location("resonance_v11", V11_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V11 builder")
v11 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v11)
v1 = v11.v1
mcube = v11.mcube
write_overlay = v11.write_overlay


def v12_bones() -> list[dict]:
    cowl = v1.bone("ouros_resonance_cowl", "head_angle", [0, 37.2, -0.8], [
        mcube((-4.15, 38.0, -1.0), (8.3, 1.25, 3.15), 84, light=85, dark=80,
              pivot=(0, 38.6, .5), rotation=(-5, 0, 0)),
        mcube((-4.48, 35.15, -4.80), (1.12, 3.55, .28), 82, light=83, dark=81,
              pivot=(-3.85, 36.9, -4.66), rotation=(0, -6, 15)),
        mcube((3.35, 35.4, -4.78), (1.02, 3.15, .26), 82, light=83, dark=81,
              pivot=(3.82, 36.9, -4.65), rotation=(0, 5, -11)),
    ])

    # One half-cloak system anchored at the official left shoulder. The pieces
    # overlap vertically and progressively narrow; no horizontal bar/wing chain.
    half_cloak = v1.bone("ouros_resonance_half_cloak", "shoulder_left", [8.1, 30.6, 1.0], [
        # compact contact cap
        mcube((5.6, 28.8, -0.25), (5.4, 3.9, 4.15), 82, light=83, dark=81,
              pivot=(8.2, 30.7, 1.5), rotation=(4, -8, -12)),
        # upper drape: vertical, angled back and slightly outward
        mcube((7.15, 24.65, 2.05), (5.7, 7.0, .58), 81, light=83, dark=88,
              pivot=(8.3, 29.1, 2.34), rotation=(-14, -13, -9)),
        # middle wrap overlaps upper drape and bends toward the hip
        mcube((7.55, 18.75, 2.48), (5.25, 7.4, .52), 82, light=83, dark=81,
              pivot=(8.45, 24.4, 2.74), rotation=(-18, -9, 5)),
        # lower flare narrows and turns back toward body, creating a curved edge
        mcube((7.25, 12.9, 2.78), (4.55, 7.2, .46), 81, light=83, dark=88,
              pivot=(8.45, 18.7, 3.0), rotation=(-21, -6, 14)),
        # small terminal wedge ends the contour rather than a square slab
        mcube((6.85, 9.5, 3.02), (3.55, 4.6, .38), 84, light=85, dark=80,
              pivot=(8.25, 13.1, 3.2), rotation=(-24, -4, 21)),
    ])

    # Narrow back bridge makes the half-cloak read as garment, not an isolated
    # shoulder attachment. It tapers into two thin unequal tails.
    mantle = v1.bone("ouros_resonance_mantle", "torso3", [0, 28.7, 1.5], [
        mcube((-5.8, 26.8, 1.55), (11.8, 3.8, 1.02), 82, light=83, dark=81,
              pivot=(0, 28.9, 2.0), rotation=(-9, 0, 1)),
        mcube((-4.9, 21.8, 2.26), (10.0, 5.9, .54), 81, light=83, dark=88,
              pivot=(0, 26.7, 2.52), rotation=(-15, 0, 2)),
        mcube((-4.15, 17.0, 2.58), (8.5, 5.5, .46), 82, light=83, dark=81,
              pivot=(0, 21.7, 2.8), rotation=(-18, 0, 3)),
        mcube((-4.85, 9.55, 2.9), (4.05, 9.1, .40), 81, light=83, dark=88,
              pivot=(-2.7, 17.2, 3.1), rotation=(-22, 7, -7)),
        mcube((.95, 11.4, 2.9), (3.55, 7.2, .38), 84, light=85, dark=80,
              pivot=(2.7, 17.0, 3.1), rotation=(-20, -6, 9)),
    ])

    # Minimal front bridge. It deliberately leaves the biological chest spike,
    # cream chest mass and waist silhouette open.
    sash = v1.bone("ouros_resonance_sash", "torso3", [0, 27.0, -3.4], [
        mcube((-5.0, 27.05, -4.08), (8.6, .72, .24), 84, light=85, dark=80,
              pivot=(-.7, 27.45, -3.96), rotation=(0, 0, -31)),
        mcube((-3.35, 22.8, -3.60), (6.7, .55, .50), 84, light=85, dark=80),
    ])

    left_vambrace = v1.bone("ouros_resonance_left_vambrace", "arm_left2", [10.3,29.4,-.3], [
        mcube((8.95,27.75,-2.17),(2.7,2.65,.26),80,light=82,dark=88,
              pivot=(10.3,29.05,-2.04),rotation=(0,-5,-7)),
    ])
    right_vambrace = v1.bone("ouros_resonance_right_vambrace", "arm_right2", [-10.3,29.4,-.3], [
        mcube((-11.45,27.95,-2.13),(2.2,2.25,.23),80,light=82,dark=88,
              pivot=(-10.3,29.05,-2.02),rotation=(0,3,5)),
    ])
    left_greave = v1.bone("ouros_resonance_left_greave", "leg_left4", [3.5,6.15,-1.5], [
        mcube((1.9,-.9,-2.01),(2.9,5.45,.28),80,light=82,dark=88,
              pivot=(3.35,1.8,-1.87),rotation=(-9,0,-6)),
    ])
    right_greave = v1.bone("ouros_resonance_right_greave", "leg_right4", [-3.5,6.15,-1.5], [
        mcube((-4.7,-.9,-1.99),(2.7,5.35,.27),80,light=82,dark=88,
              pivot=(-3.35,1.8,-1.86),rotation=(-8,0,5)),
    ])
    tail_clasp = v1.bone("ouros_resonance_tail_clasp", "tail2", [0,19.4,10.0], [
        mcube((-1.0,18.3,9.36),(2.0,.95,.26),84,light=85,dark=80),
    ])
    return [cowl, half_cloak, mantle, sash, left_vambrace, right_vambrace, left_greave, right_greave, tail_clasp]


def build_model() -> int:
    v11.build_model()
    data = json.loads(v1.MODEL.read_text(encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    official = geo["bones"][:v1.OFFICIAL_BONES]
    if len(official) != v1.OFFICIAL_BONES:
        raise SystemExit("official Lucario bone prefix missing")
    extras = v12_bones()
    geo["bones"] = official + extras
    v1.MODEL.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    return sum(len(b.get("cubes", [])) for b in extras)


def patch_manifest(cubes: int) -> None:
    data = json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["artStatus"] = "ARTISTIC FAIL"
    data["ownerApproval"] = {"required": True, "approved": False, "approvedHeadSha": None, "evidenceSetSha256": None, "approvalRecord": None}
    data["production"]["modelSha256"] = v1.sha256(v1.MODEL)
    data["production"]["productionBoneCount"] = v1.OFFICIAL_BONES + 9
    data["production"]["cosmeticBoneCount"] = 9
    data["production"]["cosmeticCubeCount"] = cubes
    overlay = next(t for t in data["production"]["textures"] if t["role"] == "OVERLAY")
    overlay["sha256"] = v1.sha256(v1.OVERLAY)
    for asset in data["production"].get("runtimeAssets", []):
        if asset.get("role") == "RESOLVER": asset["sha256"] = v1.sha256(v1.RESOLVER)
    data["builder"]["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v12.py"
    data["builder"]["command"] = ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v12.py"]
    data["qualityIntent"]["signaturePieces"] = [
        "Shoulder-rooted vertical half-cloak using five overlapping rotated surfaces that taper from cap to hip",
        "Narrow back bridge resolving into unequal separated tails instead of a lateral wing or plate chain",
        "Open-face cowl and sparse diagonal front sash preserving face, chest spike and biological torso read"
    ]
    data["qualityIntent"]["macroFormPlan"] = (
        "V12 rejects V11's technically successful but artistically failed horizontal shoulder slabs. "
        "The signature system now travels vertically from the official left shoulder toward the hip in five overlapping surfaces whose width, angle and depth progressively change, creating wrap and taper. "
        "A narrow back bridge connects it to two unequal tails. No quality threshold is relaxed."
    )
    data["qualityIntent"]["gameplayReadGoal"] = (
        "At 160 px the silhouette should read as a one-sided ceremonial half-cloak wrapped around Lucario rather than a rigid wing, backpack, cage or stack of plates. "
        "The unchanged technical floors remain 0.0800 pixel difference and 0.0400 silhouette delta."
    )
    data["qualityIntent"]["iterationNote"] = "V12 follows direct inspection of V11: V11 passed metrics but was ARTISTIC FAIL because the shoulder macro-form read as broad horizontal slabs. Owner approval remains absent."
    v1.MANIFEST.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(); parser.add_argument("--bootstrap", action="store_true"); args = parser.parse_args()
    if v1.sha256(v1.BODY) != v1.OFFICIAL_NORMAL_SHA256: raise SystemExit("normal body texture drifted from official Lucario")
    if v1.sha256(v1.SHINY) != v1.OFFICIAL_SHINY_SHA256: raise SystemExit("shiny body texture drifted from official Lucario")
    cubes = build_model(); write_overlay(v1.OVERLAY); v1.build_resolver()
    if args.bootstrap: patch_manifest(cubes)
    print(json.dumps({"status":"BUILT","concept":"Aura Sentinel — Resonance Ronin V12","officialBones":v1.OFFICIAL_BONES,"cosmeticBones":9,"cosmeticCubes":cubes,"modelSha256":v1.sha256(v1.MODEL),"overlaySha256":v1.sha256(v1.OVERLAY),"resolverSha256":v1.sha256(v1.RESOLVER),"normalBodySha256":v1.sha256(v1.BODY),"shinyBodySha256":v1.sha256(v1.SHINY),"bodyTexelRework":"NONE","visualChange":"vertical shoulder-rooted half-cloak with tapered wrap and narrow rear tails"}, indent=2, ensure_ascii=False))

if __name__ == "__main__": main()
