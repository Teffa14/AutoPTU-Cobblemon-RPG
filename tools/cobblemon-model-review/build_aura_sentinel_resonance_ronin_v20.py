#!/usr/bin/env python3
"""Resonance Ronin V20: one asymmetric half-cloak macro-form.

V19 proved that moving the costume inward solved some plate noise but also hid the
transformation inside Lucario's official silhouette (matched-camera pixel delta
0.0614 vs the unchanged 0.0800 floor). V20 removes the scattered vambraces and
rebuilds the dominant system as one camera-near half-cloak: an attached shoulder
root, three heavily-overlapped tapered drape masses, a narrow back handoff, and
unequal lower folds. The material stays dark; gold remains a small clasp/accent.

Presentation only. AutoPTU/Ouros remains authoritative for tactical battle facts.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V19_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v19.py"
spec = importlib.util.spec_from_file_location("resonance_v19", V19_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V19 builder")
v19 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v19)
v14 = v19.v14
v1 = v19.v1
mcube = v19.mcube
write_overlay = v19.write_overlay

RETAINED = {"ouros_resonance_cowl", "ouros_resonance_high_collar"}


def v20_bones() -> list[dict]:
    retained = [b for b in v14.v14_bones() if b["name"] in RETAINED]
    if {b["name"] for b in retained} != RETAINED:
        raise SystemExit("retained cosmetic contract drifted")

    # One attached half-cloak. The pieces overlap deeply, change all three
    # dimensions, and rotate progressively so the eye reads one tapering mass
    # rather than four disconnected plates.
    half_cloak = v1.bone("ouros_resonance_half_cloak", "shoulder_right", [-7.2, 29.9, -1.0], [
        mcube((-11.75, 27.65, -3.35), (6.20, 3.30, 2.05), 80, light=82, dark=88,
              pivot=(-7.75, 29.85, -2.15), rotation=(-13, 24, 34)),
        mcube((-13.35, 24.25, -2.85), (5.75, 4.65, 1.45), 80, light=82, dark=88,
              pivot=(-9.10, 27.55, -2.05), rotation=(-12, 29, 23)),
        mcube((-13.70, 20.30, -2.35), (4.85, 5.20, 1.05), 81, light=83, dark=88,
              pivot=(-10.05, 24.25, -1.82), rotation=(-8, 24, 11)),
        mcube((-12.95, 16.45, -1.85), (3.70, 5.15, .78), 80, light=82, dark=88,
              pivot=(-10.15, 20.55, -1.48), rotation=(-4, 17, -2)),
    ])

    # Narrow dorsal handoff prevents the cloak from looking pasted to only one
    # camera angle. It crosses toward the spine without becoming a rear plate.
    back_handoff = v1.bone("ouros_resonance_back_handoff", "torso3", [-4.5, 24.5, 1.3], [
        mcube((-8.75, 22.15, .75), (4.55, 4.35, 1.05), 80, light=83, dark=88,
              pivot=(-6.15, 25.15, 1.30), rotation=(10, -20, 14)),
        mcube((-6.15, 18.75, 1.05), (3.55, 4.20, .72), 81, light=83, dark=88,
              pivot=(-4.45, 21.70, 1.40), rotation=(7, -10, 3)),
    ])

    # Two different lengths give the lower silhouette a clear cut-out around the
    # official tail/legs instead of producing shorts or a centered skirt block.
    lower_folds = v1.bone("ouros_resonance_lower_folds_v20", "torso", [-3.6, 14.6, .9], [
        mcube((-7.20, 7.10, .20), (3.35, 7.35, .62), 80, light=83, dark=88,
              pivot=(-5.05, 13.15, .58), rotation=(-17, 13, 11)),
        mcube((.85, 10.55, .75), (1.85, 3.95, .40), 81, light=83, dark=88,
              pivot=(1.70, 13.45, .98), rotation=(-11, -8, -15)),
    ])

    hip_clasp = v1.bone("ouros_resonance_hip_clasp_v20", "torso", [-3.4, 14.8, -1.45], [
        mcube((-5.30, 13.75, -2.40), (2.10, 1.00, .70), 84, light=85, dark=80,
              pivot=(-4.20, 14.25, -2.02), rotation=(3, 8, -19)),
    ])

    return retained + [half_cloak, back_handoff, lower_folds, hip_clasp]


def build_model() -> int:
    v19.build_model()
    data = json.loads(v1.MODEL.read_text(encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    official = geo["bones"][:v1.OFFICIAL_BONES]
    if len(official) != v1.OFFICIAL_BONES:
        raise SystemExit("official Lucario bone prefix missing")
    extras = v20_bones()
    geo["bones"] = official + extras
    v1.MODEL.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    return sum(len(b.get("cubes", [])) for b in extras)


def patch_manifest(cubes: int) -> None:
    data = json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["artStatus"] = "ARTISTIC FAIL"
    data["ownerApproval"] = {"required": True, "approved": False, "approvedHeadSha": None,
                             "evidenceSetSha256": None, "approvalRecord": None}
    data["production"]["modelSha256"] = v1.sha256(v1.MODEL)
    data["production"]["productionBoneCount"] = v1.OFFICIAL_BONES + 6
    data["production"]["cosmeticBoneCount"] = 6
    data["production"]["cosmeticCubeCount"] = cubes
    next(t for t in data["production"]["textures"] if t["role"] == "OVERLAY")["sha256"] = v1.sha256(v1.OVERLAY)
    for asset in data["production"].get("runtimeAssets", []):
        if asset.get("role") == "RESOLVER":
            asset["sha256"] = v1.sha256(v1.RESOLVER)
    data["builder"]["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v20.py"
    data["builder"]["command"] = ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v20.py"]
    data["qualityIntent"]["signaturePieces"] = [
        "Single deep dark half-cloak rooted on the camera-near shoulder and tapering down the flank",
        "Narrow diagonal dorsal handoff that turns the same material around the torso without forming a rear plate",
        "Unequal lower folds framing tail and legs with deliberate negative space",
    ]
    data["qualityIntent"]["macroFormPlan"] = (
        "V20 removes V19's two scattered vambraces and replaces its mostly internal shoulder/back masses with one attached half-cloak. Four deeply overlapping volumes project beyond the camera-near shoulder and taper in width, depth and angle toward the hip; a two-piece dorsal handoff continues the material around the back without centering it. Two unequal lower folds complete the envelope while leaving the biological tail and legs open."
    )
    data["qualityIntent"]["paintPlan"] = (
        "Keep exact official biological normal/shiny bytes. Use the existing authored overlay's dark cloth ramp across the dominant cloak, lighter facing planes for volume separation, deep occlusion faces at overlaps, and antique-gold only on the compact hip clasp/seams."
    )
    data["qualityIntent"]["gameplayReadGoal"] = (
        "At 160 px the costume should read first as one asymmetric dark half-cloak with a clear sloped outer contour, rather than Lucario plus small black accessories. The face, ears, aura sensors, chest spike, hands, feet and biological tail stay readable."
    )
    data["qualityIntent"]["iterationNote"] = (
        "V19 exact-head Blockbench preserved anatomy/attachment and removed V18's gold armor-facet read, but failed the unchanged pixel floor at 0.0614 vs 0.0800 because the wrap stayed almost entirely inside the official silhouette. Direct contact-sheet QA confirmed weak 3/4 first read and blocky rear shoulders. V20 moves authored volume into one tapered lateral cloak without lowering thresholds."
    )
    v1.MANIFEST.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(); parser.add_argument("--bootstrap", action="store_true"); args = parser.parse_args()
    if v1.sha256(v1.BODY) != v1.OFFICIAL_NORMAL_SHA256: raise SystemExit("normal body texture drifted")
    if v1.sha256(v1.SHINY) != v1.OFFICIAL_SHINY_SHA256: raise SystemExit("shiny body texture drifted")
    cubes = build_model(); write_overlay(v1.OVERLAY); v1.build_resolver()
    if args.bootstrap: patch_manifest(cubes)
    print(json.dumps({
        "status":"BUILT","concept":"Aura Sentinel — Resonance Ronin V20",
        "officialBones":v1.OFFICIAL_BONES,"cosmeticBones":6,"cosmeticCubes":cubes,
        "modelSha256":v1.sha256(v1.MODEL),"overlaySha256":v1.sha256(v1.OVERLAY),
        "bodyTexelRework":"NONE",
        "visualChange":"single tapered dark half-cloak; scattered vambraces removed; unchanged visual floors"
    }, indent=2))

if __name__ == "__main__": main()
