#!/usr/bin/env python3
"""Build Hitmonlee Cobra Dojo v2 from the exact official Cobblemon model.

Design correction after v1 visual rejection:
- preserve all 30 official bones exactly and in order;
- keep Hitmonlee's biological body/face texture intact;
- add a martial-arts outfit around the body instead of a helmet/armor replacement;
- keep telescoping legs visually readable by using slim animated wraps rather than shells.
"""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
from pathlib import Path

from PIL import Image

PALETTE = {
    "dojo_black": (18, 20, 18, 255),
    "charcoal": (43, 44, 39, 255),
    "gold": (232, 184, 42, 255),
    "gold_dark": (151, 104, 24, 255),
    "wrap": (112, 103, 84, 255),
    "cream": (226, 213, 165, 255),
    "cobra_green": (74, 102, 43, 255),
    "shadow": (9, 10, 9, 255),
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


def cobra_collar() -> dict:
    """Open cobra-hood collar: strong rear/side silhouette, zero face helmet."""
    return {
        "name": "ouros_cobra_collar",
        "parent": "torso2",
        "pivot": [0, 18.0, 0.0],
        "cubes": [
            # Thin rear collar under the top of the biological head/body.
            cube([-4.75, 17.15, 3.30], [9.50, 3.95, 0.55], "dojo_black"),
            cube([-4.45, 20.72, 3.58], [8.90, 0.34, 0.26], "gold"),
            # Cobra hood flares behind the outline; front face remains completely exposed.
            cube([-7.05, 17.60, 2.92], [2.60, 4.55, 0.66], "dojo_black", pivot=[-5.75, 19.88, 3.25], rotation=[0, 0, -13]),
            cube([4.45, 17.85, 2.92], [2.25, 4.15, 0.66], "charcoal", pivot=[5.58, 19.93, 3.25], rotation=[0, 0, 11]),
            cube([-7.22, 21.55, 3.00], [2.45, 0.30, 0.48], "gold", pivot=[-5.99, 21.70, 3.24], rotation=[0, 0, -13]),
            cube([4.58, 21.55, 3.00], [2.00, 0.28, 0.48], "gold_dark", pivot=[5.58, 21.69, 3.24], rotation=[0, 0, 11]),
            # Small collar points below the eyes, not across them.
            cube([-4.50, 16.75, -3.72], [2.25, 0.42, 0.34], "gold", pivot=[-3.38, 16.96, -3.55], rotation=[0, 0, -18]),
            cube([2.25, 16.75, -3.72], [2.25, 0.42, 0.34], "gold_dark", pivot=[3.38, 16.96, -3.55], rotation=[0, 0, 18]),
        ],
    }


def gi_shell() -> dict:
    """Sleeveless gi built as lapels/panels; it never boxes in the face or full torso."""
    return {
        "name": "ouros_cobra_gi",
        "parent": "torso2",
        "pivot": [0, 15.2, 0.0],
        "cubes": [
            # Open front lapels leave a central V of original Hitmonlee visible.
            cube([-4.36, 12.20, -3.72], [3.28, 5.45, 0.34], "dojo_black", pivot=[-2.72, 14.93, -3.55], rotation=[0, 0, -4]),
            cube([1.08, 12.20, -3.72], [3.28, 5.45, 0.34], "charcoal", pivot=[2.72, 14.93, -3.55], rotation=[0, 0, 4]),
            cube([-3.95, 16.98, -3.93], [3.20, 0.38, 0.22], "gold", pivot=[-2.35, 17.17, -3.82], rotation=[0, 0, -23]),
            cube([0.75, 16.98, -3.93], [3.20, 0.38, 0.22], "gold_dark", pivot=[2.35, 17.17, -3.82], rotation=[0, 0, 23]),
            # Slim side and back cloth establishes a garment without turning the body into a cube.
            cube([-4.67, 12.15, -2.95], [0.36, 5.80, 5.90], "dojo_black"),
            cube([4.31, 12.15, -2.95], [0.36, 5.80, 5.90], "charcoal"),
            cube([-4.20, 12.15, 3.34], [8.40, 5.80, 0.32], "dojo_black"),
            cube([-4.10, 12.03, 3.64], [8.20, 0.32, 0.20], "gold_dark"),
            # Sleeveless shoulder caps; left side intentionally stronger for premium asymmetry.
            cube([-6.30, 17.15, -2.25], [2.15, 0.92, 4.65], "dojo_black", pivot=[-5.22, 17.61, 0.08], rotation=[0, 0, -7]),
            cube([4.15, 17.30, -2.10], [1.75, 0.74, 4.35], "charcoal", pivot=[5.02, 17.67, 0.08], rotation=[0, 0, 6]),
            cube([-6.34, 17.92, -2.10], [2.05, 0.26, 4.35], "gold", pivot=[-5.31, 18.05, 0.08], rotation=[0, 0, -7]),
            cube([4.23, 17.88, -1.98], [1.58, 0.22, 4.10], "gold_dark", pivot=[5.02, 17.99, 0.07], rotation=[0, 0, 6]),
        ],
    }


def belt_sash() -> dict:
    return {
        "name": "ouros_cobra_belt_sash",
        "parent": "torso",
        "pivot": [0, 12.3, 0.0],
        "cubes": [
            cube([-4.42, 11.70, -3.34], [8.84, 0.92, 0.34], "shadow"),
            cube([-4.42, 11.70, 3.00], [8.84, 0.92, 0.34], "shadow"),
            cube([-4.58, 11.70, -3.00], [0.34, 0.92, 6.00], "shadow"),
            cube([4.24, 11.70, -3.00], [0.34, 0.92, 6.00], "shadow"),
            cube([-1.25, 11.50, -3.70], [2.50, 1.18, 0.42], "gold"),
            cube([-0.42, 10.65, -3.86], [0.84, 0.92, 0.26], "cobra_green"),
            # Two cloth tails, narrow and separated so the leg silhouette stays readable.
            cube([-2.75, 6.10, -3.42], [1.45, 5.72, 0.40], "dojo_black", pivot=[-2.03, 11.55, -3.22], rotation=[-5, 0, 6]),
            cube([1.22, 6.65, -3.40], [1.34, 5.15, 0.40], "charcoal", pivot=[1.89, 11.55, -3.20], rotation=[-5, 0, -5]),
            cube([-2.58, 6.10, -3.58], [1.12, 0.28, 0.18], "gold", pivot=[-2.02, 6.24, -3.49], rotation=[-5, 0, 6]),
            cube([1.34, 6.65, -3.56], [1.08, 0.26, 0.18], "gold_dark", pivot=[1.88, 6.78, -3.47], rotation=[-5, 0, -5]),
        ],
    }


def forearm_wrap(name: str, parent: str, left: bool) -> dict:
    if left:
        x0, x1, accent = 9.25, 13.65, "gold"
    else:
        x0, x1, accent = -13.65, -9.25, "gold_dark"
    return {
        "name": name,
        "parent": parent,
        "pivot": [(x0 + x1) / 2.0, 20.5, 0.0],
        "cubes": [
            cube([x0, 19.30, -1.18], [x1 - x0, 0.34, 2.36], "wrap"),
            cube([x0, 21.22, -1.18], [x1 - x0, 0.34, 2.36], "wrap"),
            cube([x0 + 0.50, 19.55, -1.30], [x1 - x0 - 1.0, 1.60, 0.24], "dojo_black"),
            cube([x0 + 0.78, 20.20, -1.52], [x1 - x0 - 1.56, 0.28, 0.18], accent),
        ],
    }


def leg_wrap(name: str, parent: str, x0: float, stage: int, accent: str) -> dict:
    y0 = {2: 7.50, 3: 4.50, 4: 1.50}[stage]
    return {
        "name": name,
        "parent": parent,
        "pivot": [x0 + 1.50, y0 + 1.50, 0.0],
        "cubes": [
            # Thin wrap rings preserve the segmented/telescoping leg as the dominant shape.
            cube([x0 - 0.10, y0 + 0.18, -1.60], [3.20, 0.30, 3.20], "dojo_black"),
            cube([x0 - 0.10, y0 + 2.52, -1.60], [3.20, 0.30, 3.20], "wrap"),
            cube([x0 + 0.34, y0 + 1.22, -1.72], [2.32, 0.24, 0.18], accent),
        ],
    }


def foot_wrap(name: str, parent: str, x0: float, accent: str) -> dict:
    return {
        "name": name,
        "parent": parent,
        "pivot": [x0 + 2.0, 1.0, -0.5],
        "cubes": [
            cube([x0 - 0.06, 1.52, -2.88], [4.12, 0.28, 4.66], "dojo_black"),
            cube([x0 + 0.18, 0.36, -3.14], [3.64, 0.30, 4.96], "wrap"),
            cube([x0 + 0.42, 1.78, -3.08], [3.16, 0.20, 0.20], accent),
        ],
    }


def cosmetic_bones() -> list[dict]:
    return [
        cobra_collar(),
        gi_shell(),
        belt_sash(),
        forearm_wrap("ouros_cobra_left_forearm", "arm_left2", True),
        forearm_wrap("ouros_cobra_right_forearm", "arm_right2", False),
        leg_wrap("ouros_cobra_left_leg2", "leg_left2", 1.0, 2, "gold"),
        leg_wrap("ouros_cobra_left_leg3", "leg_left3", 1.0, 3, "gold_dark"),
        leg_wrap("ouros_cobra_left_leg4", "leg_left4", 1.0, 4, "gold"),
        leg_wrap("ouros_cobra_right_leg2", "leg_right2", -4.0, 2, "gold_dark"),
        leg_wrap("ouros_cobra_right_leg3", "leg_right3", -4.0, 3, "gold"),
        leg_wrap("ouros_cobra_right_leg4", "leg_right4", -4.0, 4, "gold_dark"),
        foot_wrap("ouros_cobra_left_foot", "foot_left", 0.5, "gold"),
        foot_wrap("ouros_cobra_right_foot", "foot_right", -4.5, "gold_dark"),
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
    """Keep every official biological texel unchanged; reserve only verified free texels."""
    image = Image.open(src).convert("RGBA")
    if image.size != (64, 64):
        raise SystemExit(f"unexpected Hitmonlee texture size {image.size}")
    for name, (x, y) in PIXELS.items():
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
        "bodyTexturePolicy": "official biological texels unchanged; only verified transparent y=63 material swatches added",
    }
    (args.output_root / "build-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
