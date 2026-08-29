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
    """Layered storm goggles with readable hardware at front and three-quarter angles."""
    cubes = [
        cube([-3.72, 14.70, -4.22], [2.12, 1.52, 0.10], "glass"),
        cube([1.60, 14.70, -4.22], [2.12, 1.52, 0.10], "glass"),
        cube([-3.84, 16.18, -4.26], [2.36, 0.20, 0.17], "copper"),
        cube([1.48, 16.18, -4.26], [2.36, 0.20, 0.17], "copper"),
        cube([-1.62, 15.34, -4.28], [3.24, 0.22, 0.17], "copper"),
        cube([-0.44, 15.04, -4.30], [0.88, 0.20, 0.18], "brass"),
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
            cube([-4.44, 15.30, -3.62], [0.72, 0.24, 1.28], "leather"),
            cube([3.72, 15.30, -3.62], [0.72, 0.24, 1.28], "leather"),
            cube([-4.46, 15.30, -2.48], [0.26, 0.24, 1.08], "navy"),
            cube([4.20, 15.30, -2.48], [0.26, 0.24, 1.08], "navy"),
            cube([-3.96, 16.05, -4.34], [0.28, 0.28, 0.14], "brass"),
            cube([-1.62, 16.05, -4.34], [0.28, 0.28, 0.14], "brass"),
            cube([1.34, 16.05, -4.34], [0.28, 0.28, 0.14], "brass"),
            cube([3.68, 16.05, -4.34], [0.28, 0.28, 0.14], "brass"),
            # Signature right-temple weather tab: tiny, readable, and still local to the head.
            cube([4.28, 15.64, -3.18], [0.28, 0.54, 0.72], "copper"),
            cube([4.34, 15.76, -3.30], [0.16, 0.30, 0.18], "glass"),
        ]
    )
    return {
        "name": "ouros_storm_goggles",
        "parent": "head_angle",
        "pivot": [0, 15.5, -4.0],
        "cubes": cubes,
    }


def harness_bone() -> dict:
    """Courier harness with strong chest readability and restrained utility hardware."""
    return {
        "name": "ouros_storm_harness",
        "parent": "torso2",
        "pivot": [0, 8.75, 0.5],
        "cubes": [
            cube(
                [-3.92, 8.72, -3.78],
                [7.84, 0.48, 0.26],
                "leather",
                pivot=[0, 8.96, -3.65],
                rotation=[0, 0, 27],
            ),
            cube(
                [-3.92, 8.72, -3.77],
                [7.84, 0.48, 0.26],
                "navy",
                pivot=[0, 8.96, -3.64],
                rotation=[0, 0, -27],
            ),
            cube([-4.18, 6.82, -3.76], [8.36, 0.46, 0.26], "leather"),
            cube([-4.44, 6.94, -3.42], [0.28, 1.90, 6.84], "navy"),
            cube([4.16, 6.94, -3.42], [0.28, 1.90, 6.84], "navy"),
            # Upper yoke stays above the arms in idle and gives the front a real equipment silhouette.
            cube([-2.34, 10.72, -3.80], [4.68, 0.34, 0.24], "leather"),
            cube([-1.30, 10.58, -3.92], [2.60, 0.18, 0.16], "copper"),
            # Main storm compass: larger than the prototype clasp so it remains readable in gameplay.
            cube([-0.78, 9.58, -3.98], [1.56, 1.34, 0.34], "brass"),
            cube([-0.48, 9.86, -4.08], [0.96, 0.78, 0.12], "glass"),
            cube([-0.18, 10.00, -4.12], [0.36, 0.50, 0.10], "copper", pivot=[0, 10.25, -4.07], rotation=[0, 0, -28]),
            # Lower keeper continues the V toward the belt without becoming a painted line.
            cube([-0.52, 7.48, -3.96], [1.04, 0.28, 0.24], "copper"),
            cube([-0.30, 7.76, -4.02], [0.60, 0.46, 0.10], "glass"),
            # Shoulder clips.
            cube([-3.62, 9.74, -3.96], [0.40, 0.40, 0.22], "brass"),
            cube([3.22, 9.74, -3.96], [0.40, 0.40, 0.22], "brass"),
            # Small utility pouches sit on the sides, visible from 3/4 but outside the arm swing path.
            cube([-4.36, 7.34, -2.36], [0.62, 1.10, 1.42], "canvas"),
            cube([3.74, 7.34, -2.36], [0.62, 1.10, 1.42], "canvas"),
            cube([-4.42, 8.24, -2.42], [0.74, 0.26, 1.54], "leather"),
            cube([3.68, 8.24, -2.42], [0.74, 0.26, 1.54], "leather"),
            cube([-4.48, 7.70, -2.54], [0.14, 0.40, 0.48], "brass"),
            cube([4.34, 7.70, -2.54], [0.14, 0.40, 0.48], "brass"),
        ],
    }


def pack_bone() -> dict:
    """Compact expedition pack with an asymmetric field-kit profile for three-quarter readability."""
    return {
        "name": "ouros_storm_pack",
        "parent": "torso2",
        "pivot": [0, 8.75, 4.5],
        "cubes": [
            cube([-2.40, 7.02, 4.36], [5.20, 3.78, 1.48], "canvas"),
            cube([-2.14, 6.48, 4.52], [4.68, 0.62, 1.18], "navy"),
            cube([-2.56, 10.06, 4.25], [5.52, 0.78, 1.70], "leather"),
            cube([-2.28, 9.82, 5.72], [4.96, 0.26, 0.16], "copper"),
            cube([-3.18, 7.52, 4.48], [0.84, 2.14, 1.20], "navy"),
            cube([2.74, 7.52, 4.48], [0.84, 2.14, 1.20], "navy"),
            cube([-3.26, 9.32, 4.42], [1.00, 0.38, 1.34], "leather"),
            cube([2.66, 9.32, 4.42], [1.00, 0.38, 1.34], "leather"),
            cube([-1.92, 10.88, 4.56], [4.24, 0.66, 0.94], "cream"),
            cube([-1.28, 10.82, 4.48], [0.34, 0.82, 1.10], "leather"),
            cube([1.34, 10.82, 4.48], [0.34, 0.82, 1.10], "leather"),
            cube([-1.52, 7.18, 5.76], [0.34, 2.72, 0.16], "leather"),
            cube([1.58, 7.18, 5.76], [0.34, 2.72, 0.16], "leather"),
            cube([-1.62, 8.10, 5.86], [0.54, 0.54, 0.12], "brass"),
            cube([1.48, 8.10, 5.86], [0.54, 0.54, 0.12], "brass"),
            cube([0.00, 8.72, 5.88], [0.40, 1.18, 0.12], "copper", pivot=[0.20, 9.31, 5.94], rotation=[0, 0, -18]),
            cube([0.22, 8.18, 5.89], [0.36, 0.86, 0.12], "brass", pivot=[0.40, 8.61, 5.95], rotation=[0, 0, 28]),
            # Right-side storm vial and copper cap create a recognizable asymmetry from the hero 3/4 view.
            cube([3.42, 7.74, 4.70], [0.42, 1.38, 0.74], "glass"),
            cube([3.34, 9.10, 4.62], [0.58, 0.28, 0.90], "copper"),
            cube([3.34, 7.54, 4.62], [0.58, 0.24, 0.90], "brass"),
            # Left-side folded route case balances the profile without making the pack wider than the body.
            cube([-3.54, 7.70, 4.62], [0.40, 1.42, 0.82], "cream"),
            cube([-3.62, 8.98, 4.54], [0.56, 0.28, 0.98], "leather"),
        ],
    }


def tail_clamp_bone() -> dict:
    """Grounding clamp with visible rails and indicator, still fitted to the official tail plane."""
    return {
        "name": "ouros_storm_tail_clamp",
        "parent": "tail2",
        "pivot": [0, 9.75, 8.9],
        "cubes": [
            cube([-0.42, 9.32, 8.00], [0.84, 0.56, 2.18], "copper"),
            cube([-0.54, 9.20, 8.18], [1.08, 0.18, 1.82], "charcoal"),
            cube([-0.54, 9.92, 8.18], [1.08, 0.18, 1.82], "charcoal"),
            cube([-0.58, 9.42, 8.66], [1.16, 0.38, 0.74], "brass"),
            cube([-0.64, 9.50, 8.82], [1.28, 0.22, 0.42], "glass"),
            cube([-0.48, 9.34, 7.88], [0.96, 0.50, 0.20], "leather"),
            cube([-0.48, 9.34, 10.10], [0.96, 0.50, 0.20], "leather"),
            # Two thin conductor rails extend the hardware language without repainting the tail.
            cube([-0.24, 9.48, 7.36], [0.18, 0.18, 0.58], "brass"),
            cube([0.06, 9.48, 10.18], [0.18, 0.18, 0.64], "brass"),
            cube([-0.18, 9.44, 8.28], [0.36, 0.22, 0.24], "navy"),
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
