#!/usr/bin/env python3
"""Resonance Ronin V15c: ribbon-sweep iteration after V15b visual QA failure.

V15b cleared the unchanged technical visual floors but its four-stage shoulder
crescent still read in real Blockbench evidence as a staircase of rectangular
plates. V15c keeps V14c's narrow mantle architecture and replaces that stepped
system with a two-stage shoulder-rooted ceremonial ribbon: long, thin, strongly
compound-rotated facets overlap at a real shoulder contact and taper sharply as
they travel up/back. The opposite hip uses two shorter unequal ribbons. This
preserves a strong asymmetric contour while removing the repeated slab cadence.
Official Lucario anatomy remains untouched and owner approval remains absent.
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

    # One readable shoulder gesture rather than a plate staircase. The first
    # ribbon overlaps the official shoulder/contact zone and the second narrows
    # hard while rotating farther back. Their proportions and compound rotations
    # create a bent ceremonial streamer without a parallel-bar cadence.
    shoulder_sweep = v1.bone("ouros_resonance_shoulder_sweep", "shoulder_left", [7.5, 30.4, .7], [
        mcube((6.45, 27.55, .92), (2.20, 8.20, .36), 82, light=83, dark=81,
              pivot=(7.35, 30.10, 1.10), rotation=(-24, -23, -36)),
        mcube((8.35, 31.45, 1.48), (1.25, 7.15, .27), 84, light=85, dark=80,
              pivot=(8.75, 32.25, 1.62), rotation=(-31, -34, -56)),
    ])

    # Short unequal hip response; it terminates the diagonal rhythm without
    # mirroring the shoulder or enclosing Lucario's biological tail.
    hip_streamers = v1.bone("ouros_resonance_hip_streamers", "torso", [0, 15.0, 1.4], [
        mcube((-5.85, 7.65, 1.30), (1.80, 6.65, .34), 82, light=83, dark=81,
              pivot=(-4.95, 11.80, 1.47), rotation=(-30, 18, 27)),
        mcube((2.15, 9.05, 2.42), (1.15, 4.40, .26), 84, light=85, dark=80,
              pivot=(2.62, 11.70, 2.55), rotation=(-27, -16, -25)),
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
        "Continuous cowl/collar/shoulder/back/hip mantle arc leaving Lucario's face and chest spike open",
        "One two-stage shoulder-rooted ceremonial ribbon whose long thin facets overlap, taper sharply and turn in compound rotation rather than forming a stepped plate fan",
        "Asymmetric hip knot with two short unequal ribbon responses and negative space around the biological tail",
    ]
    data["qualityIntent"]["macroFormPlan"] = (
        "V15c preserves V14c's narrow mantle architecture and removes V15b's four-stage stepped crescent after direct Blockbench QA. The signature contour is now one shoulder-rooted ribbon gesture built from only two long, thin, strongly rotated overlapping facets, with an unequal two-ribbon hip response. The design spends silhouette on curvature and directional flow rather than repeated slabs. No threshold changes and no biological edits."
    )
    data["qualityIntent"]["gameplayReadGoal"] = (
        "At 160 px the first read should be a single asymmetric ceremonial sweep, with one narrow ribbon rising/back from the left shoulder and a much shorter response at the opposite hip. The silhouette should read as cloth motion rather than armor plates while Lucario's ears, aura sensors, chest spike, hands, feet and biological tail remain unmistakable."
    )
    data["qualityIntent"]["iterationNote"] = (
        "Exact V15b head a814ee180d5ee272d1ec3bf09e0ab7e79f061901 passed the unchanged technical floors at pixelDifferenceRatio=0.11852 and silhouetteDeltaRatio=0.053315, but direct inspection of its Blockbench PNGs remained ARTISTIC FAIL because the shoulder crescent read as a staircase of rectangular plates. V15c removes that cadence rather than adding volume or relaxing thresholds; owner approval remains absent."
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
        "concept": "Aura Sentinel — Resonance Ronin V15c",
        "officialBones": v1.OFFICIAL_BONES,
        "cosmeticBones": 13,
        "cosmeticCubes": cubes,
        "modelSha256": v1.sha256(v1.MODEL),
        "overlaySha256": v1.sha256(v1.OVERLAY),
        "bodyTexelRework": "NONE",
        "visualChange": "two-stage tapered shoulder ribbon plus unequal hip response; unchanged technical floors",
    }, indent=2))


if __name__ == "__main__":
    main()
