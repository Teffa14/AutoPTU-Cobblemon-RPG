#!/usr/bin/env python3
"""Resonance Ronin V8: continuous drape pass after V7 visual QA rejection.

V7 cleared the technical silhouette floor but its lower panels read as blue shorts
and its outer crescent fragmented into detached-looking diamonds. V8 removes all
thigh-covering coat geometry and rebuilds the signature as one strongly-overlapped
shoulder-to-side drape with a single continuous outer edge. Official Lucario's
87 biological bones remain unchanged and in order. Presentation only.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V7_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v7.py"
spec = importlib.util.spec_from_file_location("resonance_v7", V7_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V7 builder")
v7 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v7)
v6 = v7.v6
v1 = v7.v1
mcube = v6.mcube
write_overlay = v6.write_overlay


def v8_bones() -> list[dict]:
    circlet = v1.bone("ouros_resonance_circlet", "head_angle", [0, 38.0, -1.6], [
        mcube((-4.0, 37.72, -4.7), (3.1, .34, .20), 84, light=85, dark=80,
              pivot=(-2.4, 37.9, -4.6), rotation=(0, 0, -9)),
        mcube((.9, 37.72, -4.7), (3.1, .34, .20), 84, light=85, dark=80,
              pivot=(2.4, 37.9, -4.6), rotation=(0, 0, 9)),
    ])

    # One continuous drape. Every large panel overlaps the next by >1 unit in
    # bind space. Broad surfaces are paper-thin and progressively taper in width.
    # The visible contour is a single diagonal edge, not a chain of ornaments.
    mantle = v1.bone("ouros_resonance_mantle", "torso3", [0, 29.5, 1.5], [
        # shoulder root wraps from biological torso into the cape
        mcube((-7.2, 28.7, -1.0), (6.0, 2.6, 3.6), 82, light=83, dark=81,
              pivot=(-4.15, 30.0, .7), rotation=(3, -7, -12)),
        # upper drape overlaps root deeply and moves outward/back
        mcube((-8.6, 24.1, 2.35), (6.2, 5.9, .42), 81, light=83, dark=88,
              pivot=(-5.45, 29.2, 2.56), rotation=(-10, 6, -7)),
        # middle drape overlaps upper by ~1.7 y and ~2.6 x
        mcube((-11.0, 19.3, 2.62), (6.0, 6.5, .40), 82, light=83, dark=81,
              pivot=(-7.95, 24.8, 2.82), rotation=(-12, 8, -10)),
        # lower drape overlaps middle and tapers instead of breaking away
        mcube((-12.8, 14.45, 2.9), (5.1, 6.3, .36), 81, light=83, dark=88,
              pivot=(-10.2, 19.7, 3.08), rotation=(-14, 10, -12)),
        # terminal taper; still overlaps lower, gives a deliberate pointed finish
        mcube((-13.45, 10.8, 3.15), (3.65, 4.7, .32), 82, light=83, dark=81,
              pivot=(-11.6, 14.7, 3.3), rotation=(-16, 12, -14)),
        # restrained aura-lit terminal seam only
        mcube((-13.0, 10.55, 3.38), (2.8, .28, .14), 86, light=87, dark=81,
              pivot=(-11.6, 10.7, 3.45), rotation=(-16, 12, -14)),
    ])

    # Minimal front treatment: a diagonal ceremonial sash plus slim waist obi.
    sash = v1.bone("ouros_resonance_sash", "torso3", [0, 27.0, -3.4], [
        mcube((-4.8, 27.0, -4.08), (8.0, .70, .24), 81, light=83, dark=88,
              pivot=(-.8, 27.35, -3.96), rotation=(0, 0, -30)),
        mcube((-3.1, 22.95, -3.62), (6.2, .58, .55), 84, light=85, dark=80),
    ])

    left_vambrace = v1.bone("ouros_resonance_left_vambrace", "arm_left2", [10.3, 29.4, -.3], [
        mcube((9.05, 27.85, -2.16), (2.5, 2.55, .28), 80, light=82, dark=88,
              pivot=(10.3, 29.1, -2.02), rotation=(0, -3, -5)),
    ])
    right_vambrace = v1.bone("ouros_resonance_right_vambrace", "arm_right2", [-10.3, 29.4, -.3], [
        mcube((-11.55, 27.85, -2.16), (2.5, 2.55, .28), 80, light=82, dark=88,
              pivot=(-10.3, 29.1, -2.02), rotation=(0, 3, 5)),
    ])
    left_greave = v1.bone("ouros_resonance_left_greave", "leg_left4", [3.5, 6.15, -1.5], [
        mcube((2.0, -.9, -1.98), (2.7, 5.35, .28), 80, light=82, dark=88,
              pivot=(3.35, 1.8, -1.84), rotation=(-7, 0, -4)),
    ])
    right_greave = v1.bone("ouros_resonance_right_greave", "leg_right4", [-3.5, 6.15, -1.5], [
        mcube((-4.7, -.9, -1.98), (2.7, 5.35, .28), 80, light=82, dark=88,
              pivot=(-3.35, 1.8, -1.84), rotation=(-7, 0, 4)),
    ])
    tail_clasp = v1.bone("ouros_resonance_tail_clasp", "tail2", [0, 19.4, 10.0], [
        mcube((-1.05, 18.25, 9.35), (2.1, 1.05, .28), 84, light=85, dark=80),
    ])
    return [circlet, mantle, sash, left_vambrace, right_vambrace, left_greave, right_greave, tail_clasp]


def build_model() -> int:
    v7.build_model()
    data = json.loads(v1.MODEL.read_text(encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    official = geo["bones"][:v1.OFFICIAL_BONES]
    if len(official) != v1.OFFICIAL_BONES:
        raise SystemExit("official Lucario bone prefix missing")
    extras = v8_bones()
    geo["bones"] = official + extras
    v1.MODEL.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    return sum(len(b.get("cubes", [])) for b in extras)


def patch_manifest(cubes: int) -> None:
    data = json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["artStatus"] = "ARTISTIC FAIL"
    data["production"]["modelSha256"] = v1.sha256(v1.MODEL)
    data["production"]["productionBoneCount"] = v1.OFFICIAL_BONES + 8
    data["production"]["cosmeticBoneCount"] = 8
    data["production"]["cosmeticCubeCount"] = cubes
    overlay = next(t for t in data["production"]["textures"] if t["role"] == "OVERLAY")
    overlay["sha256"] = v1.sha256(v1.OVERLAY)
    data["builder"]["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v8.py"
    data["builder"]["command"] = ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v8.py"]
    data["qualityIntent"]["signaturePieces"] = [
        "Single five-stage shoulder-to-side resonance drape with deep overlaps and one continuous diagonal contour",
        "Minimal diagonal chest sash preserving the biological chest spike and abdomen",
        "Thin articulation-safe arm, shin and tail accents subordinate to the drape"
    ]
    data["qualityIntent"]["macroFormPlan"] = (
        "V8 removes every thigh-covering coat panel and every separated outer-crescent fragment seen in V7. "
        "The transformation is carried by one paper-thin, deeply overlapping shoulder-to-side drape whose panels progressively narrow toward a terminal point. "
        "Front anatomy remains nearly unobstructed and there is no skirt, shorts, backpack, cage, repeated bar or armor-shell system."
    )
    data["qualityIntent"]["gameplayReadGoal"] = (
        "At 160 px the candidate must read as Lucario carrying one authored asymmetrical ceremonial drape, not Lucario wearing blue shorts or floating tiles. "
        "The drape must remain a single connected silhouette in front, back and three-quarter evidence."
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
        "status":"BUILT","concept":"Aura Sentinel — Resonance Ronin V8",
        "officialBones":v1.OFFICIAL_BONES,"cosmeticBones":8,"cosmeticCubes":cubes,
        "modelSha256":v1.sha256(v1.MODEL),"overlaySha256":v1.sha256(v1.OVERLAY),
        "resolverSha256":v1.sha256(v1.RESOLVER),"normalBodySha256":v1.sha256(v1.BODY),
        "shinyBodySha256":v1.sha256(v1.SHINY),"bodyTexelRework":"NONE",
        "visualChange":"continuous shoulder-to-side drape; all shorts/skirt geometry removed"
    }, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()
