#!/usr/bin/env python3
"""Resonance Ronin V13: continuous contour-wrap macroform after V12 artistic fail.

V12 was technically valid but its half-cloak still read as a cascade of rectangular
panels and its lower front composition competed with Lucario's body. V13 removes
that panel ladder. It preserves the exact official Lucario prefix and concentrates
the transformation into one torso-rooted shoulder/back/hip sweep with changing
width, depth and compound rotations, plus sparse motion-safe supports.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V12_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v12.py"
spec = importlib.util.spec_from_file_location("resonance_v12", V12_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V12 builder")
v12 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v12)
v1 = v12.v1
mcube = v12.mcube
write_overlay = v12.write_overlay


def v13_bones() -> list[dict]:
    # Open cowl: two angled cheek/temple masses only. No horizontal visor bar.
    cowl = v1.bone("ouros_resonance_cowl", "head_angle", [0, 37.0, -1.2], [
        mcube((-4.35, 35.55, -3.85), (2.25, 4.05, .42), 82, light=85, dark=80,
              pivot=(-3.05, 37.45, -3.62), rotation=(-5, -10, 18)),
        mcube((2.15, 36.0, -3.78), (1.95, 3.45, .38), 82, light=85, dark=80,
              pivot=(3.05, 37.55, -3.58), rotation=(-4, 8, -14)),
    ])

    # Signature macroform. One connected sweep travels from left shoulder across
    # the back and resolves at the opposite hip. Width/depth/angle change on each
    # overlapping section so the silhouette reads as a wrapped garment, not a
    # ladder of equal panels or a rigid wing.
    contour_wrap = v1.bone("ouros_resonance_contour_wrap", "torso3", [0, 27.9, 1.8], [
        # Shoulder root/contact mass, compact and angled into torso.
        mcube((3.45, 28.15, -.25), (6.2, 4.15, 3.55), 82, light=83, dark=81,
              pivot=(6.2, 30.15, 1.35), rotation=(7, -12, -18)),
        # Upper back sweep: broad diagonal plane, not horizontal.
        mcube((1.45, 24.0, 2.0), (8.7, 6.35, .54), 81, light=83, dark=88,
              pivot=(5.7, 28.35, 2.28), rotation=(-17, -9, -24)),
        # Mid-back turn: narrower, deeper and rotated the opposite way in Z.
        mcube((-1.15, 19.15, 2.42), (8.1, 6.15, .62), 82, light=83, dark=81,
              pivot=(3.8, 23.75, 2.72), rotation=(-22, 8, -13)),
        # Hip wrap: substantial depth makes it hug the body instead of floating.
        mcube((-4.35, 15.25, 1.82), (7.15, 5.2, 1.05), 81, light=83, dark=88,
              pivot=(-.55, 19.15, 2.35), rotation=(-15, 13, 12)),
        # Single long terminal tail with strong taper/read; offset creates negative space.
        mcube((-5.6, 8.2, 2.45), (4.45, 8.85, .46), 82, light=85, dark=80,
              pivot=(-3.05, 15.1, 2.67), rotation=(-25, 12, 20)),
        # Short counter-tail, deliberately unequal to avoid a skirt/shorts silhouette.
        mcube((-.15, 11.45, 2.62), (2.7, 5.5, .36), 84, light=85, dark=80,
              pivot=(1.1, 15.65, 2.80), rotation=(-21, -9, -11)),
    ])

    # One narrow diagonal chest tie only; no lower-front plates.
    chest_tie = v1.bone("ouros_resonance_chest_tie", "torso3", [0, 27.5, -3.65], [
        mcube((-4.55, 27.05, -4.18), (7.55, .62, .24), 84, light=85, dark=80,
              pivot=(-.8, 27.38, -4.05), rotation=(0, 0, -34)),
    ])

    # Sparse limb continuation: each support is a single tapered-looking rotated strip.
    left_vambrace = v1.bone("ouros_resonance_left_vambrace", "arm_left2", [10.3, 29.0, -.4], [
        mcube((9.0, 27.6, -2.10), (2.45, 2.55, .24), 80, light=82, dark=88,
              pivot=(10.25, 28.85, -1.98), rotation=(2, -7, -10)),
    ])
    right_vambrace = v1.bone("ouros_resonance_right_vambrace", "arm_right2", [-10.3, 29.0, -.4], [
        mcube((-11.25, 28.0, -2.07), (1.95, 2.0, .22), 80, light=82, dark=88,
              pivot=(-10.28, 29.0, -1.96), rotation=(2, 4, 7)),
    ])
    left_greave = v1.bone("ouros_resonance_left_greave", "leg_left4", [3.45, 5.2, -1.45], [
        mcube((2.05, -.4, -1.98), (2.55, 4.7, .26), 80, light=82, dark=88,
              pivot=(3.32, 2.0, -1.85), rotation=(-10, 2, -7)),
    ])
    right_greave = v1.bone("ouros_resonance_right_greave", "leg_right4", [-3.45, 5.2, -1.45], [
        mcube((-4.55, -.25, -1.96), (2.35, 4.45, .25), 80, light=82, dark=88,
              pivot=(-3.36, 2.0, -1.84), rotation=(-9, -2, 6)),
    ])

    return [cowl, contour_wrap, chest_tie, left_vambrace, right_vambrace, left_greave, right_greave]


def build_model() -> int:
    v12.build_model()
    data = json.loads(v1.MODEL.read_text(encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    official = geo["bones"][:v1.OFFICIAL_BONES]
    if len(official) != v1.OFFICIAL_BONES:
        raise SystemExit("official Lucario bone prefix missing")
    extras = v13_bones()
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
    data["production"]["productionBoneCount"] = v1.OFFICIAL_BONES + 7
    data["production"]["cosmeticBoneCount"] = 7
    data["production"]["cosmeticCubeCount"] = cubes
    overlay = next(t for t in data["production"]["textures"] if t["role"] == "OVERLAY")
    overlay["sha256"] = v1.sha256(v1.OVERLAY)
    for asset in data["production"].get("runtimeAssets", []):
        if asset.get("role") == "RESOLVER":
            asset["sha256"] = v1.sha256(v1.RESOLVER)
    data["builder"]["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v13.py"
    data["builder"]["command"] = ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v13.py"]
    data["qualityIntent"]["signaturePieces"] = [
        "Single torso-rooted shoulder-to-back-to-opposite-hip contour wrap with six unequal compound-rotated sections",
        "Asymmetric long terminal tail plus short counter-tail separated by deliberate rear negative space",
        "Open cowl and one diagonal chest tie that leave Lucario's face, chest spike and lower-front anatomy unobstructed",
    ]
    data["qualityIntent"]["macroFormPlan"] = (
        "V13 removes V12's visible panel ladder and all lower-front plate masses. One connected contour-wrap system now crosses the body diagonally from the left shoulder, turns through the back, hugs the opposite hip and resolves into unequal tails. Each section changes width, depth and compound rotation so the outside edge bends around the anatomy rather than reading as repeated slabs."
    )
    data["qualityIntent"]["gameplayReadGoal"] = (
        "At 160 px the first read should be a single asymmetric ceremonial wrap cutting a strong diagonal silhouette through Lucario, with the chest and legs still clearly biological. The transformation must remain legible without any cage, wing, shorts or stacked-panel read."
    )
    data["qualityIntent"]["iterationNote"] = (
        "V13 follows direct V12 Blockbench rejection: V12 passed engineering floors but remained ARTISTIC FAIL because the half-cloak read as a rectangular panel cascade and the lower-front composition resembled oversized shorts/plates. V13 deliberately reduces cosmetic systems and concentrates authorship into one continuous diagonal macroform. Owner approval remains absent."
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
        "status": "BUILT",
        "concept": "Aura Sentinel — Resonance Ronin V13",
        "officialBones": v1.OFFICIAL_BONES,
        "cosmeticBones": 7,
        "cosmeticCubes": cubes,
        "modelSha256": v1.sha256(v1.MODEL),
        "overlaySha256": v1.sha256(v1.OVERLAY),
        "resolverSha256": v1.sha256(v1.RESOLVER),
        "normalBodySha256": v1.sha256(v1.BODY),
        "shinyBodySha256": v1.sha256(v1.SHINY),
        "bodyTexelRework": "NONE",
        "visualChange": "continuous shoulder-back-hip contour wrap; no lower-front plates or panel ladder",
    }, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
