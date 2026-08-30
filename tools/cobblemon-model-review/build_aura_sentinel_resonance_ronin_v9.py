#!/usr/bin/env python3
"""Resonance Ronin V9: broad continuous haori pass after V8 visual-floor failure.

V8 preserved the intended clean front read but changed too few matched-camera
pixels (0.0725 < 0.0800). V9 does not lower that floor. It keeps Lucario's exact
87-bone biological prefix and rebuilds the signature as one wider shoulder-rooted
haori sweep with deep physical overlap, progressive taper and a deliberate outer
crescent. Presentation only; AutoPTU/Ouros remains battle authority.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V8_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v8.py"
spec = importlib.util.spec_from_file_location("resonance_v8", V8_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V8 builder")
v8 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v8)
v7 = v8.v7
v6 = v8.v6
v1 = v8.v1
mcube = v8.mcube
write_overlay = v8.write_overlay


def v9_bones() -> list[dict]:
    circlet = v1.bone("ouros_resonance_circlet", "head_angle", [0, 38.0, -1.6], [
        mcube((-4.0, 37.72, -4.7), (3.1, .34, .20), 84, light=85, dark=80,
              pivot=(-2.4, 37.9, -4.6), rotation=(0, 0, -9)),
        mcube((.9, 37.72, -4.7), (3.1, .34, .20), 84, light=85, dark=80,
              pivot=(2.4, 37.9, -4.6), rotation=(0, 0, 9)),
    ])

    # Signature system: one connected shoulder-rooted haori sweep. Every stage
    # overlaps its neighbour in bind-space AABB, while rotations progressively
    # turn the material outward/back. The outer edge widens first, then tapers,
    # creating a readable crescent rather than a chain of floating tiles.
    mantle = v1.bone("ouros_resonance_mantle", "torso3", [0, 29.4, 1.2], [
        mcube((-8.5, 28.0, -1.45), (7.2, 3.8, 5.0), 82, light=83, dark=81,
              pivot=(-4.9, 30.0, .9), rotation=(4, -9, -14)),
        mcube((-9.35, 30.25, .55), (5.0, 2.7, .52), 84, light=85, dark=80,
              pivot=(-6.8, 31.45, .8), rotation=(-4, 8, -23)),
        mcube((-10.0, 24.0, 1.82), (7.6, 6.2, .58), 81, light=83, dark=88,
              pivot=(-6.1, 29.25, 2.11), rotation=(-11, 7, -8)),
        mcube((-12.8, 19.15, 2.02), (7.9, 6.6, .54), 82, light=83, dark=81,
              pivot=(-8.75, 24.6, 2.29), rotation=(-13, 9, -11)),
        mcube((-15.0, 14.15, 2.22), (7.5, 6.55, .48), 81, light=83, dark=88,
              pivot=(-11.1, 19.55, 2.46), rotation=(-16, 12, -14)),
        mcube((-16.55, 9.8, 2.44), (6.35, 5.95, .42), 82, light=83, dark=81,
              pivot=(-13.2, 14.6, 2.65), rotation=(-19, 14, -17)),
        mcube((-16.05, 6.8, 2.66), (4.5, 4.4, .36), 81, light=83, dark=88,
              pivot=(-13.7, 10.3, 2.84), rotation=(-22, 16, -20)),
        mcube((-15.4, 6.55, 2.91), (3.3, .30, .16), 86, light=87, dark=81,
              pivot=(-13.7, 6.72, 2.98), rotation=(-22, 16, -20)),
        mcube((-8.2, 25.2, 2.37), (2.0, 4.8, .22), 84, light=85, dark=80,
              pivot=(-7.15, 28.9, 2.48), rotation=(-11, 7, -8)),
    ])

    # Front remains deliberately open around the chest spike. The wider diagonal
    # lapel ties the mantle into the torso without building a box around Lucario.
    sash = v1.bone("ouros_resonance_sash", "torso3", [0, 27.0, -3.4], [
        mcube((-5.25, 27.15, -4.10), (8.8, .92, .26), 81, light=83, dark=88,
              pivot=(-.8, 27.6, -3.96), rotation=(0, 0, -31)),
        mcube((-3.45, 22.8, -3.64), (6.9, .68, .58), 84, light=85, dark=80),
        mcube((-5.05, 23.0, -3.20), (2.8, 2.7, .24), 82, light=83, dark=81,
              pivot=(-3.65, 24.3, -3.08), rotation=(0, 0, -18)),
    ])

    left_vambrace = v1.bone("ouros_resonance_left_vambrace", "arm_left2", [10.3, 29.4, -.3], [
        mcube((9.0, 27.75, -2.18), (2.65, 2.7, .30), 80, light=82, dark=88,
              pivot=(10.3, 29.1, -2.03), rotation=(0, -4, -6)),
    ])
    right_vambrace = v1.bone("ouros_resonance_right_vambrace", "arm_right2", [-10.3, 29.4, -.3], [
        mcube((-11.55, 27.9, -2.14), (2.35, 2.4, .26), 80, light=82, dark=88,
              pivot=(-10.3, 29.1, -2.01), rotation=(0, 3, 5)),
    ])
    left_greave = v1.bone("ouros_resonance_left_greave", "leg_left4", [3.5, 6.15, -1.5], [
        mcube((1.95, -1.0, -2.0), (2.8, 5.6, .30), 80, light=82, dark=88,
              pivot=(3.35, 1.8, -1.85), rotation=(-8, 0, -5)),
    ])
    right_greave = v1.bone("ouros_resonance_right_greave", "leg_right4", [-3.5, 6.15, -1.5], [
        mcube((-4.75, -1.0, -2.0), (2.8, 5.6, .30), 80, light=82, dark=88,
              pivot=(-3.35, 1.8, -1.85), rotation=(-8, 0, 5)),
    ])
    tail_clasp = v1.bone("ouros_resonance_tail_clasp", "tail2", [0, 19.4, 10.0], [
        mcube((-1.05, 18.25, 9.35), (2.1, 1.05, .28), 84, light=85, dark=80),
    ])
    return [circlet, mantle, sash, left_vambrace, right_vambrace, left_greave, right_greave, tail_clasp]


def build_model() -> int:
    v8.build_model()
    data = json.loads(v1.MODEL.read_text(encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    official = geo["bones"][:v1.OFFICIAL_BONES]
    if len(official) != v1.OFFICIAL_BONES:
        raise SystemExit("official Lucario bone prefix missing")
    extras = v9_bones()
    geo["bones"] = official + extras
    v1.MODEL.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    return sum(len(b.get("cubes", [])) for b in extras)


def patch_manifest(cubes: int) -> None:
    data = json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["artStatus"] = "ARTISTIC FAIL"
    data["ownerApproval"] = {
        "required": True, "approved": False, "approvedHeadSha": None,
        "evidenceSetSha256": None, "approvalRecord": None
    }
    data["production"]["modelSha256"] = v1.sha256(v1.MODEL)
    data["production"]["productionBoneCount"] = v1.OFFICIAL_BONES + 8
    data["production"]["cosmeticBoneCount"] = 8
    data["production"]["cosmeticCubeCount"] = cubes
    overlay = next(t for t in data["production"]["textures"] if t["role"] == "OVERLAY")
    overlay["sha256"] = v1.sha256(v1.OVERLAY)
    for asset in data["production"].get("runtimeAssets", []):
        if asset.get("role") == "RESOLVER":
            asset["sha256"] = v1.sha256(v1.RESOLVER)
    data["builder"]["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v9.py"
    data["builder"]["command"] = ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v9.py"]
    data["qualityIntent"]["signaturePieces"] = [
        "Single seven-stage shoulder-rooted resonance haori with broad outer crescent and continuous taper",
        "Open diagonal lapel/sash that visually ties the haori into the torso while preserving the chest spike",
        "Subordinate articulation-safe arm, shin and tail accents"
    ]
    data["qualityIntent"]["macroFormPlan"] = (
        "V9 keeps V8's clean biological front but widens the signature into one connected shoulder-rooted haori. "
        "Seven deeply overlapping thin stages turn progressively outward/back, grow into a broad middle crescent, then taper to one terminal point. "
        "No skirt, shorts, cage, backpack, repeated bar or armor-shell system is introduced."
    )
    data["qualityIntent"]["gameplayReadGoal"] = (
        "At 160 px Lucario must read with one unmistakable asymmetric haori crescent from shoulder through hip, while the face, chest spike, limbs and tail remain species-clear. "
        "The matched-camera pixel floor stays at 0.0800 and must be exceeded by authored silhouette rather than threshold relaxation."
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
    cubes = build_model()
    write_overlay(v1.OVERLAY)
    v1.build_resolver()
    if args.bootstrap:
        patch_manifest(cubes)
    print(json.dumps({
        "status":"BUILT","concept":"Aura Sentinel — Resonance Ronin V9",
        "officialBones":v1.OFFICIAL_BONES,"cosmeticBones":8,"cosmeticCubes":cubes,
        "modelSha256":v1.sha256(v1.MODEL),"overlaySha256":v1.sha256(v1.OVERLAY),
        "resolverSha256":v1.sha256(v1.RESOLVER),"normalBodySha256":v1.sha256(v1.BODY),
        "shinyBodySha256":v1.sha256(v1.SHINY),"bodyTexelRework":"NONE",
        "visualChange":"wider continuous shoulder-rooted haori crescent; technical visual floor unchanged"
    }, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()
