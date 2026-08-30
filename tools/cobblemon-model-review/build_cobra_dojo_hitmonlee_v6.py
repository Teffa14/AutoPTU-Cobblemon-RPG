#!/usr/bin/env python3
"""Build Hitmonlee Cobra Dojo v6 from exact Cobblemon 1.7.3 Hitmonlee.

V6 abandons the rejected literal cobra hood. The transformation is a premium
karate champion: exact-fit headband, open sleeveless gi, wide champion belt,
asymmetric split coat tails, original geometric back crest, forearm guards and
continuous longitudinal kick rails across Hitmonlee's telescoping leg bones.

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
    """Thin band fitted to the official 9x9x7 torso2 head cube."""
    return {
        "name": "ouros_champion_headband",
        "parent": "torso2",
        "pivot": [0, 22.65, 0],
        "cubes": [
            # Ring sits above the eyes (eye pivots y=21) and never covers them.
            cube([-4.64, 22.42, -3.72], [9.28, 0.72, 0.24], "dojo_black"),
            cube([-4.64, 22.42, 3.48], [9.28, 0.72, 0.24], "dojo_black"),
            cube([-4.72, 22.42, -3.48], [0.24, 0.72, 6.96], "dojo_black"),
            cube([4.48, 22.42, -3.48], [0.24, 0.72, 6.96], "dojo_black"),
            # Central champion plaque, original Ouros motif.
            cube([-1.35, 22.38, -3.94], [2.70, 0.82, 0.20], "gold"),
            cube([-0.34, 22.48, -4.12], [0.68, 0.62, 0.14], "cobra_green"),
            # Two asymmetric cloth tails fall behind the right rear corner.
            cube([3.05, 18.30, 3.62], [0.72, 4.28, 0.26], "dojo_black", pivot=[3.41, 22.30, 3.75], rotation=[-3, 0, 8]),
            cube([3.92, 19.45, 3.68], [0.52, 3.10, 0.24], "gold_dark", pivot=[4.18, 22.30, 3.80], rotation=[-4, 0, -7]),
        ],
    }


def champion_gi() -> dict:
    """Sleeveless open gi fitted around the exact 8x5x6 official torso."""
    return {
        "name": "ouros_champion_gi",
        "parent": "torso",
        "pivot": [0, 14.5, 0],
        "cubes": [
            # Narrow diagonal lapels. Centre chest remains biological Hitmonlee.
            cube([-4.22, 12.05, -3.26], [1.78, 4.82, 0.32], "dojo_black", pivot=[-3.30, 14.48, -3.10], rotation=[0, 0, -10]),
            cube([2.44, 12.05, -3.26], [1.78, 4.82, 0.32], "charcoal", pivot=[3.30, 14.48, -3.10], rotation=[0, 0, 10]),
            cube([-3.90, 16.20, -3.48], [2.35, 0.34, 0.20], "gold", pivot=[-2.72, 16.37, -3.38], rotation=[0, 0, -30]),
            cube([1.55, 16.20, -3.48], [2.35, 0.34, 0.20], "gold_dark", pivot=[2.72, 16.37, -3.38], rotation=[0, 0, 30]),
            # Side panels make the gi a garment instead of two floating lapels.
            cube([-4.28, 11.92, -2.78], [0.36, 5.18, 5.56], "dojo_black"),
            cube([3.92, 11.92, -2.78], [0.36, 5.18, 5.56], "charcoal"),
            # Rear jacket panel, shallow and fitted rather than a backpack.
            cube([-4.12, 12.00, 3.02], [8.24, 5.08, 0.34], "dojo_black"),
            cube([-3.78, 16.68, 3.36], [7.56, 0.32, 0.18], "gold_dark"),
            # Low shoulder caps broaden the fighter silhouette below the head.
            cube([-6.05, 16.30, -2.20], [2.00, 0.82, 4.40], "dojo_black", pivot=[-5.05, 16.71, 0], rotation=[0, 0, -10]),
            cube([4.05, 16.42, -2.08], [1.78, 0.70, 4.16], "charcoal", pivot=[4.94, 16.77, 0], rotation=[0, 0, 9]),
            cube([-6.05, 16.98, -2.12], [1.90, 0.24, 4.24], "gold", pivot=[-5.10, 17.10, 0], rotation=[0, 0, -10]),
            cube([4.08, 17.00, -2.00], [1.68, 0.22, 4.00], "gold_dark", pivot=[4.92, 17.11, 0], rotation=[0, 0, 9]),
            # Large original back crest: chevron + green core; no third-party logo.
            cube([-2.55, 13.15, 3.40], [2.35, 0.46, 0.18], "gold", pivot=[-1.38, 13.38, 3.49], rotation=[0, 0, -32]),
            cube([0.20, 13.15, 3.40], [2.35, 0.46, 0.18], "gold", pivot=[1.38, 13.38, 3.49], rotation=[0, 0, 32]),
            cube([-0.72, 13.72, 3.44], [1.44, 1.44, 0.18], "cobra_green", pivot=[0, 14.44, 3.53], rotation=[0, 0, 45]),
            cube([-1.52, 15.02, 3.42], [3.04, 0.34, 0.18], "gold_dark", pivot=[0, 15.19, 3.51], rotation=[0, 0, 0]),
        ],
    }


def champion_belt_and_coat() -> dict:
    """Dominant belt and split asymmetric coat tails define the lower silhouette."""
    return {
        "name": "ouros_champion_belt_sash",
        "parent": "torso",
        "pivot": [0, 12.0, 0],
        "cubes": [
            # Wide belt wraps the exact torso waist.
            cube([-4.30, 11.55, -3.34], [8.60, 1.10, 0.38], "shadow"),
            cube([-4.30, 11.55, 2.96], [8.60, 1.10, 0.38], "shadow"),
            cube([-4.42, 11.55, -2.96], [0.34, 1.10, 5.92], "shadow"),
            cube([4.08, 11.55, -2.96], [0.34, 1.10, 5.92], "shadow"),
            cube([-1.65, 11.28, -3.78], [3.30, 1.48, 0.46], "gold"),
            cube([-0.46, 10.35, -3.96], [0.92, 1.02, 0.25], "cobra_green"),
            # Front split tails change silhouette but leave legs readable.
            cube([-3.70, 6.10, -3.24], [2.55, 5.62, 0.46], "dojo_black", pivot=[-2.42, 11.45, -3.01], rotation=[-6, 0, 7]),
            cube([1.05, 7.10, -3.22], [2.35, 4.58, 0.44], "charcoal", pivot=[2.22, 11.42, -3.00], rotation=[-6, 0, -6]),
            cube([-3.42, 6.05, -3.45], [1.98, 0.34, 0.18], "gold", pivot=[-2.43, 6.22, -3.36], rotation=[-6, 0, 7]),
            cube([1.28, 7.05, -3.43], [1.86, 0.30, 0.18], "gold_dark", pivot=[2.21, 7.20, -3.34], rotation=[-6, 0, -6]),
            # Rear coat tails provide a premium martial jacket read from 3/4/back.
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
            cube([x0, 21.08, -1.10], [x1 - x0, 0.32, 2.20], "wrap"),
            cube([x0 + 0.72, 20.08, -1.40], [x1 - x0 - 1.44, 0.34, 0.20], accent),
        ],
    }


def kick_rail(name: str, parent: str, x0: float, y0: float, left: bool, accent: str) -> dict:
    outer_x = x0 + (2.78 if left else -0.06)
    return {
        "name": name,
        "parent": parent,
        "pivot": [x0 + 1.5, y0 + 1.5, -1.5],
        "cubes": [
            # Continuous black/gold line down the outside of each telescoping segment.
            cube([outer_x, y0 + 0.20, -1.72], [0.28, 2.58, 0.34], "dojo_black"),
            cube([outer_x + 0.04, y0 + 0.55, -1.98], [0.20, 1.88, 0.16], accent),
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
        ],
    }


def cosmetic_bones() -> list[dict]:
    return [
        champion_headband(),
        champion_gi(),
        champion_belt_and_coat(),
        forearm_guard("ouros_champion_left_forearm", "arm_left2", True),
        forearm_guard("ouros_champion_right_forearm", "arm_right2", False),
        kick_rail("ouros_champion_left_leg2", "leg_left2", 1.0, 7.5, True, "gold"),
        kick_rail("ouros_champion_left_leg3", "leg_left3", 1.0, 4.5, True, "gold_dark"),
        kick_rail("ouros_champion_left_leg4", "leg_left4", 1.0, 1.5, True, "gold"),
        kick_rail("ouros_champion_right_leg2", "leg_right2", -4.0, 7.5, False, "gold_dark"),
        kick_rail("ouros_champion_right_leg3", "leg_right3", -4.0, 4.5, False, "gold"),
        kick_rail("ouros_champion_right_leg4", "leg_right4", -4.0, 1.5, False, "gold_dark"),
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
        "derivedBones": 43,
        "cosmeticBones": 13,
        "cosmeticCubes": sum(len(b.get("cubes", [])) for b in cosmetics),
        "cosmeticNames": [b["name"] for b in cosmetics],
        "palettePixels": PIXELS,
        "bodyTexturePolicy": "official biological texels unchanged; only verified transparent y=63 accessory swatches added",
        "artDirection": "karate champion: exact-fit headband + open sleeveless gi + wide belt + asymmetric split coat + continuous kick rails",
    }
    (args.output_root / "build-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
