#!/usr/bin/env python3
"""Resonance Ronin V11: connected asymmetric resonance crest + tapered rear mantle.

V10 failed the unchanged Blockbench silhouette floor at 0.0350 (< 0.0400).
V11 does not relax that gate. It preserves Lucario's exact 87-bone official
Cobblemon prefix and replaces the weak rear read with one shoulder-rooted,
body-connected signature system built from broad overlapping rotated planes.
Presentation only: AutoPTU/Ouros remains authoritative for tactical battle facts.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V10_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v10.py"
spec = importlib.util.spec_from_file_location("resonance_v10", V10_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V10 builder")
v10 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v10)
v1 = v10.v1
mcube = v10.mcube
write_overlay = v10.write_overlay


def v11_bones() -> list[dict]:
    # Open-face cowl: small enough that Lucario's biological face and aura sensors
    # stay dominant, but angled side planes connect visually into the shoulder crest.
    cowl = v1.bone("ouros_resonance_cowl", "head_angle", [0, 37.2, -0.8], [
        mcube((-4.15, 38.0, -1.0), (8.3, 1.35, 3.25), 84, light=85, dark=80,
              pivot=(0, 38.6, .5), rotation=(-5, 0, 0)),
        mcube((-4.55, 35.0, -4.82), (1.25, 3.8, .30), 82, light=83, dark=81,
              pivot=(-3.85, 36.9, -4.67), rotation=(0, -6, 16)),
        mcube((3.30, 35.3, -4.80), (1.10, 3.35, .28), 82, light=83, dark=81,
              pivot=(3.82, 36.9, -4.66), rotation=(0, 5, -12)),
    ])

    # Signature crest. This is deliberately one connected shoulder-rooted system,
    # not a detached banner or repeated bar chain. Three broad overlapping planes
    # change scale and angle to form a tapered swept contour behind the left side.
    crest = v1.bone("ouros_resonance_signature_crest", "shoulder_left", [8.4, 31.2, 1.0], [
        # contact/root mass overlaps the official shoulder/torso envelope
        mcube((5.8, 29.0, -0.4), (5.4, 4.2, 4.5), 82, light=83, dark=81,
              pivot=(8.4, 31.1, 1.6), rotation=(4, -8, -13)),
        # broad middle sweep; substantial surface rather than a thin bar
        mcube((8.8, 29.5, 1.55), (7.8, 5.6, .62), 81, light=83, dark=88,
              pivot=(9.1, 31.6, 1.85), rotation=(-12, -17, -24)),
        # outer taper; smaller and more rotated to finish the silhouette
        mcube((13.3, 30.4, 2.05), (6.3, 4.4, .54), 82, light=83, dark=81,
              pivot=(13.6, 32.0, 2.32), rotation=(-15, -23, -34)),
        # lower overlap closes the visual root into the back mantle
        mcube((6.6, 25.7, 2.0), (7.7, 4.7, .58), 81, light=83, dark=88,
              pivot=(8.6, 29.3, 2.28), rotation=(-14, -9, -12)),
    ])

    # Rear mantle continues the crest through the torso, then splits into two
    # unequal tails. Width and angle shrink progressively to create contour/taper.
    mantle = v1.bone("ouros_resonance_mantle", "torso3", [0, 28.9, 1.5], [
        mcube((-6.8, 27.3, 1.5), (13.8, 4.2, 1.15), 82, light=83, dark=81,
              pivot=(0, 29.4, 2.0), rotation=(-9, 0, 2)),
        mcube((-6.25, 22.6, 2.28), (12.7, 5.6, .62), 81, light=83, dark=88,
              pivot=(0, 27.1, 2.58), rotation=(-14, 0, 2)),
        mcube((-5.75, 18.0, 2.58), (11.6, 5.6, .52), 82, light=83, dark=81,
              pivot=(0, 22.5, 2.84), rotation=(-17, 0, 3)),
        # longer left tail, sweeping outward and back
        mcube((-6.7, 10.1, 2.92), (5.8, 9.7, .46), 81, light=83, dark=88,
              pivot=(-3.7, 18.1, 3.15), rotation=(-21, 8, -9)),
        mcube((-6.1, 5.2, 3.18), (4.5, 6.4, .38), 82, light=83, dark=81,
              pivot=(-3.75, 10.5, 3.37), rotation=(-24, 11, -13)),
        # shorter right tail leaves a deliberate centre gap and asymmetry
        mcube((1.0, 11.8, 2.94), (5.1, 7.9, .44), 82, light=83, dark=81,
              pivot=(3.6, 18.3, 3.16), rotation=(-19, -7, 8)),
        mcube((1.8, 8.0, 3.17), (3.8, 5.1, .36), 81, light=83, dark=88,
              pivot=(3.8, 12.1, 3.35), rotation=(-22, -10, 12)),
    ])

    # Front remains sparse. A diagonal lapel and low obi link the rear mass to the
    # body without covering Lucario's chest spike or rebuilding the torso.
    sash = v1.bone("ouros_resonance_sash", "torso3", [0, 27.0, -3.4], [
        mcube((-5.2, 27.0, -4.10), (8.9, .82, .26), 84, light=85, dark=80,
              pivot=(-.8, 27.5, -3.97), rotation=(0, 0, -31)),
        mcube((-3.5, 22.75, -3.62), (7.0, .62, .54), 84, light=85, dark=80),
    ])

    left_vambrace = v1.bone("ouros_resonance_left_vambrace", "arm_left2", [10.3,29.4,-.3], [
        mcube((8.95,27.7,-2.18),(2.75,2.75,.28),80,light=82,dark=88,
              pivot=(10.3,29.05,-2.04),rotation=(0,-5,-7)),
    ])
    right_vambrace = v1.bone("ouros_resonance_right_vambrace", "arm_right2", [-10.3,29.4,-.3], [
        mcube((-11.5,27.9,-2.14),(2.3,2.35,.24),80,light=82,dark=88,
              pivot=(-10.3,29.05,-2.02),rotation=(0,3,5)),
    ])
    left_greave = v1.bone("ouros_resonance_left_greave", "leg_left4", [3.5,6.15,-1.5], [
        mcube((1.85,-1.0,-2.02),(3.0,5.7,.30),80,light=82,dark=88,
              pivot=(3.35,1.8,-1.87),rotation=(-9,0,-6)),
    ])
    right_greave = v1.bone("ouros_resonance_right_greave", "leg_right4", [-3.5,6.15,-1.5], [
        mcube((-4.75,-1.0,-2.0),(2.8,5.6,.28),80,light=82,dark=88,
              pivot=(-3.35,1.8,-1.86),rotation=(-8,0,5)),
    ])
    tail_clasp = v1.bone("ouros_resonance_tail_clasp", "tail2", [0,19.4,10.0], [
        mcube((-1.05,18.25,9.35),(2.1,1.05,.28),84,light=85,dark=80),
    ])
    return [cowl, crest, mantle, sash, left_vambrace, right_vambrace, left_greave, right_greave, tail_clasp]


def build_model() -> int:
    v10.build_model()
    data = json.loads(v1.MODEL.read_text(encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    official = geo["bones"][:v1.OFFICIAL_BONES]
    if len(official) != v1.OFFICIAL_BONES:
        raise SystemExit("official Lucario bone prefix missing")
    extras = v11_bones()
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
        if asset.get("role") == "RESOLVER":
            asset["sha256"] = v1.sha256(v1.RESOLVER)
    data["builder"]["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v11.py"
    data["builder"]["command"] = ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v11.py"]
    data["qualityIntent"]["signaturePieces"] = [
        "Asymmetric shoulder-rooted resonance crest made from broad overlapping rotated planes with a tapered outer contour",
        "Continuous rear mantle narrowing through the back into unequal split tails with deliberate centre negative space",
        "Compact open-face cowl and sparse diagonal front lapel preserving Lucario's face and chest spike"
    ]
    data["qualityIntent"]["macroFormPlan"] = (
        "V11 responds directly to V10's 0.0350 silhouette-floor failure without lowering the 0.0400 threshold. "
        "A shoulder-rooted asymmetric crest now supplies the primary three-quarter silhouette through four broad overlapping surfaces that change scale and angle, then connects into a tapered rear mantle and unequal split tails. "
        "The front stays open and minimal so the candidate does not return to box armor, a cage, repeated bars, shorts, or a portal-frame read."
    )
    data["qualityIntent"]["gameplayReadGoal"] = (
        "At 160 px the first read should be Lucario transformed into a resonance ronin by one swept shoulder-to-back silhouette, with a clear open front and two separated trailing tails. "
        "The technical floors remain minimumPixelDifferenceRatio 0.0800 and minimumSilhouetteDeltaRatio 0.0400 and must not be relaxed."
    )
    data["qualityIntent"]["iterationNote"] = "V11 follows V10 Blockbench technical visual-floor failure (silhouetteDeltaRatio 0.0350 < 0.0400); owner approval remains absent."
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
        "status": "BUILT",
        "concept": "Aura Sentinel — Resonance Ronin V11",
        "officialBones": v1.OFFICIAL_BONES,
        "cosmeticBones": 9,
        "cosmeticCubes": cubes,
        "modelSha256": v1.sha256(v1.MODEL),
        "overlaySha256": v1.sha256(v1.OVERLAY),
        "resolverSha256": v1.sha256(v1.RESOLVER),
        "normalBodySha256": v1.sha256(v1.BODY),
        "shinyBodySha256": v1.sha256(v1.SHINY),
        "bodyTexelRework": "NONE",
        "visualChange": "asymmetric shoulder-rooted resonance crest connected into tapered rear mantle"
    }, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
