#!/usr/bin/env python3
"""Build Storm Courier by appending premium cosmetic geometry to official Cobblemon Pikachu.

Inputs must be the exact male/female geometry extracted from the pinned official
Cobblemon 1.7.3 Fabric JAR. Existing bones, cubes, pivots, locators, hierarchy,
UVs and animation-facing names are never rewritten. The only allowed model
changes are the geometry identifier and four appended `ouros_*` cosmetic bones.
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
    """Layered courier goggles, fitted tightly to the existing face and eye line."""
    cubes = [
        # Lenses: thin enough to preserve the original eyes underneath.
        cube([-3.72, 14.70, -4.22], [2.12, 1.52, 0.10], "glass"),
        cube([1.60, 14.70, -4.22], [2.12, 1.52, 0.10], "glass"),
        # Copper brows give the equipment a crafted silhouette without covering the forehead.
        cube([-3.82, 16.18, -4.26], [2.32, 0.18, 0.16], "copper"),
        cube([1.50, 16.18, -4.26], [2.32, 0.18, 0.16], "copper"),
        # Bridge and lower nose brace.
        cube([-1.62, 15.34, -4.28], [3.24, 0.20, 0.16], "copper"),
        cube([-0.44, 15.04, -4.29], [0.88, 0.18, 0.17], "brass"),
    ]
    for x in (-3.84, 1.48):
        cubes.extend(
            [
                cube([x, 14.58, -4.27], [2.36, 0.18, 0.16], "charcoal"),
                cube([x, 14.58, -4.27], [0.18, 1.78, 0.16], "charcoal"),
                cube([x + 2.18, 14.58, -4.27], [0.18, 1.78, 0.16], "charcoal"),
            ]
        )
    cubes.extend(
        [
            # Temple straps wrap toward the side of the official head cube.
            cube([-4.44, 15.30, -3.62], [0.72, 0.24, 1.28], "leather"),
            cube([3.72, 15.30, -3.62], [0.72, 0.24, 1.28], "leather"),
            # Four small hardware rivets.
            cube([-3.96, 16.05, -4.34], [0.28, 0.28, 0.14], "brass"),
            cube([-1.62, 16.05, -4.34], [0.28, 0.28, 0.14], "brass"),
            cube([1.34, 16.05, -4.34], [0.28, 0.28, 0.14], "brass"),
            cube([3.68, 16.05, -4.34], [0.28, 0.28, 0.14], "brass"),
        ]
    )
    return {
        "name": "ouros_storm_goggles",
        "parent": "head_angle",
        "pivot": [0, 15.5, -4.0],
        "cubes": cubes,
    }


def harness_bone() -> dict:
    """Three-dimensional courier harness with crossed straps, waist wrap and storm clasp."""
    return {
        "name": "ouros_storm_harness",
        "parent": "torso2",
        "pivot": [0, 8.75, 0.5],
        "cubes": [
            # Crossed front straps. They sit just proud of torso2 instead of repainting it.
            cube(
                [-3.85, 8.72, -3.76],
                [7.70, 0.40, 0.22],
                "leather",
                pivot=[0, 8.92, -3.65],
                rotation=[0, 0, 27],
            ),
            cube(
                [-3.85, 8.72, -3.75],
                [7.70, 0.40, 0.22],
                "navy",
                pivot=[0, 8.92, -3.64],
                rotation=[0, 0, -27],
            ),
            # Lower belt plus side wraps provide visible depth in 3/4 views.
            cube([-4.18, 6.82, -3.74], [8.36, 0.42, 0.22], "leather"),
            cube([-4.42, 6.94, -3.42], [0.24, 1.86, 6.84], "navy"),
            cube([4.18, 6.94, -3.42], [0.24, 1.86, 6.84], "navy"),
            # Central weather clasp: brass frame, glass core, copper lower keeper.
            cube([-0.62, 7.70, -3.91], [1.24, 1.16, 0.28], "brass"),
            cube([-0.34, 7.98, -4.00], [0.68, 0.60, 0.10], "glass"),
            cube([-0.44, 7.48, -3.94], [0.88, 0.22, 0.22], "copper"),
            # Shoulder and belt hardware.
            cube([-3.58, 9.66, -3.92], [0.34, 0.34, 0.20], "brass"),
            cube([3.24, 9.66, -3.92], [0.34, 0.34, 0.20], "brass"),
            cube([-3.55, 6.72, -3.90], [0.30, 0.30, 0.18], "copper"),
            cube([3.25, 6.72, -3.90], [0.30, 0.30, 0.18], "copper"),
        ],
    }


def pack_bone() -> dict:
    """Compact expedition pack with flap, side pockets, bedroll, buckles and storm sigil."""
    return {
        "name": "ouros_storm_pack",
        "parent": "torso2",
        "pivot": [0, 8.75, 4.5],
        "cubes": [
            # Main pack: compact enough to keep the Pikachu silhouette, deep enough for 3/4 readability.
            cube([-2.60, 7.02, 4.36], [5.20, 3.78, 1.48], "canvas"),
            cube([-2.34, 6.48, 4.52], [4.68, 0.62, 1.18], "navy"),
            # Leather flap and reinforced top lip.
            cube([-2.76, 10.06, 4.25], [5.52, 0.78, 1.70], "leather"),
            cube([-2.48, 9.82, 5.72], [4.96, 0.26, 0.16], "copper"),
            # Side pouches visibly break the rectangular pack outline in 3/4.
            cube([-3.38, 7.52, 4.48], [0.84, 2.14, 1.20], "navy"),
            cube([2.54, 7.52, 4.48], [0.84, 2.14, 1.20], "navy"),
            cube([-3.46, 9.32, 4.42], [1.00, 0.38, 1.34], "leather"),
            cube([2.46, 9.32, 4.42], [1.00, 0.38, 1.34], "leather"),
            # Rolled weather cloth mounted across the top.
            cube([-2.12, 10.88, 4.56], [4.24, 0.66, 0.94], "cream"),
            cube([-1.48, 10.82, 4.48], [0.34, 0.82, 1.10], "leather"),
            cube([1.14, 10.82, 4.48], [0.34, 0.82, 1.10], "leather"),
            # Two vertical retention straps and real buckles on the visible back face.
            cube([-1.72, 7.18, 5.76], [0.34, 2.72, 0.16], "leather"),
            cube([1.38, 7.18, 5.76], [0.34, 2.72, 0.16], "leather"),
            cube([-1.82, 8.10, 5.86], [0.54, 0.54, 0.12], "brass"),
            cube([1.28, 8.10, 5.86], [0.54, 0.54, 0.12], "brass"),
            # Small copper lightning mark: geometry, not body texture replacement.
            cube([-0.20, 8.72, 5.88], [0.40, 1.18, 0.12], "copper", pivot=[0, 9.31, 5.94], rotation=[0, 0, -18]),
            cube([0.02, 8.18, 5.89], [0.36, 0.86, 0.12], "brass", pivot=[0.20, 8.61, 5.95], rotation=[0, 0, 28]),
        ],
    }


def tail_clamp_bone() -> dict:
    """Compact grounding clamp that reads as fitted hardware on the official flat tail plane."""
    return {
        "name": "ouros_storm_tail_clamp",
        "parent": "tail2",
        "pivot": [0, 9.75, 8.9],
        "cubes": [
            # Broad but shallow collar across a small section of tail2.
            cube([-0.42, 9.32, 8.00], [0.84, 0.56, 2.18], "copper"),
            cube([-0.52, 9.22, 8.18], [1.04, 0.18, 1.82], "charcoal"),
            cube([-0.52, 9.90, 8.18], [1.04, 0.18, 1.82], "charcoal"),
            # Central grounding hub and glass indicator.
            cube([-0.58, 9.42, 8.66], [1.16, 0.38, 0.74], "brass"),
            cube([-0.64, 9.50, 8.82], [1.28, 0.22, 0.42], "glass"),
            # Two tiny keepers make it read as a clamp rather than a painted stripe.
            cube([-0.48, 9.34, 7.88], [0.96, 0.50, 0.20], "leather"),
            cube([-0.48, 9.34, 10.10], [0.96, 0.50, 0.20], "leather"),
        ],
    }


def build(source: Path, identifier: str) -> dict:
    data = json.loads(source.read_text(encoding="utf-8"))
    geometry = data["minecraft:geometry"][0]
    geometry["description"]["identifier"] = identifier
    geometry["bones"].extend(
        [goggles_bone(), harness_bone(), pack_bone(), tail_clamp_bone()]
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
