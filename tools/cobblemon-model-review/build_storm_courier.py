#!/usr/bin/env python3
"""Build Storm Courier only by appending cosmetic bones to official Cobblemon Pikachu.

Inputs must be the exact male/female geometry extracted from the pinned official
Cobblemon 1.7.3 Fabric JAR. The script changes only the geometry identifier and
appends Ouros accessory bones. Existing bones are never rewritten.
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


def visor_bone() -> dict:
    cubes = [
        cube([-4.0, 14.45, -4.23], [2.7, 2.35, 0.12], "glass"),
        cube([1.3, 14.45, -4.23], [2.7, 2.35, 0.12], "glass"),
    ]
    for x in (-4.12, 1.18):
        cubes.extend(
            [
                cube([x, 14.30, -4.28], [2.94, 0.24, 0.18], "charcoal"),
                cube([x, 16.76, -4.28], [2.94, 0.24, 0.18], "charcoal"),
                cube([x, 14.30, -4.28], [0.24, 2.70, 0.18], "charcoal"),
                cube([x + 2.70, 14.30, -4.28], [0.24, 2.70, 0.18], "charcoal"),
            ]
        )
    cubes.extend(
        [
            cube([-1.30, 15.43, -4.29], [2.60, 0.34, 0.20], "copper"),
            cube(
                [-4.55, 17.15, -3.25],
                [1.15, 0.38, 1.45],
                "leather",
                rotation=[0, -12, 0],
                pivot=[-4.0, 17.3, -2.5],
            ),
            cube(
                [3.40, 17.15, -3.25],
                [1.15, 0.38, 1.45],
                "leather",
                rotation=[0, 12, 0],
                pivot=[4.0, 17.3, -2.5],
            ),
        ]
    )
    return {
        "name": "ouros_storm_visor",
        "parent": "head_angle",
        "pivot": [0, 15.7, -4.05],
        "cubes": cubes,
    }


def harness_bone() -> dict:
    return {
        "name": "ouros_storm_harness",
        "parent": "torso2",
        "pivot": [0, 8.75, 0.5],
        "cubes": [
            cube(
                [-4.25, 8.55, -3.78],
                [8.5, 0.52, 0.26],
                "leather",
                pivot=[0, 8.8, -3.65],
                rotation=[0, 0, 28],
            ),
            cube(
                [-4.25, 8.55, -3.76],
                [8.5, 0.52, 0.26],
                "leather",
                pivot=[0, 8.8, -3.65],
                rotation=[0, 0, -28],
            ),
            cube([-4.70, 6.35, -3.80], [9.4, 0.58, 0.28], "navy"),
            cube([-0.78, 7.55, -3.96], [1.56, 1.56, 0.34], "brass"),
            cube([-0.42, 7.91, -4.10], [0.84, 0.84, 0.12], "glass"),
            cube(
                [-4.35, 10.80, -3.55],
                [1.10, 0.58, 0.45],
                "copper",
                rotation=[0, 0, -18],
                pivot=[-3.8, 11.0, -3.3],
            ),
            cube(
                [3.25, 10.80, -3.55],
                [1.10, 0.58, 0.45],
                "copper",
                rotation=[0, 0, 18],
                pivot=[3.8, 11.0, -3.3],
            ),
        ],
    }


def pack_bone() -> dict:
    return {
        "name": "ouros_storm_pack",
        "parent": "torso2",
        "pivot": [0, 8.75, 4.5],
        "cubes": [
            cube([-3.25, 6.75, 4.40], [6.50, 5.25, 2.15], "canvas"),
            cube([-3.50, 10.85, 4.18], [7.00, 1.15, 2.55], "leather"),
            cube([-3.05, 7.10, 6.48], [6.10, 0.48, 0.22], "brass"),
            cube([-4.15, 7.45, 4.50], [0.95, 2.75, 1.95], "navy"),
            cube([3.20, 7.45, 4.50], [0.95, 2.75, 1.95], "navy"),
            cube(
                [-1.70, 9.15, 6.53],
                [2.15, 0.36, 0.16],
                "copper",
                pivot=[-0.65, 9.33, 6.60],
                rotation=[0, 0, -28],
            ),
            cube(
                [-0.35, 8.20, 6.54],
                [2.15, 0.36, 0.16],
                "copper",
                pivot=[0.70, 8.38, 6.60],
                rotation=[0, 0, 28],
            ),
        ],
    }


def antenna_bone() -> dict:
    return {
        "name": "ouros_storm_antenna",
        "parent": "torso2",
        "pivot": [3.15, 11.4, 5.1],
        "cubes": [
            cube([3.00, 10.80, 4.78], [0.38, 3.65, 0.38], "copper"),
            cube([2.62, 14.20, 4.40], [1.15, 1.15, 1.15], "glass"),
            cube([2.82, 14.40, 4.60], [0.75, 0.75, 0.75], "brass"),
        ],
    }


def tail_clamp_bone() -> dict:
    return {
        "name": "ouros_storm_tail_clamp",
        "parent": "tail2",
        "pivot": [0, 9.8, 8.8],
        "cubes": [
            cube([-0.44, 9.15, 7.25], [0.88, 1.18, 3.25], "copper"),
            cube([-0.66, 9.38, 8.20], [1.32, 0.72, 1.10], "brass"),
            cube([-0.77, 9.49, 8.31], [1.54, 0.50, 0.88], "glass"),
        ],
    }


def build(source: Path, identifier: str) -> dict:
    data = json.loads(source.read_text(encoding="utf-8"))
    geometry = data["minecraft:geometry"][0]
    geometry["description"]["identifier"] = identifier
    geometry["bones"].extend(
        [visor_bone(), harness_bone(), pack_bone(), antenna_bone(), tail_clamp_bone()]
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
