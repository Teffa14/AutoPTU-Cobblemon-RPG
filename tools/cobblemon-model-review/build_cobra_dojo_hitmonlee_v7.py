#!/usr/bin/env python3
"""Build Hitmonlee Cobra Dojo v7 from exact Cobblemon 1.7.3 Hitmonlee.

This fitted v7 pass follows the approved clothing reference while respecting the
real Hitmonlee rig:
- a lower gi covers the biological torso as clothing instead of floating panels;
- an upper gi/collar is attached to torso2 and rises around the lower head in a V;
- torso-root shoulder bridges meet fully enclosed short gi sleeves attached to
  arm_left/arm_right, so the shoulders are covered instead of exposed;
- dedicated knee cuffs plus full layered sleeves on every telescoping leg segment
  make the uniform read one size larger and properly fitted from hip to foot;
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
            cube([-4.28, 11.86, -3.34], [4.28, 3.72, 0.38], "dojo_black"),
            cube([0.00, 11.86, -3.34], [4.28, 3.72, 0.38], "charcoal"),
            cube([-4.34, 11.82, -2.96], [0.42, 5.50, 5.92], "dojo_black"),
            cube([3.92, 11.82, -2.96], [0.42, 5.50, 5.92], "charcoal"),
            cube([-4.16, 11.84, 3.02], [8.32, 5.48, 0.38], "dojo_black"),
            cube([-4.18, 11.84, -3.58], [8.36, 0.24, 0.18], "gold_dark"),
            cube([-4.48, 12.04, -2.54], [0.18, 4.92, 5.08], "gold"),
            cube([4.30, 12.04, -2.54], [0.18, 4.92, 5.08], "gold_dark"),
            cube([-4.12, 15.12, -3.48], [2.88, 2.28, 0.24], "dojo_black", pivot=[-2.68, 16.24, -3.36], rotation=[0, 0, -5]),
            cube([1.24, 15.12, -3.48], [2.88, 2.28, 0.24], "charcoal", pivot=[2.68, 16.24, -3.36], rotation=[0, 0, 5]),
            cube([1.58, 13.38, -3.62], [1.56, 1.56, 0.18], "gold_dark", pivot=[2.36, 14.16, -3.53], rotation=[0, 0, 45]),
            cube([2.04, 13.84, -3.82], [0.64, 0.64, 0.14], "cobra_green"),
            cube([1.82, 14.56, -3.78], [1.08, 0.22, 0.14], "gold"),
            cube([-3.22, 12.80, 3.44], [2.90, 0.50, 0.18], "gold", pivot=[-1.77, 13.05, 3.53], rotation=[0, 0, -34]),
            cube([0.32, 12.80, 3.44], [2.90, 0.50, 0.18], "gold", pivot=[1.77, 13.05, 3.53], rotation=[0, 0, 34]),
            cube([-1.00, 13.50, 3.46], [2.00, 2.00, 0.18], "cobra_green", pivot=[0, 14.50, 3.55], rotation=[0, 0, 45]),
            cube([-2.00, 15.30, 3.46], [4.00, 0.38, 0.18], "gold_dark"),
            cube([-0.40, 15.68, 3.48], [0.80, 1.30, 0.16], "gold"),
        ],
    }


def upper_gi() -> dict:
    """High V collar plus torso-owned shoulder bridges beneath Hitmonlee's eyes."""
    return {
        "name": "ouros_champion_upper_gi",
        "parent": "torso2",
        "pivot": [0, 17.8, 0],
        "cubes": [
            cube([-4.72, 16.00, -3.72], [2.98, 3.20, 0.34], "dojo_black", pivot=[-3.23, 17.58, -3.55], rotation=[0, 0, -5]),
            cube([1.74, 16.00, -3.72], [2.98, 3.20, 0.34], "charcoal", pivot=[3.23, 17.58, -3.55], rotation=[0, 0, 5]),
            cube([-3.60, 15.94, -3.94], [0.58, 4.20, 0.18], "shadow", pivot=[-3.31, 17.98, -3.85], rotation=[0, 0, -38]),
            cube([3.02, 15.94, -3.94], [0.58, 4.20, 0.18], "shadow", pivot=[3.31, 17.98, -3.85], rotation=[0, 0, 38]),
            cube([-3.32, 16.02, -4.10], [0.32, 4.08, 0.14], "gold", pivot=[-3.16, 18.00, -4.03], rotation=[0, 0, -38]),
            cube([3.00, 16.02, -4.10], [0.32, 4.08, 0.14], "gold", pivot=[3.16, 18.00, -4.03], rotation=[0, 0, 38]),
            cube([-4.86, 16.02, -3.28], [0.34, 2.94, 6.56], "dojo_black"),
            cube([4.52, 16.02, -3.28], [0.34, 2.94, 6.56], "charcoal"),
            cube([-4.58, 16.02, 3.50], [9.16, 3.04, 0.32], "dojo_black"),
            cube([-4.26, 18.78, 3.80], [8.52, 0.28, 0.16], "gold_dark"),
            cube([-1.32, 16.04, -3.86], [1.04, 0.30, 0.16], "gold_dark", pivot=[-0.80, 16.19, -3.78], rotation=[0, 0, -20]),
            cube([0.28, 16.04, -3.86], [1.04, 0.30, 0.16], "gold_dark", pivot=[0.80, 16.19, -3.78], rotation=[0, 0, 20]),
            # Torso-owned shoulder bridges close the skin gap between collar and arm sleeves.
            cube([3.72, 18.36, -2.92], [2.54, 1.18, 5.84], "charcoal", pivot=[4.58, 18.95, 0], rotation=[0, 0, -4]),
            cube([-6.26, 18.36, -2.92], [2.54, 1.18, 5.84], "dojo_black", pivot=[-4.58, 18.95, 0], rotation=[0, 0, 4]),
            cube([3.92, 17.20, -3.46], [1.98, 1.50, 0.36], "charcoal", pivot=[4.62, 17.95, -3.28], rotation=[0, 0, -4]),
            cube([-5.90, 17.20, -3.46], [1.98, 1.50, 0.36], "dojo_black", pivot=[-4.62, 17.95, -3.28], rotation=[0, 0, 4]),
            cube([3.92, 17.20, 3.10], [1.98, 1.50, 0.36], "charcoal", pivot=[4.62, 17.95, 3.28], rotation=[0, 0, -4]),
            cube([-5.90, 17.20, 3.10], [1.98, 1.50, 0.36], "dojo_black", pivot=[-4.62, 17.95, 3.28], rotation=[0, 0, 4]),
            cube([4.02, 19.38, -2.66], [1.72, 0.22, 5.32], "gold_dark", pivot=[4.60, 19.49, 0], rotation=[0, 0, -4]),
            cube([-5.74, 19.38, -2.66], [1.72, 0.22, 5.32], "gold", pivot=[-4.60, 19.49, 0], rotation=[0, 0, 4]),
        ],
    }


def shoulder_cap(name: str, parent: str, left: bool) -> dict:
    """Fully enclosed short gi sleeve attached to the official arm-root bone."""
    accent = "gold" if left else "gold_dark"
    material = "dojo_black" if left else "charcoal"
    secondary = "charcoal" if left else "dojo_black"
    if left:
        x1, x2 = 3.90, 9.15
        outer_wall_x = 8.73
        inner_wall_x = 3.90
        angle = -4
    else:
        x1, x2 = -9.15, -3.90
        outer_wall_x = -9.15
        inner_wall_x = -4.20
        angle = 4
    width = x2 - x1
    mid = (x1 + x2) / 2
    cubes = [
        # Top, front, back and side walls create one continuous cloth sleeve around the shoulder.
        cube([x1, 20.46, -3.02], [width, 1.12, 6.04], material, pivot=[mid, 21.02, 0], rotation=[0, 0, angle]),
        cube([x1 + 0.12, 18.02, -3.26], [width - 0.12, 2.78, 0.42], material, pivot=[mid, 19.41, -3.05], rotation=[0, 0, angle]),
        cube([x1 + 0.12, 18.02, 2.84], [width - 0.12, 2.78, 0.42], secondary, pivot=[mid, 19.41, 3.05], rotation=[0, 0, angle]),
        cube([outer_wall_x, 18.06, -2.84], [0.42, 2.82, 5.68], material, pivot=[outer_wall_x + 0.21, 19.47, 0], rotation=[0, 0, angle]),
        cube([inner_wall_x, 18.48, -2.58], [0.30, 2.18, 5.16], secondary, pivot=[inner_wall_x + 0.15, 19.57, 0], rotation=[0, 0, angle]),
        # Lower cuff overlaps the upper arm, making the garment look correctly sized.
        cube([x1 + 0.58, 17.72, -3.04], [width - 1.02, 0.48, 0.34], "shadow", pivot=[mid, 17.96, -2.87], rotation=[0, 0, angle]),
        cube([x1 + 0.58, 17.72, 2.70], [width - 1.02, 0.48, 0.34], "shadow", pivot=[mid, 17.96, 2.87], rotation=[0, 0, angle]),
        cube([x1 + 0.72, 17.62, -2.68], [width - 1.26, 0.30, 5.36], secondary, pivot=[mid, 17.77, 0], rotation=[0, 0, angle]),
        # Gold piping is subordinate to the black fabric mass.
        cube([x1 + 0.14, 21.50, -2.84], [width - 0.28, 0.22, 5.68], accent, pivot=[mid, 21.61, 0], rotation=[0, 0, angle]),
        cube([outer_wall_x - (0.02 if left else -0.22), 18.28, -2.62], [0.22, 2.40, 5.24], accent, pivot=[outer_wall_x + 0.11, 19.48, 0], rotation=[0, 0, angle]),
        cube([x1 + 0.82, 18.08, -3.50], [width - 1.54, 0.22, 0.16], accent, pivot=[mid, 18.19, -3.42], rotation=[0, 0, angle]),
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
            cube([-1.82, 11.14, -3.80], [3.64, 1.58, 0.48], "dojo_black"),
            cube([-0.88, 11.28, -4.02], [1.76, 1.22, 0.22], "gold_dark"),
            cube([-0.34, 11.50, -4.18], [0.68, 0.76, 0.16], "cobra_green"),
            cube([-1.48, 7.18, -3.62], [1.18, 4.24, 0.34], "dojo_black", pivot=[-0.89,11.26,-3.45], rotation=[-5,0,8]),
            cube([0.40, 6.50, -3.60], [1.10, 4.92, 0.34], "shadow", pivot=[0.95,11.26,-3.43], rotation=[-5,0,-8]),
            cube([-1.28, 7.14, -3.86], [0.78, 0.24, 0.16], "gold", pivot=[-0.89,7.26,-3.78], rotation=[-5,0,8]),
            cube([0.56, 6.46, -3.84], [0.76, 0.24, 0.16], "gold_dark", pivot=[0.94,6.58,-3.76], rotation=[-5,0,-8]),
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
    """Full trouser/knee cuff around the official first 4x4x4 leg segment."""
    material = "dojo_black" if left else "charcoal"
    secondary = "charcoal" if left else "dojo_black"
    return {
        "name": name,
        "parent": parent,
        "pivot": [x0 + 2.0, 12.0, 0],
        "cubes": [
            # Full front/back/side garment shell is deliberately wider than the biological segment.
            cube([x0 + 0.18, 10.34, -2.30], [3.64, 3.18, 0.34], material),
            cube([x0 + 0.18, 10.34, 1.96], [3.64, 3.18, 0.34], secondary),
            cube([x0 - 0.12, 10.42, -1.98], [0.34, 3.02, 3.96], secondary),
            cube([x0 + 3.78, 10.42, -1.98], [0.34, 3.02, 3.96], material),
            # Wide upper and lower cuffs visually connect the hip section to the telescoping guards.
            cube([x0 - 0.04, 13.34, -2.14], [4.08, 0.34, 4.28], "shadow"),
            cube([x0 + 0.06, 13.62, -2.04], [3.88, 0.22, 4.08], accent),
            cube([x0 - 0.02, 10.12, -2.10], [4.04, 0.34, 4.20], "cream"),
            cube([x0 + 0.12, 9.92, -1.94], [3.76, 0.20, 3.88], "wrap"),
            # Central strike plate and crest sit on the fabric, rather than replacing it.
            cube([x0 + 0.62, 10.74, -2.54], [2.76, 2.42, 0.22], "shadow"),
            cube([x0 + 0.82, 12.94, -2.72], [2.36, 0.20, 0.14], accent),
            cube([x0 + 1.38, 11.26, -2.74], [1.24, 1.20, 0.14], "gold_dark" if left else "gold"),
            cube([x0 + 1.74, 11.60, -2.90], [0.52, 0.52, 0.12], "cobra_green"),
        ],
    }


def kick_guard(name: str, parent: str, x0: float, y0: float, left: bool, accent: str) -> dict:
    """Full cloth sleeve plus layered strike hardware for one telescoping 3x3 segment."""
    material = "dojo_black" if left else "charcoal"
    secondary = "charcoal" if left else "dojo_black"
    strap_rotation = -14 if left else 14
    outer_x = x0 + (2.90 if left else -0.18)
    return {
        "name": name,
        "parent": parent,
        "pivot": [x0 + 1.5, y0 + 1.5, 0],
        "cubes": [
            # Four-sided sleeve makes every leg segment look intentionally clothed and larger.
            cube([x0 + 0.04, y0 + 0.18, -1.88], [2.92, 2.64, 0.30], material),
            cube([x0 + 0.04, y0 + 0.18, 1.58], [2.92, 2.64, 0.30], secondary),
            cube([x0 - 0.18, y0 + 0.24, -1.60], [0.30, 2.52, 3.20], secondary),
            cube([x0 + 2.88, y0 + 0.24, -1.60], [0.30, 2.52, 3.20], material),
            # Broad top and bottom cuffs preserve the layered wrap language from the reference.
            cube([x0 - 0.08, y0 + 2.70, -1.76], [3.16, 0.28, 3.52], "cream"),
            cube([x0 + 0.02, y0 + 2.94, -1.66], [2.96, 0.18, 3.32], accent),
            cube([x0 - 0.04, y0 + 0.04, -1.72], [3.08, 0.30, 3.44], "wrap"),
            cube([x0 + 0.08, y0 - 0.12, -1.60], [2.84, 0.18, 3.20], "shadow"),
            # Diagonal strap and front strike plate add hierarchy without shrinking the garment mass.
            cube([x0 + 0.30, y0 + 1.20, -2.08], [2.40, 0.28, 0.16], "shadow", pivot=[x0 + 1.50, y0 + 1.34, -2.00], rotation=[0, 0, strap_rotation]),
            cube([x0 + 0.54, y0 + 0.70, -2.04], [1.92, 1.56, 0.22], material),
            cube([x0 + 0.72, y0 + 0.56, -2.26], [1.56, 0.20, 0.14], accent),
            cube([outer_x, y0 + 0.42, -1.92], [0.24, 2.16, 0.40], accent),
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
            cube([x0 + 0.16, 1.70, -0.18], [3.68, 0.30, 1.82], "charcoal" if left else "dojo_black"),
            cube([x0 + 0.28, 1.92, -0.04], [3.44, 0.18, 1.54], accent),
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
        "artDirection": "fitted v7: closed shoulder sleeves + torso bridges + oversized knee-to-foot leg sleeves",
        "presentationOnly": True,
    }
    (args.output_root / "build-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
