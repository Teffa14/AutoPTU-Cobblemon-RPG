#!/usr/bin/env python3
"""Resonance Ronin V15b: authored crescent-contour rework after exact V15 floor failure.

Exact V15 Blockbench evidence measured silhouetteDeltaRatio=0.0303 against the
unchanged 0.0400 floor. V15b does not lower that floor and does not add a cage,
portal frame, broad rectangular pauldron, or alternate body rig. It keeps V14c's
narrow shoulder-to-hip mantle architecture, then replaces V15's small pennons with
one visually continuous shoulder-rooted crescent sweep: four overlapping thin
segments progressively taper, rotate and recede from the official left shoulder.
A paired hip flourish continues the same diagonal rhythm with explicit negative
space around Lucario's biological tail. Official Lucario anatomy remains untouched.
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


def v15_bones() -> list[dict]:
    bones = v14.v14_bones()

    # Signature system: a shoulder-rooted crescent made from overlapping cloth
    # facets. The contact/root piece is deliberately substantial enough to make
    # the sweep feel worn, while the following segments narrow and rotate away.
    # Gaps stay visible at the outer edge so the contour reads as layered cloth,
    # not one rectangular wing.
    shoulder_sweep = v1.bone("ouros_resonance_shoulder_sweep", "shoulder_left", [7.5, 30.4, .7], [
        mcube((5.85, 29.35, .85), (3.45, 2.55, .56), 81, light=83, dark=88,
              pivot=(7.15, 30.55, 1.12), rotation=(-10, -13, -20)),
        mcube((7.45, 30.45, 1.18), (3.15, 5.65, .38), 82, light=83, dark=81,
              pivot=(8.15, 31.45, 1.37), rotation=(-17, -22, -33)),
        mcube((9.55, 31.65, 1.65), (2.65, 5.30, .34), 81, light=85, dark=88,
              pivot=(10.05, 32.55, 1.82), rotation=(-21, -27, -44)),
        mcube((11.65, 32.55, 2.12), (2.05, 4.55, .29), 84, light=85, dark=80,
              pivot=(11.95, 33.30, 2.26), rotation=(-27, -31, -56)),
    ])

    # The hip flourish is intentionally shorter than the shoulder crescent. It
    # resolves the diagonal composition without mirroring it or enclosing the
    # body. Two tapered ribbons split around the biological tail with clear air.
    hip_streamers = v1.bone("ouros_resonance_hip_streamers", "torso", [0, 15.0, 1.4], [
        mcube((-5.75, 8.25, 1.25), (2.05, 6.55, .38), 82, light=83, dark=81,
              pivot=(-4.75, 12.25, 1.44), rotation=(-27, 17, 24)),
        mcube((-7.45, 6.15, 1.72), (1.55, 5.15, .31), 84, light=85, dark=80,
              pivot=(-6.45, 9.65, 1.87), rotation=(-34, 22, 36)),
        mcube((2.15, 8.75, 2.38), (1.30, 4.70, .28), 81, light=85, dark=88,
              pivot=(2.65, 11.75, 2.52), rotation=(-29, -15, -24)),
    ])
    return bones + [shoulder_sweep, hip_streamers]


def build_model() -> int:
    v14.build_model()
    data = json.loads(v1.MODEL.read_text(encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    official = geo["bones"][:v1.OFFICIAL_BONES]
    if len(official) != v1.OFFICIAL_BONES:
        raise SystemExit("official Lucario bone prefix missing")
    extras = v15_bones()
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
    data["production"]["productionBoneCount"] = v1.OFFICIAL_BONES + 13
    data["production"]["cosmeticBoneCount"] = 13
    data["production"]["cosmeticCubeCount"] = cubes
    next(t for t in data["production"]["textures"] if t["role"] == "OVERLAY")["sha256"] = v1.sha256(v1.OVERLAY)
    for asset in data["production"].get("runtimeAssets", []):
        if asset.get("role") == "RESOLVER":
            asset["sha256"] = v1.sha256(v1.RESOLVER)
    data["builder"]["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v15.py"
    data["builder"]["command"] = ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v15.py"]
    data["qualityIntent"]["signaturePieces"] = [
        "Continuous cowl/collar/shoulder/back/hip mantle arc with the official chest spike and face left open",
        "One shoulder-rooted four-stage crescent sweep whose overlapping facets taper and rotate progressively instead of forming a slab or repeated bars",
        "Asymmetric hip knot with split tapered ribbons preserving negative space around Lucario's biological tail",
    ]
    data["qualityIntent"]["macroFormPlan"] = (
        "V15b preserves V14c's narrow mantle architecture but replaces V15's undersized separated pennons with one connected visual crescent. Four thin overlapping shoulder facets expand outward only after a substantial shoulder contact mass, progressively narrowing and rotating to create a curved silhouette. The opposite hip resolves the same diagonal with three shorter split ribbons. No threshold changes and no biological edits."
    )
    data["qualityIntent"]["gameplayReadGoal"] = (
        "At 160 px the first read should be a single ceremonial crescent sweeping from Lucario's left shoulder around the back into the opposite hip. The outer edge must remain legible as tapered cloth motion with visible negative space, while the face, ears, aura sensors, chest spike, hands, feet and biological tail remain unmistakably Lucario."
    )
    data["qualityIntent"]["iterationNote"] = (
        "Exact V15 head 0bf6e61c00fc01f7d22a55e3ca123534e283725f passed reference, source, builder, bone, attachment and rendering stages but failed the unchanged matched-camera silhouette floor at 0.0303 < 0.0400. V15b materially reauthors the signature contour rather than lowering the floor; owner approval remains absent."
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
        "concept": "Aura Sentinel — Resonance Ronin V15b",
        "officialBones": v1.OFFICIAL_BONES,
        "cosmeticBones": 13,
        "cosmeticCubes": cubes,
        "modelSha256": v1.sha256(v1.MODEL),
        "overlaySha256": v1.sha256(v1.OVERLAY),
        "bodyTexelRework": "NONE",
        "visualChange": "shoulder-rooted tapered crescent plus asymmetric hip resolution; unchanged technical floors",
    }, indent=2))


if __name__ == "__main__":
    main()
