#!/usr/bin/env python3
"""Hitmonlee Cobra Dojo v5.

Art correction after rejecting v4 in real Blockbench review:
- preserve all 30 official Hitmonlee bones JSON-equivalent and ordered;
- preserve every official biological texture pixel;
- replace vertical 'antenna' mantle with a broad stepped cobra hood that reads laterally;
- leave the face and central biological chest open;
- use narrow V lapels, side/rear gi cloth, broad belt and asymmetric sash;
- keep leg equipment longitudinal and sparse so Hitmonlee's telescoping legs remain legible.
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
    "charcoal": (44, 46, 39, 255),
    "gold": (232, 184, 42, 255),
    "gold_dark": (145, 99, 22, 255),
    "wrap": (116, 106, 86, 255),
    "cream": (226, 214, 170, 255),
    "cobra_green": (70, 105, 43, 255),
    "lacquer": (82, 43, 26, 255),
    "shadow": (8, 9, 8, 255),
}
PIXELS = {name: (i, 63) for i, name in enumerate(PALETTE)}
FACES = ("north", "east", "south", "west", "up", "down")


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    h.update(path.read_bytes())
    return h.hexdigest()


def solid_uv(material: str) -> dict:
    x, y = PIXELS[material]
    return {f: {"uv": [x, y], "uv_size": [1, 1]} for f in FACES}


def cube(origin, size, material, **extra) -> dict:
    value = {"origin": origin, "size": size, "uv": solid_uv(material)}
    for key, item in extra.items():
        if item is not None:
            value[key] = item
    return value


def cobra_hood() -> dict:
    """A continuous stepped cobra hood behind and to the sides of the head."""
    return {
        "name": "ouros_cobra_mantle",
        "parent": "torso2",
        "pivot": [0, 19.5, 3.1],
        "cubes": [
            # Rear bridge binds both hood halves into one garment.
            cube([-4.55, 16.20, 3.20], [9.10, 1.15, 0.55], "shadow"),
            cube([-3.90, 17.10, 3.42], [7.80, 0.34, 0.25], "gold_dark"),

            # LEFT HOOD: overlapping horizontal steps widen around eye/head height.
            cube([-7.20, 16.35, 2.82], [2.75, 2.20, 0.88], "dojo_black", pivot=[-5.75, 17.45, 3.26], rotation=[0, 0, -5]),
            cube([-8.65, 18.05, 2.84], [4.15, 2.35, 0.88], "charcoal", pivot=[-6.55, 19.22, 3.28], rotation=[0, 0, -7]),
            cube([-9.75, 19.95, 2.86], [5.15, 2.60, 0.88], "dojo_black", pivot=[-7.15, 21.25, 3.30], rotation=[0, 0, -8]),
            cube([-8.95, 22.05, 2.88], [4.25, 1.70, 0.86], "charcoal", pivot=[-6.80, 22.90, 3.31], rotation=[0, 0, -6]),
            # Gold perimeter makes the hood read as one intentional silhouette.
            cube([-7.05, 16.48, 3.68], [2.45, 0.28, 0.18], "gold_dark", pivot=[-5.80, 16.62, 3.77], rotation=[0, 0, -5]),
            cube([-8.38, 18.18, 3.70], [3.58, 0.28, 0.18], "gold", pivot=[-6.58, 18.32, 3.79], rotation=[0, 0, -7]),
            cube([-9.42, 20.10, 3.72], [4.55, 0.30, 0.18], "gold_dark", pivot=[-7.14, 20.25, 3.81], rotation=[0, 0, -8]),
            cube([-8.60, 23.34, 3.70], [3.65, 0.30, 0.18], "gold", pivot=[-6.78, 23.49, 3.79], rotation=[0, 0, -6]),
            # Large green hood 'eye' inset, original Ouros motif rather than third-party logo.
            cube([-8.10, 20.65, 3.76], [2.10, 1.15, 0.20], "cobra_green", pivot=[-7.05, 21.22, 3.86], rotation=[0, 0, -8]),
            cube([-7.48, 21.00, 3.98], [0.58, 0.48, 0.15], "gold", pivot=[-7.19, 21.24, 4.05], rotation=[0, 0, 37]),

            # RIGHT HOOD: slightly smaller and offset for premium asymmetry.
            cube([4.45, 16.55, 2.82], [2.55, 2.05, 0.88], "dojo_black", pivot=[5.75, 17.57, 3.26], rotation=[0, 0, 4]),
            cube([4.55, 18.20, 2.84], [3.80, 2.25, 0.88], "charcoal", pivot=[6.45, 19.32, 3.28], rotation=[0, 0, 6]),
            cube([4.65, 20.00, 2.86], [4.75, 2.45, 0.88], "dojo_black", pivot=[7.02, 21.22, 3.30], rotation=[0, 0, 7]),
            cube([4.75, 22.00, 2.88], [3.95, 1.60, 0.86], "charcoal", pivot=[6.72, 22.80, 3.31], rotation=[0, 0, 5]),
            cube([4.60, 16.66, 3.68], [2.25, 0.26, 0.18], "gold", pivot=[5.74, 16.79, 3.77], rotation=[0, 0, 4]),
            cube([4.78, 18.32, 3.70], [3.25, 0.28, 0.18], "gold_dark", pivot=[6.40, 18.46, 3.79], rotation=[0, 0, 6]),
            cube([4.95, 20.13, 3.72], [4.12, 0.30, 0.18], "gold", pivot=[7.01, 20.28, 3.81], rotation=[0, 0, 7]),
            cube([5.00, 23.18, 3.70], [3.40, 0.28, 0.18], "gold_dark", pivot=[6.70, 23.32, 3.79], rotation=[0, 0, 5]),
            cube([5.95, 20.62, 3.76], [1.92, 1.05, 0.20], "cobra_green", pivot=[6.91, 21.14, 3.86], rotation=[0, 0, 7]),
            cube([6.53, 20.94, 3.98], [0.54, 0.46, 0.15], "gold_dark", pivot=[6.80, 21.17, 4.05], rotation=[0, 0, -37]),

            # Central spine and two low forward fangs at shoulder level. Face stays unobstructed.
            cube([-0.38, 17.55, 3.82], [0.76, 4.65, 0.24], "gold"),
            cube([-1.10, 19.30, 3.86], [2.20, 0.48, 0.20], "cobra_green", pivot=[0, 19.54, 3.96], rotation=[0, 0, 45]),
            cube([-5.25, 16.60, -3.70], [2.35, 0.42, 0.26], "gold", pivot=[-4.08, 16.81, -3.57], rotation=[0, 0, -18]),
            cube([2.90, 16.60, -3.70], [2.35, 0.42, 0.26], "gold_dark", pivot=[4.08, 16.81, -3.57], rotation=[0, 0, 18]),
        ],
    }


def open_gi() -> dict:
    """Narrow lapels and side/rear cloth; central chest remains visibly Hitmonlee."""
    return {
        "name": "ouros_cobra_gi",
        "parent": "torso2",
        "pivot": [0, 15.0, 0],
        "cubes": [
            cube([-4.18, 12.35, -3.72], [1.55, 5.20, 0.34], "dojo_black", pivot=[-3.40, 14.95, -3.55], rotation=[0, 0, -12]),
            cube([2.63, 12.35, -3.72], [1.55, 5.20, 0.34], "charcoal", pivot=[3.40, 14.95, -3.55], rotation=[0, 0, 12]),
            cube([-3.95, 16.95, -3.94], [2.10, 0.34, 0.20], "gold", pivot=[-2.90, 17.12, -3.84], rotation=[0, 0, -30]),
            cube([1.85, 16.95, -3.94], [2.10, 0.34, 0.20], "gold_dark", pivot=[2.90, 17.12, -3.84], rotation=[0, 0, 30]),
            # Side cloth gives depth without hiding the chest.
            cube([-4.68, 12.05, -2.75], [0.38, 5.80, 5.55], "dojo_black"),
            cube([4.30, 12.05, -2.75], [0.38, 5.80, 5.55], "charcoal"),
            # Rear split coat is the main garment mass below the cobra hood.
            cube([-4.24, 11.65, 3.28], [3.78, 6.15, 0.36], "dojo_black", pivot=[-2.35, 17.10, 3.46], rotation=[-4, 0, 4]),
            cube([0.46, 12.10, 3.28], [3.78, 5.70, 0.36], "charcoal", pivot=[2.35, 17.10, 3.46], rotation=[-4, 0, -4]),
            cube([-4.05, 11.58, 3.66], [3.42, 0.30, 0.18], "gold", pivot=[-2.34, 11.73, 3.75], rotation=[-4, 0, 4]),
            cube([0.65, 12.03, 3.66], [3.42, 0.28, 0.18], "gold_dark", pivot=[2.36, 12.17, 3.75], rotation=[-4, 0, -4]),
            # Broad sleeveless shoulder caps link the gi to the hood.
            cube([-6.45, 16.85, -2.20], [2.15, 0.86, 4.55], "dojo_black", pivot=[-5.38, 17.28, 0.08], rotation=[0, 0, -9]),
            cube([4.30, 17.05, -2.05], [1.90, 0.74, 4.30], "charcoal", pivot=[5.25, 17.42, 0.10], rotation=[0, 0, 8]),
            cube([-6.45, 17.57, -2.12], [2.02, 0.24, 4.38], "gold", pivot=[-5.44, 17.69, 0.07], rotation=[0, 0, -9]),
            cube([4.34, 17.68, -1.98], [1.78, 0.22, 4.14], "gold_dark", pivot=[5.23, 17.79, 0.09], rotation=[0, 0, 8]),
        ],
    }


def belt_sash() -> dict:
    return {
        "name": "ouros_cobra_belt_sash",
        "parent": "torso",
        "pivot": [0, 12.0, 0],
        "cubes": [
            cube([-4.60, 11.42, -3.36], [9.20, 1.14, 0.40], "shadow"),
            cube([-4.60, 11.42, 2.96], [9.20, 1.14, 0.40], "shadow"),
            cube([-1.58, 11.15, -3.80], [3.16, 1.46, 0.46], "gold"),
            cube([-0.44, 10.18, -3.96], [0.88, 1.06, 0.26], "cobra_green"),
            cube([-3.38, 5.20, -3.48], [1.98, 6.35, 0.44], "dojo_black", pivot=[-2.39, 11.25, -3.26], rotation=[-7, 0, 9]),
            cube([-3.12, 5.20, -3.66], [1.46, 0.34, 0.18], "gold", pivot=[-2.39, 5.37, -3.57], rotation=[-7, 0, 9]),
            cube([1.30, 7.00, -3.44], [1.34, 4.50, 0.42], "charcoal", pivot=[1.97, 11.25, -3.23], rotation=[-6, 0, -6]),
            cube([1.45, 7.00, -3.62], [1.04, 0.28, 0.18], "gold_dark", pivot=[1.97, 7.14, -3.53], rotation=[-6, 0, -6]),
        ],
    }


def forearm(name: str, parent: str, left: bool) -> dict:
    x0, x1 = ((9.25, 13.65) if left else (-13.65, -9.25))
    accent = "gold" if left else "gold_dark"
    return {
        "name": name,
        "parent": parent,
        "pivot": [(x0 + x1) / 2, 20.45, 0],
        "cubes": [
            cube([x0, 19.45, -1.10], [x1 - x0, 0.32, 2.20], "wrap"),
            cube([x0, 21.08, -1.10], [x1 - x0, 0.32, 2.20], "wrap"),
            cube([x0 + 0.70, 20.10, -1.38], [x1 - x0 - 1.40, 0.30, 0.18], accent),
        ],
    }


def leg_rail(name: str, parent: str, x0: float, stage: int, accent: str, left: bool) -> dict:
    y0 = {2: 7.50, 3: 4.50, 4: 1.50}[stage]
    outer_x = x0 + (2.76 if left else 0.00)
    return {
        "name": name,
        "parent": parent,
        "pivot": [x0 + 1.5, y0 + 1.5, 0],
        "cubes": [
            cube([x0 - 0.05, y0 + 2.38, -1.54], [3.10, 0.26, 3.08], "wrap"),
            cube([outer_x, y0 + 0.25, -1.70], [0.28, 2.05, 0.20], "dojo_black"),
            cube([outer_x + 0.04, y0 + 0.58, -1.94], [0.20, 1.40, 0.16], accent),
        ],
    }


def foot_guard(name: str, parent: str, x0: float, accent: str, left: bool) -> dict:
    cubes = [
        cube([x0 + 0.16, 1.48, -2.74], [3.68, 0.32, 3.56], "dojo_black"),
        cube([x0 + 0.38, 1.78, -2.82], [3.24, 0.20, 0.20], accent),
        cube([x0 + 0.28, 0.42, 0.62], [3.44, 0.28, 1.10], "wrap"),
    ]
    if left:
        cubes.append(cube([x0 + 3.44, 1.44, -2.34], [0.28, 0.38, 1.85], "gold"))
    return {"name": name, "parent": parent, "pivot": [x0 + 2, 1, -0.4], "cubes": cubes}


def cosmetic_bones() -> list[dict]:
    return [
        cobra_hood(), open_gi(), belt_sash(),
        forearm("ouros_cobra_left_forearm", "arm_left2", True),
        forearm("ouros_cobra_right_forearm", "arm_right2", False),
        leg_rail("ouros_cobra_left_leg2", "leg_left2", 1.0, 2, "gold", True),
        leg_rail("ouros_cobra_left_leg3", "leg_left3", 1.0, 3, "gold_dark", True),
        leg_rail("ouros_cobra_left_leg4", "leg_left4", 1.0, 4, "gold", True),
        leg_rail("ouros_cobra_right_leg2", "leg_right2", -4.0, 2, "gold_dark", False),
        leg_rail("ouros_cobra_right_leg3", "leg_right3", -4.0, 3, "gold", False),
        leg_rail("ouros_cobra_right_leg4", "leg_right4", -4.0, 4, "gold_dark", False),
        foot_guard("ouros_cobra_left_foot", "foot_left", 0.5, "gold", True),
        foot_guard("ouros_cobra_right_foot", "foot_right", -4.5, "gold_dark", False),
    ]


def derive_model(src: Path, dst: Path) -> None:
    data = json.loads(src.read_text(encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    original = copy.deepcopy(geo["bones"])
    if len(original) != 30:
        raise SystemExit(f"expected 30 official Hitmonlee bones, got {len(original)}")
    geo["description"]["identifier"] = "geometry.ouros_cobra_dojo_hitmonlee"
    geo["bones"] = original + cosmetic_bones()
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_text(json.dumps(data, separators=(",", ":")) + "\n", encoding="utf-8")


def derive_texture(src: Path, dst: Path) -> None:
    image = Image.open(src).convert("RGBA")
    if image.size != (64, 64):
        raise SystemExit(f"unexpected Hitmonlee texture size {image.size}")
    for _, (x, y) in PIXELS.items():
        if image.getpixel((x, y))[3] != 0:
            raise SystemExit(f"material texel {(x, y)} is not free in official texture")
    for name, (x, y) in PIXELS.items():
        image.putpixel((x, y), PALETTE[name])
    dst.parent.mkdir(parents=True, exist_ok=True)
    image.save(dst, optimize=True)


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
        "derivedBones": 43,
        "cosmeticBones": 13,
        "cosmeticCubes": sum(len(b.get("cubes", [])) for b in cosmetics),
        "palettePixels": PIXELS,
        "bodyTexturePolicy": "official biological texels unchanged; only verified transparent y=63 swatches added",
        "artDirection": "broad stepped lateral cobra hood + narrow open-V gi + asymmetric champion sash + sparse longitudinal strike rails",
    }
    (args.output_root / "build-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
