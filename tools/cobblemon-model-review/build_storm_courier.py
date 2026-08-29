#!/usr/bin/env python3
"""Build an aggressive Storm Courier cosmetic around official Cobblemon Pikachu.

Inputs must be the exact male/female geometry extracted from the pinned official
Cobblemon 1.7.3 Fabric JAR. Existing bones, cubes, pivots, locators, hierarchy,
UVs and animation-facing names are never rewritten. The only allowed model
changes are the geometry identifier and appended `ouros_*` cosmetic bones.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from PIL import Image

PALETTE = {
    "charcoal": (37, 42, 48, 255),
    "leather": (91, 58, 40, 255),
    "copper": (188, 105, 54, 255),
    "glass": (105, 211, 247, 150),
    "canvas": (45, 74, 83, 255),
    "brass": (219, 181, 84, 255),
    "cream": (219, 214, 196, 255),
    "navy": (25, 52, 66, 255),
}
PIXELS = {name: (index, 63) for index, name in enumerate(PALETTE)}
FACES = ("north", "east", "south", "west", "up", "down")


def solid_uv(material: str) -> dict:
    x, y = PIXELS[material]
    return {face: {"uv": [x, y], "uv_size": [1, 1]} for face in FACES}


def cube(origin, size, material, **extra) -> dict:
    value = {"origin": origin, "size": size, "uv": solid_uv(material)}
    value.update({key: item for key, item in extra.items() if item is not None})
    return value


def goggles_bone() -> dict:
    """Large storm visor with an unmistakable copper/brass lightning brow."""
    cubes = [
        cube([-3.92, 14.48, -4.30], [2.50, 1.92, 0.14], "glass"),
        cube([1.42, 14.48, -4.30], [2.50, 1.92, 0.14], "glass"),
        cube([-4.10, 16.28, -4.38], [2.86, 0.30, 0.24], "copper"),
        cube([1.24, 16.28, -4.38], [2.86, 0.30, 0.24], "copper"),
        cube([-1.78, 15.30, -4.40], [3.56, 0.30, 0.24], "copper"),
        cube([-0.56, 14.90, -4.42], [1.12, 0.32, 0.22], "brass"),
        cube([-3.46, 16.62, -4.32], [1.52, 0.28, 0.20], "brass", pivot=[-2.70, 16.76, -4.22], rotation=[0, 0, -18]),
        cube([1.94, 16.62, -4.32], [1.52, 0.28, 0.20], "brass", pivot=[2.70, 16.76, -4.22], rotation=[0, 0, 18]),
    ]
    for x in (-4.10, 1.24):
        cubes.extend(
            [
                cube([x, 14.32, -4.36], [2.86, 0.22, 0.22], "charcoal"),
                cube([x, 14.32, -4.36], [0.22, 2.26, 0.22], "charcoal"),
                cube([x + 2.64, 14.32, -4.36], [0.22, 2.26, 0.22], "charcoal"),
            ]
        )
    cubes.extend(
        [
            cube([-4.92, 15.12, -3.78], [0.88, 0.38, 1.72], "leather"),
            cube([4.04, 15.12, -3.78], [0.88, 0.38, 1.72], "leather"),
            cube([-5.00, 15.12, -2.20], [0.34, 0.38, 1.54], "navy"),
            cube([4.66, 15.12, -2.20], [0.34, 0.38, 1.54], "navy"),
            cube([-4.12, 16.12, -4.46], [0.38, 0.38, 0.18], "brass"),
            cube([-1.50, 16.12, -4.46], [0.38, 0.38, 0.18], "brass"),
            cube([1.12, 16.12, -4.46], [0.38, 0.38, 0.18], "brass"),
            cube([3.74, 16.12, -4.46], [0.38, 0.38, 0.18], "brass"),
            cube([4.76, 15.44, -3.34], [0.38, 0.88, 0.94], "copper"),
            cube([4.86, 15.62, -3.48], [0.18, 0.48, 0.22], "glass"),
        ]
    )
    return {
        "name": "ouros_storm_goggles",
        "parent": "head_angle",
        "pivot": [0, 15.5, -4.0],
        "cubes": cubes,
    }


def cowl_bone() -> dict:
    """Open storm cowl with a high collar and angular crown guards."""
    return {
        "name": "ouros_storm_cowl",
        "parent": "head_angle",
        "pivot": [0, 16.6, 2.0],
        "cubes": [
            # Rear shell is narrow enough to preserve the original head silhouette from front.
            cube([-4.74, 14.18, 3.92], [9.48, 5.10, 0.44], "navy"),
            cube([-5.08, 13.90, 1.22], [0.48, 4.86, 2.90], "canvas"),
            cube([4.60, 13.90, 1.22], [0.48, 4.86, 2.90], "canvas"),
            # High collar blades become visible beside the cheeks and below the ear bases.
            cube([-5.62, 12.72, 0.20], [1.14, 3.26, 2.26], "navy", pivot=[-5.05, 14.35, 1.33], rotation=[0, 0, -10]),
            cube([4.48, 12.72, 0.20], [1.14, 3.26, 2.26], "navy", pivot=[5.05, 14.35, 1.33], rotation=[0, 0, 10]),
            cube([-5.72, 13.10, 0.12], [0.26, 2.42, 2.10], "brass", pivot=[-5.59, 14.31, 1.17], rotation=[0, 0, -10]),
            cube([5.46, 13.10, 0.12], [0.26, 2.42, 2.10], "brass", pivot=[5.59, 14.31, 1.17], rotation=[0, 0, 10]),
            # Crown wings sit outside, not between, the original ears.
            cube([-6.48, 18.46, 1.20], [2.98, 0.46, 1.36], "navy", pivot=[-4.99, 18.69, 1.88], rotation=[0, 0, -28]),
            cube([3.50, 18.46, 1.20], [2.98, 0.46, 1.36], "navy", pivot=[4.99, 18.69, 1.88], rotation=[0, 0, 28]),
            cube([-6.30, 18.84, 1.30], [2.46, 0.22, 1.02], "copper", pivot=[-5.07, 18.95, 1.81], rotation=[0, 0, -28]),
            cube([3.84, 18.84, 1.30], [2.46, 0.22, 1.02], "copper", pivot=[5.07, 18.95, 1.81], rotation=[0, 0, 28]),
            cube([-5.98, 19.12, 1.42], [1.62, 0.16, 0.76], "glass", pivot=[-5.17, 19.20, 1.80], rotation=[0, 0, -28]),
            cube([4.36, 19.12, 1.42], [1.62, 0.16, 0.76], "glass", pivot=[5.17, 19.20, 1.80], rotation=[0, 0, 28]),
        ],
    }


def mantle_bone() -> dict:
    """Broad storm mantle with blade-like pauldrons and long split coat tails."""
    return {
        "name": "ouros_storm_mantle",
        "parent": "torso2",
        "pivot": [0, 10.2, 1.0],
        "cubes": [
            # Dramatic shoulder caps. Their outer tips are intentionally beyond the arms.
            cube([-7.64, 9.92, -2.40], [4.12, 1.50, 4.82], "navy", pivot=[-5.58, 10.67, 0.01], rotation=[0, 0, -12]),
            cube([3.52, 9.92, -2.40], [4.12, 1.50, 4.82], "navy", pivot=[5.58, 10.67, 0.01], rotation=[0, 0, 12]),
            cube([-7.78, 11.24, -2.20], [4.30, 0.34, 4.42], "copper", pivot=[-5.63, 11.41, 0.01], rotation=[0, 0, -12]),
            cube([3.48, 11.24, -2.20], [4.30, 0.34, 4.42], "copper", pivot=[5.63, 11.41, 0.01], rotation=[0, 0, 12]),
            cube([-7.18, 9.62, -1.96], [3.42, 0.30, 3.92], "brass", pivot=[-5.47, 9.77, 0.00], rotation=[0, 0, -12]),
            cube([3.76, 9.62, -1.96], [3.42, 0.30, 3.92], "brass", pivot=[5.47, 9.77, 0.00], rotation=[0, 0, 12]),
            # Outer lightning fins make the silhouette angular rather than boxy.
            cube([-8.42, 10.36, -0.80], [2.22, 0.40, 1.38], "charcoal", pivot=[-7.31, 10.56, -0.11], rotation=[0, 0, -34]),
            cube([6.20, 10.36, -0.80], [2.22, 0.40, 1.38], "charcoal", pivot=[7.31, 10.56, -0.11], rotation=[0, 0, 34]),
            cube([-8.16, 10.76, -0.72], [1.66, 0.22, 1.08], "glass", pivot=[-7.33, 10.87, -0.18], rotation=[0, 0, -34]),
            cube([6.50, 10.76, -0.72], [1.66, 0.22, 1.08], "glass", pivot=[7.33, 10.87, -0.18], rotation=[0, 0, 34]),
            # Long split coat tails. Kept behind the torso and above the feet.
            cube([-4.54, 5.36, 4.32], [3.94, 6.24, 0.52], "canvas", pivot=[-2.57, 10.72, 4.58], rotation=[-10, 0, 11]),
            cube([0.60, 5.36, 4.32], [3.94, 6.24, 0.52], "navy", pivot=[2.57, 10.72, 4.58], rotation=[-10, 0, -11]),
            cube([-4.32, 5.38, 4.80], [3.62, 0.28, 0.22], "copper", pivot=[-2.51, 5.52, 4.91], rotation=[-10, 0, 11]),
            cube([0.70, 5.38, 4.80], [3.62, 0.28, 0.22], "copper", pivot=[2.51, 5.52, 4.91], rotation=[-10, 0, -11]),
            cube([-3.66, 5.78, 4.86], [2.46, 0.18, 0.14], "brass", pivot=[-2.43, 5.87, 4.93], rotation=[-10, 0, 11]),
            cube([1.20, 5.78, 4.86], [2.46, 0.18, 0.14], "brass", pivot=[2.43, 5.87, 4.93], rotation=[-10, 0, -11]),
        ],
    }


def harness_bone() -> dict:
    """Heavy courier harness with an oversized storm-core chest assembly."""
    return {
        "name": "ouros_storm_harness",
        "parent": "torso2",
        "pivot": [0, 8.75, 0.5],
        "cubes": [
            cube([-4.18, 8.52, -3.90], [8.36, 0.62, 0.32], "leather", pivot=[0, 8.83, -3.74], rotation=[0, 0, 30]),
            cube([-4.18, 8.52, -3.88], [8.36, 0.62, 0.32], "navy", pivot=[0, 8.83, -3.72], rotation=[0, 0, -30]),
            cube([-4.42, 6.60, -3.88], [8.84, 0.58, 0.32], "leather"),
            cube([-4.62, 6.80, -3.48], [0.38, 2.52, 6.96], "navy"),
            cube([4.24, 6.80, -3.48], [0.38, 2.52, 6.96], "navy"),
            cube([-3.02, 10.66, -3.94], [6.04, 0.46, 0.30], "leather"),
            cube([-1.92, 10.48, -4.06], [3.84, 0.24, 0.22], "copper"),
            # Signature storm core: this must remain readable even when arms are raised.
            cube([-1.38, 9.00, -4.16], [2.76, 2.24, 0.52], "brass"),
            cube([-0.96, 9.42, -4.30], [1.92, 1.40, 0.16], "glass"),
            cube([-0.28, 9.58, -4.34], [0.56, 1.02, 0.14], "copper", pivot=[0, 10.09, -4.27], rotation=[0, 0, -34]),
            cube([-0.74, 8.38, -4.12], [1.48, 0.46, 0.32], "copper"),
            cube([-0.42, 8.72, -4.22], [0.84, 0.66, 0.14], "glass"),
            cube([-4.16, 9.70, -4.04], [0.64, 0.64, 0.28], "brass"),
            cube([3.52, 9.70, -4.04], [0.64, 0.64, 0.28], "brass"),
            cube([-4.64, 7.10, -2.50], [0.90, 1.52, 1.72], "canvas"),
            cube([3.74, 7.10, -2.50], [0.90, 1.52, 1.72], "canvas"),
            cube([-4.76, 8.38, -2.60], [1.14, 0.36, 1.94], "leather"),
            cube([3.62, 8.38, -2.60], [1.14, 0.36, 1.94], "leather"),
            cube([-4.84, 7.66, -2.72], [0.20, 0.58, 0.64], "brass"),
            cube([4.64, 7.66, -2.72], [0.20, 0.58, 0.64], "brass"),
        ],
    }


def pack_bone() -> dict:
    """Expedition power-pack sized to support the large storm frame."""
    return {
        "name": "ouros_storm_pack",
        "parent": "torso2",
        "pivot": [0, 8.75, 4.5],
        "cubes": [
            cube([-3.12, 6.46, 4.24], [6.64, 4.94, 2.00], "canvas"),
            cube([-2.82, 5.84, 4.44], [6.04, 0.78, 1.56], "navy"),
            cube([-3.26, 10.58, 4.08], [6.92, 1.06, 2.24], "leather"),
            cube([-2.98, 10.26, 6.12], [6.36, 0.34, 0.22], "copper"),
            cube([-4.10, 7.10, 4.34], [1.10, 2.82, 1.72], "navy"),
            cube([3.56, 7.10, 4.34], [1.10, 2.82, 1.72], "navy"),
            cube([-4.22, 9.52, 4.26], [1.34, 0.50, 1.92], "leather"),
            cube([3.44, 9.52, 4.26], [1.34, 0.50, 1.92], "leather"),
            cube([-2.56, 11.64, 4.40], [5.52, 0.96, 1.38], "cream"),
            cube([-1.82, 11.52, 4.28], [0.46, 1.18, 1.62], "leather"),
            cube([1.56, 11.52, 4.28], [0.46, 1.18, 1.62], "leather"),
            cube([-2.02, 6.62, 6.16], [0.46, 3.66, 0.22], "leather"),
            cube([1.96, 6.62, 6.16], [0.46, 3.66, 0.22], "leather"),
            cube([-2.18, 7.90, 6.30], [0.76, 0.76, 0.16], "brass"),
            cube([1.80, 7.90, 6.30], [0.76, 0.76, 0.16], "brass"),
            # Large rear lightning mark.
            cube([-0.12, 8.52, 6.32], [0.56, 1.78, 0.16], "copper", pivot=[0.16, 9.41, 6.40], rotation=[0, 0, -20]),
            cube([0.20, 7.70, 6.34], [0.52, 1.34, 0.16], "brass", pivot=[0.46, 8.37, 6.42], rotation=[0, 0, 32]),
            cube([4.30, 7.18, 4.56], [0.64, 2.10, 1.12], "glass"),
            cube([4.16, 9.26, 4.44], [0.92, 0.38, 1.36], "copper"),
            cube([4.16, 6.84, 4.44], [0.92, 0.34, 1.36], "brass"),
            cube([-4.84, 7.06, 4.48], [0.62, 2.20, 1.24], "cream"),
            cube([-4.96, 9.04, 4.36], [0.86, 0.40, 1.50], "leather"),
        ],
    }


def coils_bone() -> dict:
    """Wide twin storm pylons with forked lightning crowns visible from the front."""
    return {
        "name": "ouros_storm_coils",
        "parent": "torso2",
        "pivot": [0, 10.0, 4.2],
        "cubes": [
            # Left mast moved well outside the head silhouette.
            cube([-6.30, 9.36, 3.76], [0.72, 5.74, 0.72], "charcoal", pivot=[-5.94, 12.23, 4.12], rotation=[0, 0, -10]),
            cube([-6.44, 10.10, 3.62], [1.00, 0.40, 1.00], "copper", pivot=[-5.94, 10.30, 4.12], rotation=[0, 0, -10]),
            cube([-6.44, 11.42, 3.62], [1.00, 0.40, 1.00], "brass", pivot=[-5.94, 11.62, 4.12], rotation=[0, 0, -10]),
            cube([-6.44, 12.74, 3.62], [1.00, 0.40, 1.00], "copper", pivot=[-5.94, 12.94, 4.12], rotation=[0, 0, -10]),
            cube([-6.34, 13.86, 3.68], [0.80, 1.16, 0.88], "glass", pivot=[-5.94, 14.44, 4.12], rotation=[0, 0, -10]),
            # Forked lightning crown extends outward and inward from the mast top.
            cube([-8.66, 14.82, 3.66], [2.80, 0.44, 0.92], "brass", pivot=[-7.26, 15.04, 4.12], rotation=[0, 0, -18]),
            cube([-6.00, 14.90, 3.66], [2.42, 0.40, 0.92], "copper", pivot=[-4.79, 15.10, 4.12], rotation=[0, 0, 26]),
            cube([-8.34, 15.20, 3.76], [1.62, 0.22, 0.70], "glass", pivot=[-7.53, 15.31, 4.11], rotation=[0, 0, -18]),
            # Right mast.
            cube([5.58, 9.36, 3.76], [0.72, 5.74, 0.72], "charcoal", pivot=[5.94, 12.23, 4.12], rotation=[0, 0, 10]),
            cube([5.44, 10.10, 3.62], [1.00, 0.40, 1.00], "copper", pivot=[5.94, 10.30, 4.12], rotation=[0, 0, 10]),
            cube([5.44, 11.42, 3.62], [1.00, 0.40, 1.00], "brass", pivot=[5.94, 11.62, 4.12], rotation=[0, 0, 10]),
            cube([5.44, 12.74, 3.62], [1.00, 0.40, 1.00], "copper", pivot=[5.94, 12.94, 4.12], rotation=[0, 0, 10]),
            cube([5.54, 13.86, 3.68], [0.80, 1.16, 0.88], "glass", pivot=[5.94, 14.44, 4.12], rotation=[0, 0, 10]),
            cube([5.86, 14.82, 3.66], [2.80, 0.44, 0.92], "brass", pivot=[7.26, 15.04, 4.12], rotation=[0, 0, 18]),
            cube([3.58, 14.90, 3.66], [2.42, 0.40, 0.92], "copper", pivot=[4.79, 15.10, 4.12], rotation=[0, 0, -26]),
            cube([6.72, 15.20, 3.76], [1.62, 0.22, 0.70], "glass", pivot=[7.53, 15.31, 4.11], rotation=[0, 0, 18]),
            # Wide bridge makes the power-frame read as one deliberate object.
            cube([-5.72, 9.26, 3.60], [11.44, 0.44, 0.96], "navy"),
            cube([-2.02, 9.18, 4.50], [4.04, 0.58, 0.20], "glass"),
            cube([-2.46, 9.10, 4.54], [0.28, 0.74, 0.18], "brass"),
            cube([2.18, 9.10, 4.54], [0.28, 0.74, 0.18], "brass"),
        ],
    }


def tail_clamp_bone() -> dict:
    """Heavy grounding clamp fitted to the official tail plane."""
    return {
        "name": "ouros_storm_tail_clamp",
        "parent": "tail2",
        "pivot": [0, 9.75, 8.9],
        "cubes": [
            cube([-0.64, 9.02, 7.60], [1.28, 0.92, 3.10], "copper"),
            cube([-0.80, 8.86, 7.82], [1.60, 0.26, 2.66], "charcoal"),
            cube([-0.80, 10.02, 7.82], [1.60, 0.26, 2.66], "charcoal"),
            cube([-0.88, 9.26, 8.48], [1.76, 0.56, 1.08], "brass"),
            cube([-0.96, 9.38, 8.72], [1.92, 0.32, 0.60], "glass"),
            cube([-0.70, 9.08, 7.38], [1.40, 0.74, 0.30], "leather"),
            cube([-0.70, 9.08, 10.62], [1.40, 0.74, 0.30], "leather"),
            cube([-0.36, 9.44, 6.72], [0.26, 0.26, 0.78], "brass"),
            cube([0.10, 9.44, 10.80], [0.26, 0.26, 0.96], "brass"),
            cube([-0.30, 9.34, 8.06], [0.60, 0.38, 0.40], "navy"),
        ],
    }


def tail_vanes_bone() -> dict:
    """Large segmented conductor spine and side fins that hug the official tail."""
    return {
        "name": "ouros_storm_tail_vanes",
        "parent": "tail2",
        "pivot": [0, 11.0, 10.0],
        "cubes": [
            # Central spine is deliberately substantial but never replaces the flat tail mesh.
            cube([-0.50, 10.06, 10.56], [1.00, 0.36, 1.84], "navy"),
            cube([-0.44, 10.48, 11.58], [0.88, 0.32, 1.82], "copper"),
            cube([-0.38, 10.88, 12.58], [0.76, 0.30, 1.68], "brass"),
            cube([-0.32, 11.24, 13.46], [0.64, 0.28, 1.42], "glass"),
            cube([-0.28, 11.58, 14.18], [0.56, 0.26, 1.20], "copper"),
            # Blade fins widen the tail silhouette in three-quarter views.
            cube([-1.34, 10.28, 10.96], [0.76, 0.74, 1.16], "charcoal", pivot=[-0.96, 10.65, 11.54], rotation=[0, 0, -16]),
            cube([0.58, 10.28, 10.96], [0.76, 0.74, 1.16], "charcoal", pivot=[0.96, 10.65, 11.54], rotation=[0, 0, 16]),
            cube([-1.18, 10.94, 12.14], [0.64, 0.64, 1.04], "brass", pivot=[-0.86, 11.26, 12.66], rotation=[0, 0, -18]),
            cube([0.54, 10.94, 12.14], [0.64, 0.64, 1.04], "brass", pivot=[0.86, 11.26, 12.66], rotation=[0, 0, 18]),
            cube([-1.00, 11.48, 13.20], [0.52, 0.54, 0.88], "glass", pivot=[-0.74, 11.75, 13.64], rotation=[0, 0, -20]),
            cube([0.48, 11.48, 13.20], [0.52, 0.54, 0.88], "glass", pivot=[0.74, 11.75, 13.64], rotation=[0, 0, 20]),
        ],
    }


def build(source: Path, identifier: str) -> dict:
    data = json.loads(source.read_text(encoding="utf-8"))
    geometry = data["minecraft:geometry"][0]
    geometry["description"]["identifier"] = identifier
    geometry["bones"].extend(
        [
            goggles_bone(),
            cowl_bone(),
            mantle_bone(),
            harness_bone(),
            pack_bone(),
            coils_bone(),
            tail_clamp_bone(),
            tail_vanes_bone(),
        ]
    )
    return data


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--male", type=Path, required=True)
    parser.add_argument("--female", type=Path, required=True)
    parser.add_argument("--male-out", type=Path, required=True)
    parser.add_argument("--female-out", type=Path, required=True)
    parser.add_argument("--overlay-out", type=Path, required=True)
    args = parser.parse_args()

    targets = (
        (args.male, args.male_out, "geometry.ouros_storm_courier_pikachu_male"),
        (args.female, args.female_out, "geometry.ouros_storm_courier_pikachu_female"),
    )
    for source, output, identifier in targets:
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(
            json.dumps(build(source, identifier), separators=(",", ":")) + "\n",
            encoding="utf-8",
        )

    overlay = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    for name, color in PALETTE.items():
        overlay.putpixel(PIXELS[name], color)
    args.overlay_out.parent.mkdir(parents=True, exist_ok=True)
    overlay.save(args.overlay_out, optimize=True)


if __name__ == "__main__":
    main()
