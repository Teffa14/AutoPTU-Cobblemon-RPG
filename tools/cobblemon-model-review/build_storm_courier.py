#!/usr/bin/env python3
"""Build Storm Courier by appending small cosmetic bones to official Cobblemon Pikachu.

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
    """Compact goggles that sit on the existing face without replacing eyes/muzzle."""
    cubes = [
        cube([-3.72, 14.75, -4.20], [2.05, 1.45, 0.10], "glass"),
        cube([1.67, 14.75, -4.20], [2.05, 1.45, 0.10], "glass"),
    ]
    for x in (-3.82, 1.57):
        cubes.extend(
            [
                cube([x, 14.65, -4.24], [2.25, 0.16, 0.14], "charcoal"),
                cube([x, 16.14, -4.24], [2.25, 0.16, 0.14], "charcoal"),
                cube([x, 14.65, -4.24], [0.16, 1.65, 0.14], "charcoal"),
                cube([x + 2.09, 14.65, -4.24], [0.16, 1.65, 0.14], "charcoal"),
            ]
        )
    cubes.extend(
        [
            cube([-1.60, 15.30, -4.25], [3.20, 0.22, 0.15], "copper"),
            cube([-4.42, 15.30, -3.72], [0.70, 0.22, 1.18], "leather"),
            cube([3.72, 15.30, -3.72], [0.70, 0.22, 1.18], "leather"),
        ]
    )
    return {
        "name": "ouros_storm_goggles",
        "parent": "head_angle",
        "pivot": [0, 15.5, -4.0],
        "cubes": cubes,
    }


def harness_bone() -> dict:
    """Thin front harness. It follows torso2 and leaves the body silhouette exposed."""
    return {
        "name": "ouros_storm_harness",
        "parent": "torso2",
        "pivot": [0, 8.75, 0.5],
        "cubes": [
            cube(
                [-3.85, 8.70, -3.73],
                [7.70, 0.34, 0.16],
                "leather",
                pivot=[0, 8.87, -3.65],
                rotation=[0, 0, 27],
            ),
            cube([-4.28, 6.65, -3.72], [8.56, 0.34, 0.16], "navy"),
            cube([-0.50, 7.72, -3.82], [1.00, 1.00, 0.20], "brass"),
            cube([-0.27, 7.95, -3.94], [0.54, 0.54, 0.08], "glass"),
        ],
    }


def pack_bone() -> dict:
    """Small courier satchel that hugs the back rather than replacing the torso silhouette."""
    return {
        "name": "ouros_storm_pack",
        "parent": "torso2",
        "pivot": [0, 8.75, 4.5],
        "cubes": [
            cube([-2.35, 7.30, 4.38], [4.70, 3.55, 1.25], "canvas"),
            cube([-2.55, 9.95, 4.28], [5.10, 0.70, 1.48], "leather"),
            cube([-1.70, 7.62, 5.58], [3.40, 0.28, 0.12], "brass"),
            cube([-2.57, 7.65, 4.45], [0.38, 1.72, 1.10], "navy"),
            cube([2.19, 7.65, 4.45], [0.38, 1.72, 1.10], "navy"),
        ],
    }


def tail_clamp_bone() -> dict:
    """Minimal clamp on tail2; it must read as hardware on the tail, not a new tail."""
    return {
        "name": "ouros_storm_tail_clamp",
        "parent": "tail2",
        "pivot": [0, 9.8, 8.8],
        "cubes": [
            cube([-0.26, 9.35, 8.00], [0.52, 0.72, 1.45], "copper"),
            cube([-0.36, 9.47, 8.36], [0.72, 0.44, 0.72], "brass"),
            cube([-0.41, 9.53, 8.42], [0.82, 0.30, 0.60], "glass"),
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
