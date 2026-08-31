#!/usr/bin/env python3
"""Resonance Ronin V19: continuous dark shoulder-back-hip wrap.

V18 cleared the unchanged technical visual floors but direct Blockbench review
still read its four-piece shoulder crescent as visible armor facets and its rear
mantle as a dark rectangular plate. V19 removes that entire V18 macro-form and
rebuilds it as one deeper, darker asymmetric material envelope: a compact
shoulder root, three overlapping back/side masses, and two unequal lower folds.
Gold remains a minor seam/clasp accent supplied by the existing authored overlay
instead of carrying the silhouette.

Presentation only. AutoPTU/Ouros remains authoritative for combatants, legality,
HP/status, positions, RNG, damage and tactical outcomes.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V18_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v18.py"
spec = importlib.util.spec_from_file_location("resonance_v18", V18_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V18 builder")
v18 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v18)
v14 = v18.v14
v1 = v18.v1
mcube = v18.mcube
write_overlay = v18.write_overlay

RETAINED = {
    "ouros_resonance_cowl",
    "ouros_resonance_high_collar",
    "ouros_resonance_left_vambrace",
    "ouros_resonance_right_vambrace",
}


def v19_bones() -> list[dict]:
    retained = [b for b in v14.v14_bones() if b["name"] in RETAINED]
    if {b["name"] for b in retained} != RETAINED:
        raise SystemExit("retained cosmetic contract drifted")

    # A real contact/root volume hugs the camera-near right shoulder. Unlike
    # V18's thin gold facets, these have depth and overlap into one dark mass.
    shoulder_wrap = v1.bone("ouros_resonance_shoulder_wrap", "shoulder_right", [-7.0, 30.0, -1.0], [
        mcube((-10.15, 28.15, -3.15), (4.75, 2.65, 1.85), 80, light=82, dark=88,
              pivot=(-7.35, 30.05, -2.10), rotation=(-12, 22, 31)),
        mcube((-10.25, 25.35, -2.55), (4.05, 3.55, 1.25), 80, light=82, dark=88,
              pivot=(-7.65, 28.20, -1.85), rotation=(-9, 27, 20)),
        mcube((-9.15, 22.65, -2.10), (3.05, 3.55, .82), 81, light=83, dark=88,
              pivot=(-7.25, 25.55, -1.62), rotation=(-5, 22, 10)),
    ])

    # The rear system wraps diagonally around the torso with changing depth and
    # width. It is intentionally offset, overlapped, and never forms a centered
    # rectangle. Each successive mass narrows toward the hip.
    back_wrap = v1.bone("ouros_resonance_back_wrap", "torso3", [-4.0, 23.0, 1.4], [
        mcube((-8.75, 21.10, .95), (5.05, 5.10, 1.20), 80, light=83, dark=88,
              pivot=(-6.10, 24.25, 1.62), rotation=(11, -23, 17)),
        mcube((-7.05, 17.05, 1.25), (4.15, 5.05, .88), 81, light=83, dark=88,
              pivot=(-4.95, 20.40, 1.72), rotation=(8, -14, 6)),
        mcube((-5.35, 13.55, 1.15), (3.25, 4.35, .68), 80, light=82, dark=88,
              pivot=(-3.75, 16.65, 1.50), rotation=(4, -5, -7)),
    ])

    # Unequal lower folds continue the same cloth envelope instead of becoming
    # separate armor shorts. The large near fold leans behind the leg; the short
    # counter fold leaves clear negative space for Lucario's biological tail.
    lower_folds = v1.bone("ouros_resonance_lower_folds", "torso", [-3.0, 14.0, 1.2], [
        mcube((-6.55, 7.55, .45), (3.15, 6.70, .58), 80, light=83, dark=88,
              pivot=(-4.55, 13.00, .82), rotation=(-16, 12, 12)),
        mcube((1.15, 10.35, 1.05), (1.95, 4.35, .42), 81, light=83, dark=88,
              pivot=(2.05, 13.55, 1.28), rotation=(-12, -7, -14)),
    ])

    # Small oblique clasp establishes the visible contact transition at the hip;
    # it is an accent, not a silhouette-making slab.
    hip_clasp = v1.bone("ouros_resonance_hip_clasp", "torso", [-3.25, 14.6, -1.5], [
        mcube((-5.20, 13.65, -2.45), (2.15, 1.05, .72), 84, light=85, dark=80,
              pivot=(-4.05, 14.20, -2.05), rotation=(3, 8, -18)),
    ])

    return retained + [shoulder_wrap, back_wrap, lower_folds, hip_clasp]


def build_model() -> int:
    v18.build_model()
    data = json.loads(v1.MODEL.read_text(encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    official = geo["bones"][:v1.OFFICIAL_BONES]
    if len(official) != v1.OFFICIAL_BONES:
        raise SystemExit("official Lucario bone prefix missing")
    extras = v19_bones()
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
    data["production"]["productionBoneCount"] = v1.OFFICIAL_BONES + 8
    data["production"]["cosmeticBoneCount"] = 8
    data["production"]["cosmeticCubeCount"] = cubes
    next(t for t in data["production"]["textures"] if t["role"] == "OVERLAY")["sha256"] = v1.sha256(v1.OVERLAY)
    for asset in data["production"].get("runtimeAssets", []):
        if asset.get("role") == "RESOLVER":
            asset["sha256"] = v1.sha256(v1.RESOLVER)
    data["builder"]["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v19.py"
    data["builder"]["command"] = ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v19.py"]
    data["qualityIntent"]["signaturePieces"] = [
        "Deep camera-near shoulder wrap flowing into one dark diagonal back envelope",
        "Broad-to-narrow torso overlap with changing depth rather than a centered rear plate",
        "Unequal lower cloth folds and a small hip clasp preserving tail/leg negative space",
    ]
    data["qualityIntent"]["macroFormPlan"] = (
        "V19 discards V18's four thin shoulder facets, separate front lapel, rear plate stack and long frontal coat-tail architecture. A compact deep shoulder root now overlaps three progressively narrower dark torso masses and terminates in two unequal lower folds. The shape changes depth and angle as it wraps shoulder-to-back-to-hip, with gold restricted to the small clasp/material accents rather than driving the silhouette."
    )
    data["qualityIntent"]["paintPlan"] = (
        "Keep exact official biological normal/shiny bytes. Retain the authored accessory overlay ramp but use its dark cloth family for the dominant wrap, lighter facing planes for volume separation, and antique-gold only as restrained seam/clasp accents. No third-party palette or texture artwork is reused."
    )
    data["qualityIntent"]["gameplayReadGoal"] = (
        "At 160 px the first read should be a single asymmetric dark mantle wrapping Lucario from the camera-near shoulder through back and hip, with clear body/tail negative space and only restrained gold accents. It must avoid the V18 read of gold armor facets plus a rectangular rear plate."
    )
    data["qualityIntent"]["iterationNote"] = (
        "V18 passed unchanged technical floors at pixelDifferenceRatio 0.105581 and silhouetteDeltaRatio 0.062568 but direct exact-head Blockbench QA remained ARTISTIC FAIL: the gold shoulder system separated into obvious facets, the rear became a dark rectangular mass, and gameplay read remained Lucario plus equipment. V19 replaces those macro-forms rather than adding cubes or lowering thresholds."
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
        "concept": "Aura Sentinel — Resonance Ronin V19",
        "officialBones": v1.OFFICIAL_BONES,
        "cosmeticBones": 8,
        "cosmeticCubes": cubes,
        "modelSha256": v1.sha256(v1.MODEL),
        "overlaySha256": v1.sha256(v1.OVERLAY),
        "bodyTexelRework": "NONE",
        "visualChange": "continuous dark shoulder-back-hip wrap; V18 gold facet/rear-plate architecture removed; unchanged floors",
    }, indent=2))


if __name__ == "__main__":
    main()
