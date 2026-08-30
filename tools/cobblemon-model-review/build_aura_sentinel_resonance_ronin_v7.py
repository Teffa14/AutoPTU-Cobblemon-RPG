#!/usr/bin/env python3
"""Resonance Ronin V7: deliberate silhouette pass after V6 visual-floor failure.

V6 passed source/anatomy/attachment/reproducibility and rendered correctly in
Blockbench, but its matched-camera silhouette delta was only 1.08% against the
4% technical floor. V7 keeps the exact official 87-bone Lucario prefix and
rebuilds only Ouros cosmetics around one authored outer contour: a layered
left-shoulder crescent that becomes a diagonal rear resonance cape and long
rear coat tails. All new mass is thin/overlapping/tapered rather than box armor.
Presentation only; AutoPTU/Ouros remains authoritative for battle state.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V6_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v6.py"
spec = importlib.util.spec_from_file_location("resonance_v6", V6_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V6 builder")
v6 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v6)
v1 = v6.v1
mcube = v6.mcube
write_overlay = v6.write_overlay


def authored_bones() -> list[dict]:
    # Head treatment stays subordinate. The face and aura sensors remain the hero.
    circlet = v1.bone("ouros_resonance_circlet", "head_angle", [0, 38.0, -1.6], [
        mcube((-4.05, 37.72, -4.72), (3.2, .38, .22), 84, light=85, dark=80,
              pivot=(-2.4, 37.92, -4.6), rotation=(0, 0, -10)),
        mcube((.85, 37.72, -4.72), (3.2, .38, .22), 84, light=85, dark=80,
              pivot=(2.4, 37.92, -4.6), rotation=(0, 0, 10)),
        mcube((-2.15, 39.42, 2.68), (4.3, .32, .22), 81, light=82, dark=88),
    ])

    # Signature crescent. The pieces overlap continuously from torso contact to
    # beyond the official arm silhouette. Progressive rotation and shrinking
    # depth create a scalloped/tapered edge rather than a stack of bars.
    shawl = v1.bone("ouros_resonance_shawl", "torso3", [0, 30.0, 0.0], [
        mcube((-6.2, 29.15, -1.55), (5.3, 2.25, 3.5), 81, light=83, dark=88,
              pivot=(-3.55, 30.2, .15), rotation=(4, -7, -12)),
        mcube((-9.8, 29.65, -.95), (5.25, 2.35, 3.05), 82, light=83, dark=81,
              pivot=(-7.15, 30.8, .55), rotation=(5, -12, -23)),
        mcube((-12.85, 30.45, -.25), (4.65, 2.15, 2.55), 81, light=83, dark=88,
              pivot=(-10.5, 31.5, .95), rotation=(6, -17, -34)),
        mcube((-15.25, 31.55, .45), (3.75, 1.85, 2.0), 82, light=83, dark=81,
              pivot=(-13.35, 32.45, 1.4), rotation=(7, -22, -45)),
        # Inner fold connects the crescent to the front sash and makes one system.
        mcube((-5.1, 27.25, -4.0), (7.8, .72, .26), 81, light=83, dark=88,
              pivot=(-1.2, 27.6, -3.87), rotation=(0, 0, -28)),
        # Small right counterform keeps the silhouette intentionally asymmetric.
        mcube((3.6, 29.55, -1.0), (2.5, 1.0, 2.45), 81, light=82, dark=88,
              pivot=(4.85, 30.05, .2), rotation=(2, 7, 12)),
    ])

    # Chest remains almost empty. One sash, one low brace. Biological chest spike
    # and torso continue to carry the species identity.
    cuirass = v1.bone("ouros_resonance_cuirass", "torso3", [0, 27.4, -3.2], [
        mcube((-4.65, 27.0, -4.08), (7.7, .68, .24), 81, light=83, dark=88,
              pivot=(-.8, 27.32, -3.96), rotation=(0, 0, -30)),
        mcube((-2.95, 24.45, -4.09), (5.9, .52, .22), 84, light=85, dark=80),
    ])

    # The former banner group becomes the actual rear resonance cape. Four broad,
    # paper-thin panels overlap diagonally shoulder->hip->outside silhouette.
    # Their outer contour is the V7 signature and intentionally clears x=-13.
    banner = v1.bone("ouros_resonance_banner", "torso3", [-4.6, 29.1, 2.7], [
        mcube((-7.35, 24.6, 2.8), (4.9, 5.0, .34), 81, light=83, dark=88,
              pivot=(-4.9, 28.9, 2.97), rotation=(-12, 7, -10)),
        mcube((-10.45, 20.75, 2.95), (5.0, 5.15, .32), 82, light=83, dark=81,
              pivot=(-7.9, 24.9, 3.12), rotation=(-15, 10, -14)),
        mcube((-12.95, 16.65, 3.1), (4.65, 5.25, .30), 81, light=83, dark=88,
              pivot=(-10.55, 20.9, 3.25), rotation=(-18, 12, -17)),
        mcube((-14.75, 12.6, 3.23), (3.95, 5.05, .28), 82, light=83, dark=81,
              pivot=(-12.75, 16.7, 3.37), rotation=(-20, 14, -20)),
        # Aura edge only along the terminal contour, not as repeated hardware.
        mcube((-14.25, 12.32, 3.45), (3.05, .28, .14), 86, light=87, dark=81,
              pivot=(-12.72, 12.45, 3.52), rotation=(-20, 14, -20)),
    ])

    # Three long rear-biased tails continue the diagonal cape rhythm. They are
    # separated by real air and remain behind the thighs from the hero camera.
    coat = v1.bone("ouros_resonance_coat", "torso", [0, 20.0, 1.0], [
        mcube((-6.6, 8.7, 3.0), (4.0, 10.7, .38), 81, light=83, dark=88,
              pivot=(-4.35, 18.8, 3.18), rotation=(-12, -6, 11)),
        mcube((-1.85, 10.2, 3.12), (3.45, 9.2, .34), 80, light=82, dark=88,
              pivot=(-.15, 18.65, 3.28), rotation=(-13, 2, -2)),
        mcube((2.1, 12.1, 2.92), (3.3, 7.25, .36), 82, light=83, dark=81,
              pivot=(3.65, 18.5, 3.1), rotation=(-10, 5, -12)),
        mcube((-6.15, 8.45, 3.3), (3.15, .3, .15), 86, light=87, dark=81,
              pivot=(-4.55, 8.6, 3.38), rotation=(-12, -6, 11)),
    ])

    # Subordinate articulation-safe accents. Keep them thin and close to biology.
    left_vambrace = v1.bone("ouros_resonance_left_vambrace", "arm_left2", [10.3, 29.4, -.3], [
        mcube((9.0, 27.8, -2.18), (2.55, 2.7, .30), 80, light=82, dark=88,
              pivot=(10.25, 29.15, -2.03), rotation=(0, -3, -5)),
        mcube((9.2, 27.62, -2.32), (2.15, .28, .15), 84, light=85, dark=80,
              pivot=(10.28, 27.76, -2.24), rotation=(0, -3, -5)),
    ])
    right_vambrace = v1.bone("ouros_resonance_right_vambrace", "arm_right2", [-10.3, 29.4, -.3], [
        mcube((-11.55, 27.8, -2.18), (2.5, 2.7, .30), 80, light=82, dark=88,
              pivot=(-10.3, 29.15, -2.03), rotation=(0, 3, 5)),
    ])
    left_greave = v1.bone("ouros_resonance_left_greave", "leg_left4", [3.5, 6.15, -1.5], [
        mcube((1.95, -1.0, -1.98), (2.8, 5.55, .30), 80, light=82, dark=88,
              pivot=(3.35, 1.8, -1.83), rotation=(-7, 0, -4)),
    ])
    right_greave = v1.bone("ouros_resonance_right_greave", "leg_right4", [-3.5, 6.15, -1.5], [
        mcube((-4.75, -1.0, -1.98), (2.8, 5.55, .30), 80, light=82, dark=88,
              pivot=(-3.35, 1.8, -1.83), rotation=(-7, 0, 4)),
    ])
    tail_guard = v1.bone("ouros_resonance_tail_guard", "tail2", [0, 19.4, 10.0], [
        mcube((-1.1, 18.2, 9.35), (2.2, 1.15, .30), 84, light=85, dark=80),
    ])
    return [circlet, shawl, cuirass, banner, coat, left_vambrace, right_vambrace, left_greave, right_greave, tail_guard]


def build_model() -> int:
    # Reconstruct V6's exact official baseline first, then replace ALL cosmetics.
    v6.build_model()
    data = json.loads(v1.MODEL.read_text(encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    official = geo["bones"][:v1.OFFICIAL_BONES]
    if len(official) != v1.OFFICIAL_BONES:
        raise SystemExit("official Lucario bone prefix missing")
    extras = authored_bones()
    geo["bones"] = official + extras
    v1.MODEL.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    return sum(len(b.get("cubes", [])) for b in extras)


def patch_manifest(cubes: int) -> None:
    data = json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["artStatus"] = "ARTISTIC FAIL"
    data["production"]["modelSha256"] = v1.sha256(v1.MODEL)
    data["production"]["cosmeticCubeCount"] = cubes
    overlay = next(t for t in data["production"]["textures"] if t["role"] == "OVERLAY")
    overlay["sha256"] = v1.sha256(v1.OVERLAY)
    data["builder"]["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v7.py"
    data["builder"]["command"] = ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v7.py"]
    data["qualityIntent"]["signaturePieces"] = [
        "Four-stage left shoulder crescent extending materially beyond the official arm silhouette",
        "Four-panel diagonal rear resonance cape from scapula to lower-left outer contour",
        "Three long separated rear coat tails preserving front-leg negative space"
    ]
    data["qualityIntent"]["macroFormPlan"] = (
        "V7 answers the V6 1.08% silhouette-floor failure by moving authored cloth outside the official silhouette rather than thickening armor. "
        "A four-stage rotated shoulder crescent reaches past the official arm envelope, then overlaps into a diagonal rear cape and three long rear tails. "
        "The chest remains almost empty so Lucario's spike, face, aura sensors and athletic body stay unequivocal."
    )
    data["qualityIntent"]["paintPlan"] = (
        "Keep exact official biological normal/shiny bytes. Use the V6 twelve-texel accessory material ramp with dark occlusion faces, lighter facing/top planes, "
        "antique-gold seam accents and aura-cyan terminal edges only on the authored cloth/equipment system."
    )
    data["qualityIntent"]["gameplayReadGoal"] = (
        "Matched-camera 160 px must retain the left shoulder crescent, diagonal cape edge and separated long rear tails as one premium asymmetrical silhouette, "
        "while avoiding backpack, shorts, cage, bar or box-armor reads."
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
        "concept": "Aura Sentinel — Resonance Ronin V7",
        "officialBones": v1.OFFICIAL_BONES,
        "cosmeticBones": 10,
        "cosmeticCubes": cubes,
        "modelSha256": v1.sha256(v1.MODEL),
        "overlaySha256": v1.sha256(v1.OVERLAY),
        "resolverSha256": v1.sha256(v1.RESOLVER),
        "normalBodySha256": v1.sha256(v1.BODY),
        "shinyBodySha256": v1.sha256(v1.SHINY),
        "bodyTexelRework": "NONE",
        "visualChange": "outer shoulder crescent + diagonal rear cape + long separated tails"
    }, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()
