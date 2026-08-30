#!/usr/bin/env python3
"""Build Hitmonlee Cobra Dojo v3 from the exact official Cobblemon model.

V3 artistic correction after v2 rejection:
- preserve all 30 official Hitmonlee bones exactly and in order;
- keep every official biological texel unchanged;
- keep the head/eyes fully visible;
- make one dominant rear cobra mantle frame the head without becoming a helmet;
- use an open V gi with asymmetric skirt tails instead of a boxed torso;
- simplify telescoping-leg wraps and feet so Hitmonlee remains the visual subject.
"""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
from pathlib import Path
from PIL import Image

PALETTE = {
    "dojo_black": (17, 19, 17, 255),
    "charcoal": (47, 48, 41, 255),
    "gold": (232, 184, 42, 255),
    "gold_dark": (145, 99, 22, 255),
    "wrap": (116, 106, 86, 255),
    "cream": (226, 214, 170, 255),
    "cobra_green": (71, 104, 44, 255),
    "lacquer": (82, 43, 26, 255),
    "shadow": (8, 9, 8, 255),
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


def cobra_mantle() -> dict:
    """Dominant cobra hood behind the head; nothing crosses the face plane."""
    return {
        "name": "ouros_cobra_mantle",
        "parent": "torso2",
        "pivot": [0, 19.0, 3.15],
        "cubes": [
            # Low bridge behind the neck.
            cube([-4.25, 16.55, 3.28], [8.50, 1.08, 0.52], "shadow"),
            cube([-3.65, 17.45, 3.38], [7.30, 0.40, 0.30], "gold_dark"),
            # Left cobra hood: three connected planes create one readable flare.
            cube([-7.70, 17.15, 2.86], [3.30, 5.85, 0.78], "dojo_black", pivot=[-5.70, 19.95, 3.25], rotation=[0, 0, -14]),
            cube([-9.15, 18.65, 2.92], [2.75, 4.10, 0.70], "charcoal", pivot=[-7.30, 20.45, 3.25], rotation=[0, 0, -24]),
            cube([-8.80, 22.25, 2.98], [3.60, 0.48, 0.60], "gold", pivot=[-7.00, 22.49, 3.28], rotation=[0, 0, -22]),
            cube([-7.34, 18.55, 3.64], [2.18, 2.55, 0.30], "cobra_green", pivot=[-6.25, 19.82, 3.79], rotation=[0, 0, -14]),
            # Right hood is intentionally smaller/asymmetric.
            cube([4.40, 17.45, 2.88], [3.05, 5.35, 0.76], "dojo_black", pivot=[5.95, 20.00, 3.26], rotation=[0, 0, 12]),
            cube([6.30, 18.85, 2.94], [2.35, 3.70, 0.68], "charcoal", pivot=[7.25, 20.55, 3.28], rotation=[0, 0, 21]),
            cube([5.15, 22.16, 3.00], [3.30, 0.44, 0.58], "gold_dark", pivot=[6.80, 22.38, 3.29], rotation=[0, 0, 19]),
            cube([5.16, 18.75, 3.64], [2.05, 2.35, 0.30], "cobra_green", pivot=[6.18, 19.92, 3.79], rotation=[0, 0, 12]),
            # Rear cobra-spine motif, original geometry only, no external logo.
            cube([-0.36, 17.75, 3.82], [0.72, 4.10, 0.26], "gold"),
            cube([-1.30, 19.14, 3.86], [2.60, 0.54, 0.22], "gold_dark", pivot=[0, 19.41, 3.97], rotation=[0, 0, 45]),
            cube([-0.82, 21.50, 3.88], [1.64, 0.62, 0.22], "cobra_green", pivot=[0, 21.81, 3.99], rotation=[0, 0, 45]),
        ],
    }


def open_gi() -> dict:
    """Open-V gi: two lapels + side/rear cloth, never a full front box."""
    return {
        "name": "ouros_cobra_gi",
        "parent": "torso2",
        "pivot": [0, 15.2, 0],
        "cubes": [
            # Diagonal lapels leave the original chest visible in the centre.
            cube([-4.18, 12.35, -3.70], [2.25, 5.25, 0.34], "dojo_black", pivot=[-3.05, 14.95, -3.53], rotation=[0, 0, -9]),
            cube([1.93, 12.35, -3.70], [2.25, 5.25, 0.34], "charcoal", pivot=[3.05, 14.95, -3.53], rotation=[0, 0, 9]),
            cube([-3.80, 16.90, -3.92], [2.78, 0.34, 0.22], "gold", pivot=[-2.41, 17.07, -3.81], rotation=[0, 0, -28]),
            cube([1.02, 16.90, -3.92], [2.78, 0.34, 0.22], "gold_dark", pivot=[2.41, 17.07, -3.81], rotation=[0, 0, 28]),
            # Narrow side cloth and split rear panel.
            cube([-4.60, 12.30, -2.55], [0.34, 5.35, 5.10], "dojo_black"),
            cube([4.26, 12.30, -2.55], [0.34, 5.35, 5.10], "charcoal"),
            cube([-4.10, 12.30, 3.30], [3.72, 5.35, 0.30], "dojo_black"),
            cube([0.38, 12.55, 3.30], [3.72, 5.10, 0.30], "charcoal"),
            # Shoulder ledges connect visually to the mantle but stay below the face.
            cube([-6.05, 17.10, -1.95], [1.85, 0.72, 4.10], "dojo_black", pivot=[-5.12, 17.46, 0.10], rotation=[0, 0, -8]),
            cube([4.20, 17.25, -1.82], [1.55, 0.58, 3.84], "charcoal", pivot=[4.98, 17.54, 0.10], rotation=[0, 0, 7]),
            cube([-6.08, 17.70, -1.88], [1.72, 0.22, 3.94], "gold", pivot=[-5.22, 17.81, 0.09], rotation=[0, 0, -8]),
        ],
    }


def belt_and_tails() -> dict:
    return {
        "name": "ouros_cobra_belt_sash",
        "parent": "torso",
        "pivot": [0, 12.1, 0],
        "cubes": [
            cube([-4.46, 11.62, -3.25], [8.92, 0.94, 0.34], "shadow"),
            cube([-4.46, 11.62, 2.91], [8.92, 0.94, 0.34], "shadow"),
            cube([-1.46, 11.40, -3.66], [2.92, 1.22, 0.40], "gold"),
            cube([-0.38, 10.62, -3.82], [0.76, 0.86, 0.24], "cobra_green"),
            # Long asymmetric tails: one broad left blade, one shorter right streamer.
            cube([-3.05, 5.95, -3.40], [1.72, 5.75, 0.42], "dojo_black", pivot=[-2.19, 11.45, -3.19], rotation=[-6, 0, 8]),
            cube([-2.84, 5.95, -3.58], [1.30, 0.30, 0.18], "gold", pivot=[-2.19, 6.10, -3.49], rotation=[-6, 0, 8]),
            cube([1.34, 7.20, -3.38], [1.16, 4.45, 0.40], "charcoal", pivot=[1.92, 11.43, -3.18], rotation=[-6, 0, -6]),
            cube([1.46, 7.20, -3.56], [0.92, 0.26, 0.18], "gold_dark", pivot=[1.92, 7.33, -3.47], rotation=[-6, 0, -6]),
        ],
    }


def forearm_wrap(name: str, parent: str, left: bool) -> dict:
    x0, x1 = ((9.30, 13.60) if left else (-13.60, -9.30))
    accent = "gold" if left else "gold_dark"
    return {
        "name": name,
        "parent": parent,
        "pivot": [(x0 + x1) / 2, 20.45, 0],
        "cubes": [
            cube([x0, 19.42, -1.08], [x1 - x0, 0.30, 2.16], "wrap"),
            cube([x0, 21.10, -1.08], [x1 - x0, 0.30, 2.16], "wrap"),
            cube([x0 + 0.72, 20.16, -1.38], [x1 - x0 - 1.44, 0.26, 0.18], accent),
        ],
    }


def leg_strike_wrap(name: str, parent: str, x0: float, stage: int, accent: str) -> dict:
    y0 = {2: 7.50, 3: 4.50, 4: 1.50}[stage]
    outside_x = x0 + (2.78 if x0 > 0 else 0.02)
    return {
        "name": name,
        "parent": parent,
        "pivot": [x0 + 1.50, y0 + 1.50, 0],
        "cubes": [
            # One upper ring + one narrow vertical strike line. No shell around the leg.
            cube([x0 - 0.06, y0 + 2.35, -1.56], [3.12, 0.28, 3.12], "wrap"),
            cube([outside_x, y0 + 0.54, -1.68], [0.20, 1.76, 0.18], accent),
        ],
    }


def foot_strike_guard(name: str, parent: str, x0: float, accent: str, left: bool) -> dict:
    cubes = [
        # Single dominant instep plate plus heel strap; no claws/spike fan.
        cube([x0 + 0.18, 1.50, -2.72], [3.64, 0.30, 3.54], "dojo_black"),
        cube([x0 + 0.42, 1.77, -2.78], [3.16, 0.20, 0.20], accent),
        cube([x0 + 0.28, 0.40, 0.62], [3.44, 0.28, 1.10], "wrap"),
    ]
    if left:
        # One asymmetric kick-edge accent only.
        cubes.append(cube([x0 + 3.42, 1.46, -2.40], [0.26, 0.34, 2.08], "gold"))
    return {"name": name, "parent": parent, "pivot": [x0 + 2.0, 1.0, -0.4], "cubes": cubes}


def cosmetic_bones() -> list[dict]:
    return [
        cobra_mantle(),
        open_gi(),
        belt_and_tails(),
        forearm_wrap("ouros_cobra_left_forearm", "arm_left2", True),
        forearm_wrap("ouros_cobra_right_forearm", "arm_right2", False),
        leg_strike_wrap("ouros_cobra_left_leg2", "leg_left2", 1.0, 2, "gold"),
        leg_strike_wrap("ouros_cobra_left_leg3", "leg_left3", 1.0, 3, "gold_dark"),
        leg_strike_wrap("ouros_cobra_left_leg4", "leg_left4", 1.0, 4, "gold"),
        leg_strike_wrap("ouros_cobra_right_leg2", "leg_right2", -4.0, 2, "gold_dark"),
        leg_strike_wrap("ouros_cobra_right_leg3", "leg_right3", -4.0, 3, "gold"),
        leg_strike_wrap("ouros_cobra_right_leg4", "leg_right4", -4.0, 4, "gold_dark"),
        foot_strike_guard("ouros_cobra_left_foot", "foot_left", 0.5, "gold", True),
        foot_strike_guard("ouros_cobra_right_foot", "foot_right", -4.5, "gold_dark", False),
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
        "bodyTexturePolicy": "official biological texels unchanged; only verified transparent y=63 material swatches added",
        "artDirection": "dominant rear cobra mantle + open V gi + asymmetric sash + simplified strike wraps",
    }
    (args.output_root / "build-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
