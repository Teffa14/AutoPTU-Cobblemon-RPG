#!/usr/bin/env python3
"""Build Hitmonlee Cobra Dojo v7 from exact Cobblemon 1.7.3 Hitmonlee.

This refined v7 applies the visual review literally:
- the sleeveless gi rises higher and covers more of the upper torso while preserving a V opening;
- stepped shoulder caps read as martial-arts gi shoulders instead of horizontal boards;
- every telescoping leg segment gets layered wraps, a strike rail and a segmented front kick plate;
- headband, wide belt, split coat tails and an original Ouros cobra-dojo crest remain.

The official 30 bones are copied without edits and remain in original order.
Official biological texture pixels are unchanged; accessory materials occupy
only verified transparent texels on row 63.
"""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
from pathlib import Path
from PIL import Image

PALETTE = {
    "dojo_black": (16, 18, 16, 255),
    "charcoal": (45, 47, 40, 255),
    "gold": (235, 185, 36, 255),
    "gold_dark": (148, 101, 22, 255),
    "wrap": (119, 108, 87, 255),
    "cream": (229, 216, 171, 255),
    "cobra_green": (70, 106, 43, 255),
    "shadow": (8, 9, 8, 255),
    "lacquer": (85, 44, 27, 255),
}
PIXELS = {name: (index, 63) for index, name in enumerate(PALETTE)}
FACES = ("north", "east", "south", "west", "up", "down")


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    h.update(path.read_bytes())
    return h.hexdigest()


def solid_uv(material: str) -> dict:
    x, y = PIXELS[material]
    return {face: {"uv": [x, y], "uv_size": [1, 1]} for face in FACES}


def cube(origin, size, material, **extra) -> dict:
    value = {"origin": origin, "size": size, "uv": solid_uv(material)}
    for key, item in extra.items():
        if item is not None:
            value[key] = item
    return value


def champion_headband() -> dict:
    return {
        "name": "ouros_champion_headband",
        "parent": "torso2",
        "pivot": [0, 22.65, 0],
        "cubes": [
            cube([-4.64, 22.42, -3.72], [9.28, 0.72, 0.24], "dojo_black"),
            cube([-4.64, 22.42, 3.48], [9.28, 0.72, 0.24], "dojo_black"),
            cube([-4.72, 22.42, -3.48], [0.24, 0.72, 6.96], "dojo_black"),
            cube([4.48, 22.42, -3.48], [0.24, 0.72, 6.96], "dojo_black"),
            cube([-1.35, 22.38, -3.94], [2.70, 0.82, 0.20], "gold"),
            cube([-0.34, 22.48, -4.12], [0.68, 0.62, 0.14], "cobra_green"),
            cube([3.05, 18.30, 3.62], [0.72, 4.28, 0.26], "dojo_black", pivot=[3.41, 22.30, 3.75], rotation=[-3, 0, 8]),
            cube([3.92, 19.45, 3.68], [0.52, 3.10, 0.24], "gold_dark", pivot=[4.18, 22.30, 3.80], rotation=[-4, 0, -7]),
        ],
    }


def champion_gi() -> dict:
    """Higher, wider sleeveless wrap gi around the exact 8x5x6 official torso."""
    return {
        "name": "ouros_champion_gi",
        "parent": "torso",
        "pivot": [0, 15.0, 0],
        "cubes": [
            # Wider panels leave a deliberate but much narrower biological V opening.
            cube([-4.34, 11.88, -3.32], [3.14, 6.34, 0.38], "dojo_black", pivot=[-2.78, 15.02, -3.13], rotation=[0, 0, -4]),
            cube([1.20, 11.88, -3.32], [3.14, 6.34, 0.38], "charcoal", pivot=[2.78, 15.02, -3.13], rotation=[0, 0, 4]),
            # Strong high V piping reaches the base of the head but remains below eye level.
            cube([-3.46, 13.72, -3.62], [0.40, 4.82, 0.18], "gold", pivot=[-3.26, 16.08, -3.53], rotation=[0, 0, -34]),
            cube([3.06, 13.72, -3.62], [0.40, 4.82, 0.18], "gold", pivot=[3.26, 16.08, -3.53], rotation=[0, 0, 34]),
            cube([-3.18, 14.04, -3.48], [0.56, 4.24, 0.16], "shadow", pivot=[-2.90, 16.10, -3.40], rotation=[0, 0, -34]),
            cube([2.62, 14.04, -3.48], [0.56, 4.24, 0.16], "shadow", pivot=[2.90, 16.10, -3.40], rotation=[0, 0, 34]),
            # Full side seams and shallow rear panel unify the garment in all views.
            cube([-4.38, 11.82, -2.82], [0.44, 6.36, 5.64], "dojo_black"),
            cube([3.94, 11.82, -2.82], [0.44, 6.36, 5.64], "charcoal"),
            cube([-4.16, 11.88, 3.03], [8.32, 6.30, 0.38], "dojo_black"),
            cube([-3.84, 17.82, 3.40], [7.68, 0.30, 0.18], "gold_dark"),
            # Original chest crest, intentionally small and geometric rather than a copied logo.
            cube([1.62, 14.40, -3.58], [1.56, 1.56, 0.18], "gold_dark", pivot=[2.40, 15.18, -3.49], rotation=[0, 0, 45]),
            cube([2.06, 14.84, -3.80], [0.68, 0.68, 0.16], "cobra_green"),
            cube([1.84, 15.64, -3.76], [1.12, 0.22, 0.14], "gold"),
            # Large original back crest, scaled up to read at gameplay distance.
            cube([-3.28, 12.90, 3.44], [2.94, 0.52, 0.18], "gold", pivot=[-1.81, 13.16, 3.53], rotation=[0, 0, -34]),
            cube([0.34, 12.90, 3.44], [2.94, 0.52, 0.18], "gold", pivot=[1.81, 13.16, 3.53], rotation=[0, 0, 34]),
            cube([-1.02, 13.64, 3.46], [2.04, 2.04, 0.18], "cobra_green", pivot=[0, 14.66, 3.55], rotation=[0, 0, 45]),
            cube([-2.02, 15.48, 3.46], [4.04, 0.38, 0.18], "gold_dark"),
            cube([-0.40, 15.86, 3.48], [0.80, 1.42, 0.16], "gold"),
        ],
    }


def shoulder_guard(name: str, left: bool) -> dict:
    """Stepped cap-sleeve silhouette around the lower sides of Hitmonlee's head."""
    sign = -1 if left else 1
    accent = "gold" if left else "gold_dark"
    material = "dojo_black" if left else "charcoal"
    if left:
        steps = [(-5.40, 17.48, 0.98), (-6.58, 17.02, 1.22), (-7.86, 16.56, 1.30)]
    else:
        steps = [(4.42, 17.48, 0.98), (5.36, 17.02, 1.22), (6.54, 16.56, 1.30)]
    cubes = []
    for x0, y0, width in steps:
        cubes.append(cube([x0, y0, -2.82], [width, 1.18, 5.64], material, pivot=[x0 + width/2, y0 + 0.59, 0], rotation=[0, 0, sign*5]))
        cubes.append(cube([x0 + 0.08, y0 + 1.10, -2.68], [width - 0.16, 0.22, 5.36], accent, pivot=[x0 + width/2, y0 + 1.21, 0], rotation=[0, 0, sign*5]))
    # Dark underside joins the three steps so it reads as one cloth shoulder, not separate bars.
    if left:
        cubes.append(cube([-7.52, 16.30, -2.42], [2.86, 0.42, 4.84], "shadow", pivot=[-6.09, 16.51, 0], rotation=[0, 0, -5]))
    else:
        cubes.append(cube([4.66, 16.30, -2.42], [2.86, 0.42, 4.84], "shadow", pivot=[6.09, 16.51, 0], rotation=[0, 0, 5]))
    return {
        "name": name,
        "parent": "torso2",
        "pivot": [-6.05 if left else 6.05, 17.35, 0],
        "cubes": cubes,
    }


def champion_belt_and_coat() -> dict:
    return {
        "name": "ouros_champion_belt_sash",
        "parent": "torso",
        "pivot": [0, 12.0, 0],
        "cubes": [
            cube([-4.30, 11.55, -3.34], [8.60, 1.10, 0.38], "shadow"),
            cube([-4.30, 11.55, 2.96], [8.60, 1.10, 0.38], "shadow"),
            cube([-4.42, 11.55, -2.96], [0.34, 1.10, 5.92], "shadow"),
            cube([4.08, 11.55, -2.96], [0.34, 1.10, 5.92], "shadow"),
            cube([-1.65, 11.28, -3.78], [3.30, 1.48, 0.46], "gold"),
            cube([-0.46, 10.35, -3.96], [0.92, 1.02, 0.25], "cobra_green"),
            cube([-3.70, 6.10, -3.24], [2.55, 5.62, 0.46], "dojo_black", pivot=[-2.42, 11.45, -3.01], rotation=[-6, 0, 7]),
            cube([1.05, 7.10, -3.22], [2.35, 4.58, 0.44], "charcoal", pivot=[2.22, 11.42, -3.00], rotation=[-6, 0, -6]),
            cube([-3.42, 6.05, -3.45], [1.98, 0.34, 0.18], "gold", pivot=[-2.43, 6.22, -3.36], rotation=[-6, 0, 7]),
            cube([1.28, 7.05, -3.43], [1.86, 0.30, 0.18], "gold_dark", pivot=[2.21, 7.20, -3.34], rotation=[-6, 0, -6]),
            cube([-4.00, 6.45, 3.18], [3.55, 5.38, 0.48], "dojo_black", pivot=[-2.22, 11.55, 3.42], rotation=[-7, 0, 5]),
            cube([0.45, 7.05, 3.18], [3.45, 4.78, 0.48], "charcoal", pivot=[2.18, 11.55, 3.42], rotation=[-7, 0, -5]),
            cube([-3.74, 6.40, 3.68], [3.02, 0.34, 0.18], "gold", pivot=[-2.23, 6.57, 3.77], rotation=[-7, 0, 5]),
            cube([0.70, 7.00, 3.68], [2.95, 0.30, 0.18], "gold_dark", pivot=[2.18, 7.15, 3.77], rotation=[-7, 0, -5]),
        ],
    }


def forearm_guard(name: str, parent: str, left: bool) -> dict:
    x0, x1 = ((9.25, 13.65) if left else (-13.65, -9.25))
    accent = "gold" if left else "gold_dark"
    return {
        "name": name,
        "parent": parent,
        "pivot": [(x0 + x1) / 2, 20.45, 0],
        "cubes": [
            cube([x0, 19.45, -1.10], [x1 - x0, 0.32, 2.20], "wrap"),
            cube([x0, 21.08, -1.10], [x1 - x0, 0.32, 2.20], "cream"),
            cube([x0 + 0.72, 20.08, -1.40], [x1 - x0 - 1.44, 0.34, 0.20], accent),
        ],
    }


def kick_guard(name: str, parent: str, x0: float, y0: float, left: bool, accent: str) -> dict:
    """Layered cloth plus a segmented front/outer kick guard for one telescoping segment."""
    outer_x = x0 + (2.76 if left else -0.04)
    strap_rotation = -14 if left else 14
    return {
        "name": name,
        "parent": parent,
        "pivot": [x0 + 1.5, y0 + 1.5, -1.5],
        "cubes": [
            cube([x0 + 0.08, y0 + 2.34, -1.74], [2.84, 0.28, 0.22], "cream"),
            cube([x0 + 0.18, y0 + 0.42, -1.74], [2.64, 0.30, 0.22], "wrap"),
            cube([outer_x, y0 + 0.30, -1.84], [0.30, 2.34, 0.36], "dojo_black"),
            cube([outer_x + 0.05, y0 + 0.66, -2.10], [0.20, 1.62, 0.16], accent),
            cube([x0 + 0.40, y0 + 1.22, -1.96], [2.18, 0.26, 0.16], "shadow", pivot=[x0 + 1.49, y0 + 1.35, -1.88], rotation=[0, 0, strap_rotation]),
            # Central segmented pad aligns across all three telescoping bones.
            cube([x0 + 0.82, y0 + 0.82, -1.94], [1.36, 1.34, 0.20], "dojo_black"),
            cube([x0 + 0.94, y0 + 0.72, -2.14], [1.12, 0.18, 0.14], accent),
        ],
    }


def foot_guard(name: str, parent: str, x0: float, left: bool, accent: str) -> dict:
    return {
        "name": name,
        "parent": parent,
        "pivot": [x0 + 2.0, 1.0, -0.5],
        "cubes": [
            cube([x0 + 0.12, 1.46, -3.18], [3.76, 0.34, 3.22], "dojo_black"),
            cube([x0 + 0.38, 1.78, -3.24], [3.24, 0.20, 0.20], accent),
            cube([x0 + (3.52 if left else 0.18), 0.52, -2.72], [0.24, 1.18, 2.38], accent),
            cube([x0 + 0.54, 1.12, -3.42], [2.92, 0.22, 0.18], "cream"),
        ],
    }


def cosmetic_bones() -> list[dict]:
    return [
        champion_headband(),
        champion_gi(),
        shoulder_guard("ouros_champion_left_shoulder", True),
        shoulder_guard("ouros_champion_right_shoulder", False),
        champion_belt_and_coat(),
        forearm_guard("ouros_champion_left_forearm", "arm_left2", True),
        forearm_guard("ouros_champion_right_forearm", "arm_right2", False),
        kick_guard("ouros_champion_left_leg2", "leg_left2", 1.0, 7.5, True, "gold"),
        kick_guard("ouros_champion_left_leg3", "leg_left3", 1.0, 4.5, True, "gold_dark"),
        kick_guard("ouros_champion_left_leg4", "leg_left4", 1.0, 1.5, True, "gold"),
        kick_guard("ouros_champion_right_leg2", "leg_right2", -4.0, 7.5, False, "gold_dark"),
        kick_guard("ouros_champion_right_leg3", "leg_right3", -4.0, 4.5, False, "gold"),
        kick_guard("ouros_champion_right_leg4", "leg_right4", -4.0, 1.5, False, "gold_dark"),
        foot_guard("ouros_champion_left_foot", "foot_left", 0.5, True, "gold"),
        foot_guard("ouros_champion_right_foot", "foot_right", -4.5, False, "gold_dark"),
    ]


def derive_model(source: Path, destination: Path) -> None:
    data = json.loads(source.read_text(encoding="utf-8"))
    geometry = data["minecraft:geometry"][0]
    original = copy.deepcopy(geometry["bones"])
    if len(original) != 30:
        raise SystemExit(f"expected 30 official Hitmonlee bones, got {len(original)}")
    geometry["description"]["identifier"] = "geometry.ouros_cobra_dojo_hitmonlee"
    geometry["bones"] = original + cosmetic_bones()
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(data, separators=(",", ":")) + "\n", encoding="utf-8")


def derive_texture(source: Path, destination: Path) -> None:
    image = Image.open(source).convert("RGBA")
    if image.size != (64, 64):
        raise SystemExit(f"unexpected Hitmonlee texture size {image.size}")
    for _, (x, y) in PIXELS.items():
        if image.getpixel((x, y))[3] != 0:
            raise SystemExit(f"reserved material texel {(x, y)} is not transparent in official texture")
    for name, (x, y) in PIXELS.items():
        image.putpixel((x, y), PALETTE[name])
    destination.parent.mkdir(parents=True, exist_ok=True)
    image.save(destination, optimize=True)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", required=True, type=Path)
    parser.add_argument("--normal", required=True, type=Path)
    parser.add_argument("--shiny", required=True, type=Path)
    parser.add_argument("--output-root", required=True, type=Path)
    args = parser.parse_args()

    model_out = args.output_root / "ouros_cobra_dojo_hitmonlee.geo.json"
    normal_out = args.output_root / "ouros_cobra_dojo_hitmonlee.png"
    shiny_out = args.output_root / "ouros_cobra_dojo_hitmonlee_shiny.png"
    derive_model(args.model, model_out)
    derive_texture(args.normal, normal_out)
    derive_texture(args.shiny, shiny_out)

    cosmetics = cosmetic_bones()
    report = {
        "modelSha256": sha256(model_out),
        "normalSha256": sha256(normal_out),
        "shinySha256": sha256(shiny_out),
        "originalBones": 30,
        "derivedBones": 30 + len(cosmetics),
        "cosmeticBones": len(cosmetics),
        "cosmeticCubes": sum(len(b.get("cubes", [])) for b in cosmetics),
        "cosmeticNames": [b["name"] for b in cosmetics],
        "palettePixels": PIXELS,
        "bodyTexturePolicy": "official biological texels unchanged; only verified transparent y=63 accessory swatches added",
        "artDirection": "refined v7: higher/wider gi + stepped cap sleeves + layered segmented kick plates",
    }
    (args.output_root / "build-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
