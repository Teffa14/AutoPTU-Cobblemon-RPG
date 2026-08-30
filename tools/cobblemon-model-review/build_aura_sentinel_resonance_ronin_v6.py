#!/usr/bin/env python3
"""Resonance Ronin V6 after opening V5 Blockbench evidence.

V5 still read as backpack + oversized shorts + loose purple blocks. V6 rebuilds
all ten Ouros cosmetic groups into a lighter, motion-rooted ceremonial silhouette:
a thin head crest, a layered left shoulder veil, a diagonal chest sash, a flowing
rear resonance veil, three long coat tails, reduced arm/shin bands, and a small
tail clasp. The exact 87 official Lucario bones remain byte-structure equivalent.
This module is presentation-only; AutoPTU/Ouros remains battle authority.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V5_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v5.py"
spec = importlib.util.spec_from_file_location("resonance_v5", V5_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V5 builder")
v5 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v5)
v1 = v5.v1

PALETTE = {
    80: (14, 18, 32, 255),
    81: (27, 35, 60, 255),
    82: (48, 62, 98, 255),
    83: (91, 111, 145, 255),
    84: (121, 82, 30, 255),
    85: (218, 168, 69, 255),
    86: (22, 111, 139, 255),
    87: (62, 218, 232, 255),
    88: (10, 13, 21, 255),
    89: (35, 42, 55, 255),
    90: (68, 82, 101, 255),
    91: (188, 204, 220, 255),
}


def face_uv(x: int) -> dict:
    return {"uv": [x, 63], "uv_size": [1, 1]}


def mcube(origin, size, mid, *, light=None, dark=None, pivot=None, rotation=None):
    out = v1.cube(origin, size, mid, pivot=pivot, rotation=rotation)
    light = mid if light is None else light
    dark = mid if dark is None else dark
    out["uv"] = {
        "north": face_uv(light), "south": face_uv(mid),
        "east": face_uv(mid), "west": face_uv(dark),
        "up": face_uv(light), "down": face_uv(dark),
    }
    return out


def write_overlay(path: Path) -> None:
    width, height = 128, 64
    pixels = bytearray(width * height * 4)
    for x, rgba in PALETTE.items():
        i = ((63 * width) + x) * 4
        pixels[i:i+4] = bytes(rgba)
    raw = bytearray()
    stride = width * 4
    for y in range(height):
        raw.append(0)
        raw.extend(pixels[y*stride:(y+1)*stride])
    payload = b"\x89PNG\r\n\x1a\n"
    payload += v1.png_chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    payload += v1.png_chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    payload += v1.png_chunk(b"IEND", b"")
    path.write_bytes(payload)


def bones() -> list[dict]:
    # Thin ceremonial brow and rear knot. Keeps Lucario's ears/sensors dominant.
    circlet = v1.bone("ouros_resonance_circlet", "head_angle", [0, 38.0, -1.6], [
        mcube((-4.05, 37.75, -4.72), (3.2, .42, .24), 84, light=85, dark=80,
              pivot=(-2.4, 38.0, -4.6), rotation=(0, 0, -9)),
        mcube((.85, 37.75, -4.72), (3.2, .42, .24), 84, light=85, dark=80,
              pivot=(2.4, 38.0, -4.6), rotation=(0, 0, 9)),
        mcube((-3.55, 39.55, 2.72), (7.1, .38, .28), 81, light=82, dark=88),
        mcube((-.38, 39.7, 2.85), (.76, 2.15, .3), 86, light=87, dark=81,
              pivot=(0, 39.8, 3.0), rotation=(0, 0, -15)),
    ])

    # Signature shoulder veil: overlapping thin planes, progressively smaller and
    # more rotated. It creates one scalloped contour rather than a backpack mass.
    shawl = v1.bone("ouros_resonance_shawl", "torso3", [0, 30.0, 0.0], [
        mcube((-6.4, 29.2, -1.4), (4.8, 1.75, 3.35), 81, light=82, dark=88,
              pivot=(-4.0, 30.1, .25), rotation=(4, -8, -15)),
        mcube((-7.9, 29.8, -.55), (3.8, 1.7, 2.65), 82, light=83, dark=81,
              pivot=(-6.0, 30.65, .75), rotation=(5, -14, -24)),
        mcube((-8.95, 30.8, .25), (2.9, 1.45, 2.05), 81, light=82, dark=88,
              pivot=(-7.5, 31.5, 1.15), rotation=(7, -18, -34)),
        # Rear pieces overlap the shoulder shell and descend in an S-like rhythm.
        mcube((-6.3, 26.3, 2.35), (4.15, 4.0, .36), 80, light=81, dark=88,
              pivot=(-4.25, 29.5, 2.55), rotation=(-13, 8, -12)),
        mcube((-5.85, 22.85, 2.55), (3.45, 3.9, .32), 81, light=82, dark=88,
              pivot=(-4.1, 26.0, 2.72), rotation=(-16, 11, -7)),
        mcube((-5.0, 20.0, 2.72), (2.75, 3.25, .28), 82, light=83, dark=81,
              pivot=(-3.65, 22.7, 2.86), rotation=(-18, 13, -2)),
        # Small right lapel counterweight.
        mcube((3.55, 29.55, -1.15), (2.5, 1.05, 2.55), 81, light=82, dark=88,
              pivot=(4.8, 30.05, .1), rotation=(2, 6, 12)),
    ])

    # A single diagonal sash plus two short lower braces. Chest spike stays open.
    cuirass = v1.bone("ouros_resonance_cuirass", "torso3", [0, 27.4, -3.2], [
        mcube((-4.75, 27.25, -4.06), (8.0, .78, .26), 81, light=82, dark=88,
              pivot=(-.75, 27.62, -3.93), rotation=(0, 0, -29)),
        mcube((-3.3, 24.5, -4.08), (2.7, .62, .25), 84, light=85, dark=80,
              pivot=(-1.95, 24.82, -3.95), rotation=(0, 0, -18)),
        mcube((.55, 24.5, -4.08), (2.7, .62, .25), 84, light=85, dark=80,
              pivot=(1.9, 24.82, -3.95), rotation=(0, 0, 18)),
    ])

    # Former "banner" becomes a thin flowing resonance veil behind the left side.
    banner = v1.bone("ouros_resonance_banner", "torso3", [-4.2, 29.3, 2.7], [
        mcube((-6.2, 24.9, 3.0), (3.5, 4.7, .3), 80, light=81, dark=88,
              pivot=(-4.45, 29.0, 3.15), rotation=(-12, 7, -13)),
        mcube((-6.65, 21.1, 3.12), (3.05, 4.25, .28), 81, light=82, dark=88,
              pivot=(-5.05, 24.7, 3.26), rotation=(-15, 10, -8)),
        mcube((-6.15, 17.75, 3.24), (2.55, 3.75, .25), 82, light=83, dark=81,
              pivot=(-4.85, 20.9, 3.36), rotation=(-18, 12, -3)),
        # One aura-lit edge broken across the contour, not repeated horizontal bars.
        mcube((-6.05, 17.6, 3.45), (2.25, .3, .16), 86, light=87, dark=81,
              pivot=(-4.9, 17.75, 3.53), rotation=(-18, 12, -3)),
    ])

    # Rear-biased long coat tails. Nothing broad covers the front thighs.
    coat = v1.bone("ouros_resonance_coat", "torso", [0, 20.0, 1.0], [
        mcube((-4.9, 18.85, 2.65), (3.7, 1.15, .35), 80, light=81, dark=88,
              pivot=(-3.0, 19.45, 2.82), rotation=(-7, 0, 8)),
        mcube((-4.75, 10.3, 2.8), (3.55, 8.9, .38), 81, light=82, dark=88,
              pivot=(-2.9, 18.9, 2.98), rotation=(-11, -5, 10)),
        mcube((-.9, 11.8, 2.95), (2.9, 7.35, .34), 80, light=81, dark=88,
              pivot=(.45, 18.75, 3.1), rotation=(-12, 2, -2)),
        mcube((2.35, 13.0, 2.75), (2.85, 6.1, .36), 82, light=83, dark=81,
              pivot=(3.7, 18.65, 2.92), rotation=(-9, 5, -11)),
        mcube((-4.4, 10.1, 3.12), (2.8, .32, .16), 86, light=87, dark=81,
              pivot=(-3.0, 10.25, 3.2), rotation=(-11, -5, 10)),
    ])

    left_vambrace = v1.bone("ouros_resonance_left_vambrace", "arm_left2", [10.3, 29.4, -.3], [
        mcube((9.0, 27.85, -2.2), (2.6, 2.8, .34), 80, light=81, dark=88,
              pivot=(10.3, 29.2, -2.0), rotation=(0, -3, -5)),
        mcube((9.25, 27.68, -2.34), (2.1, .3, .18), 84, light=85, dark=80,
              pivot=(10.3, 27.82, -2.25), rotation=(0, -3, -5)),
    ])
    right_vambrace = v1.bone("ouros_resonance_right_vambrace", "arm_right2", [-10.3, 29.4, -.3], [
        mcube((-11.6, 27.85, -2.2), (2.6, 2.8, .34), 80, light=81, dark=88,
              pivot=(-10.3, 29.2, -2.0), rotation=(0, 3, 5)),
    ])
    left_greave = v1.bone("ouros_resonance_left_greave", "leg_left4", [3.5, 6.15, -1.5], [
        mcube((1.9, -1.2, -2.0), (2.9, 5.8, .32), 80, light=81, dark=88,
              pivot=(3.35, 1.8, -1.84), rotation=(-7, 0, -4)),
    ])
    right_greave = v1.bone("ouros_resonance_right_greave", "leg_right4", [-3.5, 6.15, -1.5], [
        mcube((-4.8, -1.2, -2.0), (2.9, 5.8, .32), 80, light=81, dark=88,
              pivot=(-3.35, 1.8, -1.84), rotation=(-7, 0, 4)),
    ])
    tail_guard = v1.bone("ouros_resonance_tail_guard", "tail2", [0, 19.4, 10.0], [
        mcube((-1.15, 18.15, 9.35), (2.3, 1.25, .34), 84, light=85, dark=80),
        mcube((-.32, 18.35, 9.62), (.64, 1.65, .2), 86, light=87, dark=81),
    ])
    return [circlet, shawl, cuirass, banner, coat, left_vambrace, right_vambrace, left_greave, right_greave, tail_guard]


def build_model() -> int:
    # V5 reconstructs from the same exact official prefix, then we discard all
    # V5 Ouros groups and append this complete V6 cosmetic set.
    v5.replace_groups()
    data = json.loads(v1.MODEL.read_text(encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    official = geo["bones"][:v1.OFFICIAL_BONES]
    if len(official) != v1.OFFICIAL_BONES:
        raise SystemExit("official Lucario bone prefix missing")
    extras = bones()
    geo["bones"] = official + extras
    v1.MODEL.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    return sum(len(b.get("cubes", [])) for b in extras)


def patch_manifest(cubes: int) -> None:
    data = json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["artStatus"] = "ARTISTIC FAIL"
    data["production"]["modelSha256"] = v1.sha256(v1.MODEL)
    data["production"]["cosmeticCubeCount"] = cubes
    overlay_entry = next(t for t in data["production"]["textures"] if t["role"] == "OVERLAY")
    overlay_entry["sha256"] = v1.sha256(v1.OVERLAY)
    data["builder"]["scriptPath"] = "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v6.py"
    data["builder"]["command"] = ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v6.py"]
    data["qualityIntent"]["signaturePieces"] = [
        "Scalloped left shoulder veil formed by progressively rotated overlapping shells",
        "Thin resonance veil descending from left scapula to hip as the dominant rear contour",
        "Three rear-biased long coat tails that preserve the front leg silhouette"
    ]
    data["qualityIntent"]["macroFormPlan"] = (
        "V6 removes the V5 backpack and oversized front-short reads. Thin overlapping shoulder shells feed a rear resonance veil, "
        "while the chest uses one diagonal sash and minimal lower braces. Three long rear-biased tails carry the silhouette downward without covering the thighs."
    )
    data["qualityIntent"]["paintPlan"] = (
        "Accessory overlay expands to twelve verified reserved texels with separate shadow, mid and facing values for indigo cloth, lacquer, metal, gold and aura. "
        "Each cosmetic cube assigns darker occluded faces and lighter facing/top planes; biological body pixels remain exact official bytes."
    )
    data["qualityIntent"]["gameplayReadGoal"] = (
        "At 160 px the left shoulder-to-hip veil and three long rear tails must remain legible while the face, chest spike, thighs and biological tail stay unobstructed."
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
        "status": "BUILT", "concept": "Aura Sentinel — Resonance Ronin V6",
        "officialBones": v1.OFFICIAL_BONES, "cosmeticBones": 10,
        "cosmeticCubes": cubes, "modelSha256": v1.sha256(v1.MODEL),
        "overlaySha256": v1.sha256(v1.OVERLAY), "resolverSha256": v1.sha256(v1.RESOLVER),
        "normalBodySha256": v1.sha256(v1.BODY), "shinyBodySha256": v1.sha256(v1.SHINY),
        "bodyTexelRework": "NONE",
        "visualChange": "thin scalloped shoulder veil, rear resonance veil, quiet sash, long rear coat tails, face-oriented material values"
    }, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()
