#!/usr/bin/env python3
"""Resonance Ronin V16b: one camera-visible asymmetric mantle envelope.

Exact-head Blockbench evidence for V16 reached only 0.0265 silhouette delta and
showed the rear mantle hidden behind Lucario in the matched 3/4 camera. V16b moves
the signature system to the camera-near official right shoulder and wraps it down
the near flank. It removes the scattered greaves and uses a small number of long,
overlapping, compound-rotated cloth facets with changing width and direction.

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


def v16_bones() -> list[dict]:
    retained = [b for b in v14.v14_bones() if b["name"] in RETAINED]
    if {b["name"] for b in retained} != RETAINED:
        raise SystemExit("V14c retained-bone contract drifted")

    # Matched 3/4 camera sits at negative X / negative Z. Root the dominant
    # system on the official right shoulder (negative X), where it can actually
    # change first-read silhouette instead of disappearing behind the torso.
    shoulder_shell = v1.bone("ouros_resonance_shoulder_shell", "shoulder_right", [-7.2, 30.3, -1.0], [
        mcube((-9.0, 28.45, -3.00), (4.35, 3.20, 1.25), 81, light=83, dark=80,
              pivot=(-7.15, 30.0, -2.35), rotation=(-8, 16, 27)),
        mcube((-10.15, 25.35, -2.72), (3.55, 5.05, .48), 82, light=85, dark=81,
              pivot=(-8.25, 29.05, -2.48), rotation=(-11, 18, 22)),
        mcube((-8.25, 30.10, -3.10), (2.15, 3.05, .34), 84, light=85, dark=80,
              pivot=(-7.10, 31.55, -2.93), rotation=(-3, 8, 43)),
    ])

    # Two main facets overlap into one near-side S curve. They deliberately
    # change direction and width rather than forming parallel rectangular steps.
    flank_mantle = v1.bone("ouros_resonance_flank_mantle", "torso3", [-4.0, 24.5, -2.65], [
        mcube((-9.10, 20.20, -3.12), (4.65, 8.35, .48), 82, light=83, dark=81,
              pivot=(-6.90, 26.55, -2.88), rotation=(-5, 10, 18)),
        mcube((-7.05, 14.15, -2.92), (4.15, 8.05, .42), 81, light=85, dark=80,
              pivot=(-5.25, 20.65, -2.70), rotation=(1, -5, -9)),
        mcube((-10.25, 18.25, -2.72), (2.05, 5.85, .30), 86, light=85, dark=81,
              pivot=(-8.95, 22.35, -2.57), rotation=(-4, 13, 33)),
    ])

    # One long lower fold and a shorter counter-fold keep negative space around
    # the biological tail and legs. They terminate the same directional gesture
    # instead of becoming a symmetric skirt.
    lower_fold = v1.bone("ouros_resonance_lower_fold", "torso", [-3.8, 14.2, -2.15], [
        mcube((-6.75, 6.35, -2.62), (3.20, 8.65, .38), 82, light=85, dark=81,
              pivot=(-5.05, 13.05, -2.43), rotation=(-7, -4, -13)),
        mcube((-9.15, 9.15, -2.30), (2.05, 5.85, .32), 84, light=85, dark=80,
              pivot=(-7.90, 13.75, -2.14), rotation=(-8, 10, 25)),
    ])

    waist_clasp = v1.bone("ouros_resonance_waist_clasp", "torso", [-4.0, 15.0, -2.2], [
        mcube((-6.55, 13.65, -3.02), (2.55, 1.35, .85), 84, light=85, dark=80,
              pivot=(-5.20, 14.35, -2.60), rotation=(3, 8, 17)),
    ])
    return retained + [shoulder_shell, flank_mantle, lower_fold, waist_clasp]


def build_model() -> int:
    v14.build_model()
    data = json.loads(v1.MODEL.read_text(encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    official = geo["bones"][:v1.OFFICIAL_BONES]
    if len(official) != v1.OFFICIAL_BONES:
        raise SystemExit("official Lucario bone prefix missing")
    extras = v16_bones()
    geo["bones"] = official + extras
    v1.MODEL.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ",")) + "\n", encoding="utf-8")
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
    data["production"]["productionBoneCount"] = v1.OFFICIAL_BONES + 9
    data["production"]["cosmeticBoneCount"] = 9
    data["production"]["cosmeticCubeCount"] = cubes
    next(t for t in data["production"]["textures"] if t["role"] == "OVERLAY")["sha256"] = v1.sha256(v1.OVERLAY)
    for asset in data["production"].get("runtimeAssets", []):
        if asset.get("role") == "RESOLVER":
            asset["sha256"] = v1.sha256(v1.RESOLVER)
    data["builder"]["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v16.py"
    data["builder"]["command"] = ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v16.py"]
    data["qualityIntent"]["signaturePieces"] = [
        "Open cowl/high collar preserving face, ears, aura sensors and chest spike",
        "Camera-near right-shoulder shell flowing into two deeply overlapping flank facets with changing direction and width",
        "Single long lower fold plus short counter-fold, leaving negative space around Lucario's tail and legs",
    ]
    data["qualityIntent"]["macroFormPlan"] = (
        "V16b responds directly to V16 exact-head Blockbench evidence. The rear-centered mantle that disappeared behind the body is removed. The dominant cloth system is rooted to the official camera-near right shoulder and wraps down the near flank using two large overlapping facets, one narrow edge facet, and two unequal lower folds. Greaves are removed to reduce scattered gear. No visual threshold is relaxed."
    )
    data["qualityIntent"]["gameplayReadGoal"] = (
        "At 160 px the first read should gain one unmistakable asymmetric near-side mantle contour from shoulder to below the waist, with the biological head, chest spike, hands, feet and tail still obvious. The 3/4 view must show the signature form directly rather than relying on a rear-only silhouette."
    )
    data["qualityIntent"]["iterationNote"] = (
        "V16 exact-head review on b5d5b982652cbfc4b5037873b6a384f5347af130 failed silhouetteDeltaRatio=0.0265 against the unchanged 0.0400 floor. Direct PNG inspection also showed the back system reading as dark plates and nearly disappearing in hero 3/4. V16b changes placement and composition rather than increasing cube count or lowering thresholds."
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
        "concept": "Aura Sentinel — Resonance Ronin V16b",
        "officialBones": v1.OFFICIAL_BONES,
        "cosmeticBones": 9,
        "cosmeticCubes": cubes,
        "modelSha256": v1.sha256(v1.MODEL),
        "overlaySha256": v1.sha256(v1.OVERLAY),
        "bodyTexelRework": "NONE",
        "visualChange": "camera-near asymmetric shoulder-to-flank mantle; no greaves; unchanged floors",
    }, indent=2))


if __name__ == "__main__":
    main()
