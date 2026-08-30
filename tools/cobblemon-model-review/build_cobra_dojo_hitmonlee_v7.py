#!/usr/bin/env python3
"""Build Hitmonlee Cobra Dojo v7 from exact Cobblemon 1.7.3 Hitmonlee.

This final v7 pass follows the approved clothing reference while respecting the
real Hitmonlee rig:
- a lower gi covers the biological torso as clothing instead of floating panels;
- an upper gi/collar is attached to torso2 and rises around the lower head in a V;
- cap sleeves are attached to arm_left/arm_right at the actual Y=20 shoulder pivots;
- dedicated knee guards plus layered guards on every telescoping leg segment make
  the legs the visual focus;
- the head and eyes remain fully visible and Hitmonlee remains unmistakable.

The official 30 bones are copied without edits and remain in original order.
Official biological texture pixels are unchanged; accessory materials occupy
only verified transparent texels on row 63. All new bones are presentation-only.
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


def lower_gi() -> dict:
    """The actual jacket body around the exact official 8x5x6 torso cube."""
    return {
        "name": "ouros_champion_lower_gi",
        "parent": "torso",
        "pivot": [0, 14.4, 0],
        "cubes": [
            # Broad lower front coverage removes the old 'two floating panels' read.
            cube([-4.28, 11.86, -3.34], [4.28, 3.72, 0.38], "dojo_black"),
            cube([0.00, 11.86, -3.34], [4.28, 3.72, 0.38], "charcoal"),
            cube([-4.34, 11.82, -2.96], [0.42, 5.50, 5.92], "dojo_black"),
            cube([3.92, 11.82, -2.96], [0.42, 5.50, 5.92], "charcoal"),
            cube([-4.16, 11.84, 3.02], [8.32, 5.48, 0.38], "dojo_black"),
            # Bottom hem and side piping.
            cube([-4.18, 11.84, -3.58], [8.36, 0.24, 0.18], "gold_dark"),
            cube([-4.48, 12.04, -2.54], [0.18, 4.92, 5.08], "gold"),
            cube([4.30, 12.04, -2.54], [0.18, 4.92, 5.08], "gold_dark"),
            # Front fabric layering above the waist, still below the upper collar.
            cube([-4.12, 15.12, -3.48], [2.88, 2.28, 0.24], "dojo_black", pivot=[-2.68, 16.24, -3.36], rotation=[0, 0, -5]),
            cube([1.24, 15.12, -3.48], [2.88, 2.28, 0.24], "charcoal", pivot=[2.68, 16.24, -3.36], rotation=[0, 0, 5]),
            # Original geometric crest on the jacket front.
            cube([1.58, 13.38, -3.62], [1.56, 1.56, 0.18], "gold_dark", pivot=[2.36, 14.16, -3.53], rotation=[0, 0, 45]),
            cube([2.04, 13.84, -3.82], [0.64, 0.64, 0.14], "cobra_green"),
            cube([1.82, 14.56, -3.78], [1.08, 0.22, 0.14], "gold"),
            # Large original back crest, no copied third-party logo or wording.
            cube([-3.22, 12.80, 3.44], [2.90, 0.50, 0.18], "gold", pivot=[-1.77, 13.05, 3.53], rotation=[0, 0, -34]),
            cube([0.32, 12.80, 3.44], [2.90, 0.50, 0.18], "gold", pivot=[1.77, 13.05, 3.53], rotation=[0, 0, 34]),
            cube([-1.00, 13.50, 3.46], [2.00, 2.00, 0.18], "cobra_green", pivot=[0, 14.50, 3.55], rotation=[0, 0, 45]),
            cube([-2.00, 15.30, 3.46], [4.00, 0.38, 0.18], "gold_dark"),
            cube([-0.40, 15.68, 3.48], [0.80, 1.30, 0.16], "gold"),
        ],
    }


def upper_gi() -> dict:
    """High sleeveless V collar attached to torso2, beneath Hitmonlee's eyes."""
    return {
        "name": "ouros_champion_upper_gi",
        "parent": "torso2",
        "pivot": [0, 17.8, 0],
        "cubes": [
            # Side chest/neck panels rise around the biological lower face, never across the eyes.
            cube([-4.72, 16.00, -3.72], [2.98, 3.20, 0.34], "dojo_black", pivot=[-3.23, 17.58, -3.55], rotation=[0, 0, -5]),
            cube([1.74, 16.00, -3.72], [2.98, 3.20, 0.34], "charcoal", pivot=[3.23, 17.58, -3.55], rotation=[0, 0, 5]),
            # Dark inner V and yellow piping mirror a real sleeveless karate gi.
            cube([-3.60, 15.94, -3.94], [0.58, 4.20, 0.18], "shadow", pivot=[-3.31, 17.98, -3.85], rotation=[0, 0, -38]),
            cube([3.02, 15.94, -3.94], [0.58, 4.20, 0.18], "shadow", pivot=[3.31, 17.98, -3.85], rotation=[0, 0, 38]),
            cube([-3.32, 16.02, -4.10], [0.32, 4.08, 0.14], "gold", pivot=[-3.16, 18.00, -4.03], rotation=[0, 0, -38]),
            cube([3.00, 16.02, -4.10], [0.32, 4.08, 0.14], "gold", pivot=[3.16, 18.00, -4.03], rotation=[0, 0, 38]),
            # Side collars connect the upper gi around the lower head.
            cube([-4.86, 16.02, -3.28], [0.34, 2.94, 6.56], "dojo_black"),
            cube([4.52, 16.02, -3.28], [0.34, 2.94, 6.56], "charcoal"),
            # Back shoulder-yoke gives the higher gi one continuous mass from rear view.
            cube([-4.58, 16.02, 3.50], [9.16, 3.04, 0.32], "dojo_black"),
            cube([-4.26, 18.78, 3.80], [8.52, 0.28, 0.16], "gold_dark"),
            # Small collar notch below the chin reinforces the V rather than a chest plate.
            cube([-1.32, 16.04, -3.86], [1.04, 0.30, 0.16], "gold_dark", pivot=[-0.80, 16.19, -3.78], rotation=[0, 0, -20]),
            cube([0.28, 16.04, -3.86], [1.04, 0.30, 0.16], "gold_dark", pivot=[0.80, 16.19, -3.78], rotation=[0, 0, 20]),
        ],
    }


def shoulder_cap(name: str, parent: str, left: bool) -> dict:
    """Actual cap sleeve attached to the official arm root at Y=20."""
    accent = "gold" if left else "gold_dark"
    material = "dojo_black" if left else "charcoal"
    if left:
        x1, x2 = 4.18, 8.22
        outer_x = 7.82
        angle = -5
    else:
        x1, x2 = -8.22, -4.18
        outer_x = -8.22
        angle = 5
    width = x2 - x1
    cubes = [
        # Three vertically overlapping cloth masses form one substantial cap sleeve.
        cube([x1, 20.04, -2.72], [width, 1.36, 5.44], material, pivot=[(x1+x2)/2,20.72,0], rotation=[0,0,angle]),
        cube([x1 + 0.34, 19.18, -2.88], [width - 0.34, 1.18, 5.76], material, pivot=[(x1+x2)/2,19.77,0], rotation=[0,0,angle]),
        cube([x1 + 0.94, 18.46, -2.48], [width - 1.14, 0.94, 4.96], "shadow", pivot=[(x1+x2)/2,18.93,0], rotation=[0,0,angle]),
        # Gold top seam and outer trim make it read as gi fabric, not armour.
        cube([x1 + 0.12, 21.30, -2.58], [width - 0.24, 0.24, 5.16], accent, pivot=[(x1+x2)/2,21.42,0], rotation=[0,0,angle]),
    ]
    if left:
        cubes += [
            cube([outer_x, 18.80, -2.44], [0.22, 2.48, 4.88], accent, pivot=[7.93,20.04,0], rotation=[0,0,angle]),
            cube([7.18, 18.54, -2.18], [0.50, 1.30, 4.36], "charcoal", pivot=[7.43,19.19,0], rotation=[0,0,angle]),
        ]
    else:
        cubes += [
            cube([outer_x, 18.80, -2.44], [0.22, 2.48, 4.88], accent, pivot=[-8.11,20.04,0], rotation=[0,0,angle]),
            cube([-7.68, 18.54, -2.18], [0.50, 1.30, 4.36], "dojo_black", pivot=[-7.43,19.19,0], rotation=[0,0,angle]),
        ]
    return {"name": name, "parent": parent, "pivot": [4.5 if left else -4.5, 20, 0], "cubes": cubes}


def champion_belt_and_coat() -> dict:
    return {
        "name": "ouros_champion_belt_sash",
        "parent": "torso",
        "pivot": [0, 12.0, 0],
        "cubes": [
            cube([-4.34, 11.42, -3.42], [8.68, 1.18, 0.42], "shadow"),
            cube([-4.34, 11.42, 3.00], [8.68, 1.18, 0.42], "shadow"),
            cube([-4.46, 11.42, -3.00], [0.36, 1.18, 6.00], "shadow"),
            cube([4.10, 11.42, -3.00], [0.36, 1.18, 6.00], "shadow"),
            # Black knot first, with smaller gold/green identity insert.
            cube([-1.82, 11.14, -3.80], [3.64, 1.58, 0.48], "dojo_black"),
            cube([-0.88, 11.28, -4.02], [1.76, 1.22, 0.22], "gold_dark"),
            cube([-0.34, 11.50, -4.18], [0.68, 0.76, 0.16], "cobra_green"),
            # Asymmetric tied belt tails.
            cube([-1.48, 7.18, -3.62], [1.18, 4.24, 0.34], "dojo_black", pivot=[-0.89,11.26,-3.45], rotation=[-5,0,8]),
            cube([0.40, 6.50, -3.60], [1.10, 4.92, 0.34], "shadow", pivot=[0.95,11.26,-3.43], rotation=[-5,0,-8]),
            cube([-1.28, 7.14, -3.86], [0.78, 0.24, 0.16], "gold", pivot=[-0.89,7.26,-3.78], rotation=[-5,0,8]),
            cube([0.56, 6.46, -3.84], [0.76, 0.24, 0.16], "gold_dark", pivot=[0.94,6.58,-3.76], rotation=[-5,0,-8]),
            # Split jacket skirts extend the gi below the belt without becoming a box.
            cube([-4.02, 6.42, 3.16], [3.54, 5.24, 0.48], "dojo_black", pivot=[-2.25,11.52,3.40], rotation=[-7,0,5]),
            cube([0.48, 7.04, 3.16], [3.44, 4.62, 0.48], "charcoal", pivot=[2.20,11.52,3.40], rotation=[-7,0,-5]),
            cube([-3.74, 6.38, 3.66], [3.02, 0.34, 0.18], "gold", pivot=[-2.23,6.55,3.75], rotation=[-7,0,5]),
            cube([0.72, 7.00, 3.66], [2.94, 0.30, 0.18], "gold_dark", pivot=[2.19,7.15,3.75], rotation=[-7,0,-5]),
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
            cube([x0, 19.40, -1.10], [x1 - x0, 0.34, 2.20], "wrap"),
            cube([x0, 21.10, -1.10], [x1 - x0, 0.34, 2.20], "cream"),
            cube([x0 + 0.54, 20.12, -1.42], [x1 - x0 - 1.08, 0.38, 0.20], "dojo_black"),
            cube([x0 + 0.82, 20.70, -1.48], [x1 - x0 - 1.64, 0.22, 0.16], accent),
        ],
    }


def knee_guard(name: str, parent: str, x0: float, left: bool, accent: str) -> dict:
    """Guard the official first 4x4x4 leg segment at y=10..14."""
    outer_x = x0 + (3.72 if left else -0.10)
    return {
        "name": name,
        "parent": parent,
        "pivot": [x0 + 2.0, 12.0, 0],
        "cubes": [
            cube([x0 + 0.42, 10.68, -2.22], [3.16, 2.56, 0.26], "dojo_black"),
            cube([x0 + 0.66, 12.90, -2.42], [2.68, 0.24, 0.16], accent),
            cube([x0 + 0.66, 10.60, -2.42], [2.68, 0.24, 0.16], "cream"),
            cube([outer_x, 10.56, -1.82], [0.28, 2.82, 3.64], accent),
            cube([x0 + 1.42, 11.30, -2.54], [1.16, 1.12, 0.14], "gold_dark" if left else "gold"),
            cube([x0 + 1.74, 11.62, -2.70], [0.52, 0.52, 0.12], "cobra_green"),
        ],
    }


def kick_guard(name: str, parent: str, x0: float, y0: float, left: bool, accent: str) -> dict:
    """Layered cloth plus a segmented front/outer guard for one 3x3x3 telescoping segment."""
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
        lower_gi(),
        upper_gi(),
        shoulder_cap("ouros_champion_left_shoulder", "arm_left", True),
        shoulder_cap("ouros_champion_right_shoulder", "arm_right", False),
        champion_belt_and_coat(),
        forearm_guard("ouros_champion_left_forearm", "arm_left2", True),
        forearm_guard("ouros_champion_right_forearm", "arm_right2", False),
        knee_guard("ouros_champion_left_knee", "leg_left", 0.5, True, "gold"),
        knee_guard("ouros_champion_right_knee", "leg_right", -4.5, False, "gold_dark"),
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
        "artDirection": "final v7: body-fitted high V gi + arm-root cap sleeves + knee-to-foot kick-guard hierarchy",
        "presentationOnly": True,
    }
    (args.output_root / "build-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
