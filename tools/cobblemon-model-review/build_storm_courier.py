#!/usr/bin/env python3
"""Build Storm Courier by appending epic cosmetic geometry to official Cobblemon Pikachu.

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
    """Storm visor with heavy frame, brow lightning and side retention hardware."""
    cubes = [
        cube([-3.82, 14.58, -4.28], [2.34, 1.78, 0.12], "glass"),
        cube([1.48, 14.58, -4.28], [2.34, 1.78, 0.12], "glass"),
        cube([-3.98, 16.24, -4.34], [2.66, 0.28, 0.22], "copper"),
        cube([1.32, 16.24, -4.34], [2.66, 0.28, 0.22], "copper"),
        cube([-1.62, 15.34, -4.36], [3.24, 0.28, 0.22], "copper"),
        cube([-0.50, 14.96, -4.38], [1.00, 0.28, 0.20], "brass"),
        # Lightning brow accents make the face read as a signature skin at distance.
        cube([-3.18, 16.52, -4.26], [1.18, 0.24, 0.18], "brass", pivot=[-2.59, 16.64, -4.17], rotation=[0, 0, -16]),
        cube([2.00, 16.52, -4.26], [1.18, 0.24, 0.18], "brass", pivot=[2.59, 16.64, -4.17], rotation=[0, 0, 16]),
    ]
    for x in (-3.98, 1.32):
        cubes.extend(
            [
                cube([x, 14.44, -4.33], [2.66, 0.20, 0.20], "charcoal"),
                cube([x, 14.44, -4.33], [0.20, 2.08, 0.20], "charcoal"),
                cube([x + 2.46, 14.44, -4.33], [0.20, 2.08, 0.20], "charcoal"),
            ]
        )
    cubes.extend(
        [
            cube([-4.72, 15.18, -3.72], [0.82, 0.34, 1.54], "leather"),
            cube([3.90, 15.18, -3.72], [0.82, 0.34, 1.54], "leather"),
            cube([-4.78, 15.18, -2.30], [0.30, 0.34, 1.42], "navy"),
            cube([4.48, 15.18, -2.30], [0.30, 0.34, 1.42], "navy"),
            cube([-4.00, 16.10, -4.42], [0.34, 0.34, 0.16], "brass"),
            cube([-1.56, 16.10, -4.42], [0.34, 0.34, 0.16], "brass"),
            cube([1.22, 16.10, -4.42], [0.34, 0.34, 0.16], "brass"),
            cube([3.66, 16.10, -4.42], [0.34, 0.34, 0.16], "brass"),
            # Right-temple weather lens is intentionally asymmetric.
            cube([4.56, 15.52, -3.28], [0.34, 0.76, 0.84], "copper"),
            cube([4.64, 15.68, -3.40], [0.18, 0.42, 0.20], "glass"),
        ]
    )
    return {
        "name": "ouros_storm_goggles",
        "parent": "head_angle",
        "pivot": [0, 15.5, -4.0],
        "cubes": cubes,
    }


def cowl_bone() -> dict:
    """Open-faced storm cowl that frames the original head and pushes silhouette outward."""
    return {
        "name": "ouros_storm_cowl",
        "parent": "head_angle",
        "pivot": [0, 17.0, 2.0],
        "cubes": [
            # Rear shell and side rails leave the original face and ears unobstructed.
            cube([-4.72, 14.20, 3.90], [9.44, 4.90, 0.44], "navy"),
            cube([-4.92, 14.72, 1.52], [0.42, 4.10, 2.48], "canvas"),
            cube([4.50, 14.72, 1.52], [0.42, 4.10, 2.48], "canvas"),
            cube([-4.92, 18.42, 1.40], [1.48, 0.38, 2.76], "copper"),
            cube([3.44, 18.42, 1.40], [1.48, 0.38, 2.76], "copper"),
            # Angular crown wings create a distinct hero silhouette between head and ears.
            cube([-5.56, 18.60, 1.34], [2.20, 0.40, 1.20], "navy", pivot=[-4.46, 18.80, 1.94], rotation=[0, 0, -24]),
            cube([3.36, 18.60, 1.34], [2.20, 0.40, 1.20], "navy", pivot=[4.46, 18.80, 1.94], rotation=[0, 0, 24]),
            cube([-5.42, 18.92, 1.42], [1.72, 0.20, 0.90], "brass", pivot=[-4.56, 19.02, 1.87], rotation=[0, 0, -24]),
            cube([3.70, 18.92, 1.42], [1.72, 0.20, 0.90], "brass", pivot=[4.56, 19.02, 1.87], rotation=[0, 0, 24]),
            # Lower jawline guards stay behind the muzzle plane.
            cube([-4.86, 13.62, 0.72], [0.38, 1.68, 2.82], "leather"),
            cube([4.48, 13.62, 0.72], [0.38, 1.68, 2.82], "leather"),
            cube([-4.98, 14.04, 0.58], [0.18, 0.72, 1.34], "brass"),
            cube([4.80, 14.04, 0.58], [0.18, 0.72, 1.34], "brass"),
        ],
    }


def mantle_bone() -> dict:
    """Layered shoulder mantle and split storm tabs for an aggressive 3/4 silhouette."""
    return {
        "name": "ouros_storm_mantle",
        "parent": "torso2",
        "pivot": [0, 10.5, 1.0],
        "cubes": [
            # Shoulder pauldrons extend past the body but remain clear of the original arm bones.
            cube([-6.10, 10.24, -2.46], [2.72, 1.26, 4.70], "navy", pivot=[-4.74, 10.87, -0.11], rotation=[0, 0, -8]),
            cube([3.38, 10.24, -2.46], [2.72, 1.26, 4.70], "navy", pivot=[4.74, 10.87, -0.11], rotation=[0, 0, 8]),
            cube([-6.20, 11.34, -2.26], [2.86, 0.30, 4.34], "copper", pivot=[-4.77, 11.49, -0.09], rotation=[0, 0, -8]),
            cube([3.34, 11.34, -2.26], [2.86, 0.30, 4.34], "copper", pivot=[4.77, 11.49, -0.09], rotation=[0, 0, 8]),
            cube([-5.82, 9.96, -2.10], [2.12, 0.30, 4.04], "brass", pivot=[-4.76, 10.11, -0.08], rotation=[0, 0, -8]),
            cube([3.70, 9.96, -2.10], [2.12, 0.30, 4.04], "brass", pivot=[4.76, 10.11, -0.08], rotation=[0, 0, 8]),
            # Rear split mantle panels give motion a stronger profile without replacing the torso.
            cube([-4.18, 7.10, 4.44], [3.54, 4.30, 0.46], "canvas", pivot=[-2.40, 10.80, 4.67], rotation=[-8, 0, 10]),
            cube([0.64, 7.10, 4.44], [3.54, 4.30, 0.46], "navy", pivot=[2.40, 10.80, 4.67], rotation=[-8, 0, -10]),
            cube([-3.98, 7.14, 4.86], [3.24, 0.22, 0.20], "copper", pivot=[-2.36, 7.25, 4.96], rotation=[-8, 0, 10]),
            cube([0.74, 7.14, 4.86], [3.24, 0.22, 0.20], "copper", pivot=[2.36, 7.25, 4.96], rotation=[-8, 0, -10]),
        ],
    }


def harness_bone() -> dict:
    """Heavy courier harness with a large storm-core chest assembly."""
    return {
        "name": "ouros_storm_harness",
        "parent": "torso2",
        "pivot": [0, 8.75, 0.5],
        "cubes": [
            cube([-4.08, 8.62, -3.86], [8.16, 0.58, 0.30], "leather", pivot=[0, 8.91, -3.71], rotation=[0, 0, 29]),
            cube([-4.08, 8.62, -3.84], [8.16, 0.58, 0.30], "navy", pivot=[0, 8.91, -3.69], rotation=[0, 0, -29]),
            cube([-4.34, 6.72, -3.84], [8.68, 0.54, 0.30], "leather"),
            cube([-4.56, 6.92, -3.46], [0.34, 2.28, 6.92], "navy"),
            cube([4.22, 6.92, -3.46], [0.34, 2.28, 6.92], "navy"),
            cube([-2.84, 10.70, -3.90], [5.68, 0.42, 0.28], "leather"),
            cube([-1.78, 10.54, -4.02], [3.56, 0.22, 0.20], "copper"),
            # Main storm core is intentionally much larger than the previous compass clasp.
            cube([-1.12, 9.28, -4.10], [2.24, 1.82, 0.44], "brass"),
            cube([-0.78, 9.62, -4.22], [1.56, 1.14, 0.14], "glass"),
            cube([-0.22, 9.78, -4.26], [0.44, 0.80, 0.12], "copper", pivot=[0, 10.18, -4.20], rotation=[0, 0, -32]),
            cube([-0.60, 8.64, -4.08], [1.20, 0.40, 0.28], "copper"),
            cube([-0.34, 8.94, -4.16], [0.68, 0.54, 0.12], "glass"),
            # Shoulder locks visually connect the harness to the mantle.
            cube([-4.04, 9.80, -4.00], [0.54, 0.54, 0.26], "brass"),
            cube([3.50, 9.80, -4.00], [0.54, 0.54, 0.26], "brass"),
            cube([-4.56, 7.26, -2.48], [0.82, 1.36, 1.64], "canvas"),
            cube([3.74, 7.26, -2.48], [0.82, 1.36, 1.64], "canvas"),
            cube([-4.66, 8.38, -2.56], [1.02, 0.32, 1.82], "leather"),
            cube([3.64, 8.38, -2.56], [1.02, 0.32, 1.82], "leather"),
            cube([-4.72, 7.74, -2.68], [0.18, 0.50, 0.56], "brass"),
            cube([4.54, 7.74, -2.68], [0.18, 0.50, 0.56], "brass"),
        ],
    }


def pack_bone() -> dict:
    """Large but body-bounded expedition pack with asymmetric storm equipment."""
    return {
        "name": "ouros_storm_pack",
        "parent": "torso2",
        "pivot": [0, 8.75, 4.5],
        "cubes": [
            cube([-2.86, 6.78, 4.30], [6.12, 4.42, 1.84], "canvas"),
            cube([-2.58, 6.18, 4.48], [5.56, 0.72, 1.44], "navy"),
            cube([-3.00, 10.48, 4.16], [6.40, 0.96, 2.06], "leather"),
            cube([-2.72, 10.18, 6.02], [5.84, 0.30, 0.20], "copper"),
            cube([-3.76, 7.34, 4.40], [1.02, 2.56, 1.56], "navy"),
            cube([3.20, 7.34, 4.40], [1.02, 2.56, 1.56], "navy"),
            cube([-3.88, 9.52, 4.32], [1.26, 0.46, 1.76], "leather"),
            cube([3.08, 9.52, 4.32], [1.26, 0.46, 1.76], "leather"),
            # Rolled stormcloth crowns the pack and reads clearly from the front 3/4.
            cube([-2.34, 11.44, 4.48], [5.08, 0.86, 1.24], "cream"),
            cube([-1.66, 11.34, 4.36], [0.42, 1.06, 1.48], "leather"),
            cube([1.44, 11.34, 4.36], [0.42, 1.06, 1.48], "leather"),
            cube([-1.82, 6.96, 6.06], [0.42, 3.20, 0.20], "leather"),
            cube([1.78, 6.96, 6.06], [0.42, 3.20, 0.20], "leather"),
            cube([-1.96, 8.06, 6.18], [0.66, 0.66, 0.14], "brass"),
            cube([1.64, 8.06, 6.18], [0.66, 0.66, 0.14], "brass"),
            # Large lightning sigil on rear plate.
            cube([-0.10, 8.78, 6.20], [0.48, 1.54, 0.14], "copper", pivot=[0.14, 9.55, 6.27], rotation=[0, 0, -18]),
            cube([0.18, 8.08, 6.22], [0.44, 1.12, 0.14], "brass", pivot=[0.40, 8.64, 6.29], rotation=[0, 0, 30]),
            # Right-side storm vial and left route case exaggerate asymmetry.
            cube([3.96, 7.48, 4.64], [0.54, 1.76, 0.94], "glass"),
            cube([3.84, 9.22, 4.54], [0.78, 0.34, 1.14], "copper"),
            cube([3.84, 7.18, 4.54], [0.78, 0.30, 1.14], "brass"),
            cube([-4.34, 7.36, 4.56], [0.52, 1.90, 1.06], "cream"),
            cube([-4.44, 9.06, 4.46], [0.72, 0.36, 1.26], "leather"),
        ],
    }


def coils_bone() -> dict:
    """Twin storm-field pylons rising behind the shoulders for an unmistakable epic silhouette."""
    return {
        "name": "ouros_storm_coils",
        "parent": "torso2",
        "pivot": [0, 10.0, 4.8],
        "cubes": [
            # Left pylon.
            cube([-3.72, 9.82, 4.84], [0.58, 4.28, 0.58], "charcoal", pivot=[-3.43, 11.96, 5.13], rotation=[0, 0, -9]),
            cube([-3.82, 10.18, 4.72], [0.78, 0.34, 0.82], "copper", pivot=[-3.43, 10.35, 5.13], rotation=[0, 0, -9]),
            cube([-3.82, 11.24, 4.72], [0.78, 0.34, 0.82], "brass", pivot=[-3.43, 11.41, 5.13], rotation=[0, 0, -9]),
            cube([-3.82, 12.30, 4.72], [0.78, 0.34, 0.82], "copper", pivot=[-3.43, 12.47, 5.13], rotation=[0, 0, -9]),
            cube([-3.74, 13.26, 4.76], [0.62, 0.78, 0.74], "glass", pivot=[-3.43, 13.65, 5.13], rotation=[0, 0, -9]),
            cube([-4.20, 13.92, 4.66], [1.54, 0.30, 0.94], "brass", pivot=[-3.43, 14.07, 5.13], rotation=[0, 0, -9]),
            # Right pylon.
            cube([3.14, 9.82, 4.84], [0.58, 4.28, 0.58], "charcoal", pivot=[3.43, 11.96, 5.13], rotation=[0, 0, 9]),
            cube([3.04, 10.18, 4.72], [0.78, 0.34, 0.82], "copper", pivot=[3.43, 10.35, 5.13], rotation=[0, 0, 9]),
            cube([3.04, 11.24, 4.72], [0.78, 0.34, 0.82], "brass", pivot=[3.43, 11.41, 5.13], rotation=[0, 0, 9]),
            cube([3.04, 12.30, 4.72], [0.78, 0.34, 0.82], "copper", pivot=[3.43, 12.47, 5.13], rotation=[0, 0, 9]),
            cube([3.12, 13.26, 4.76], [0.62, 0.78, 0.74], "glass", pivot=[3.43, 13.65, 5.13], rotation=[0, 0, 9]),
            cube([2.66, 13.92, 4.66], [1.54, 0.30, 0.94], "brass", pivot=[3.43, 14.07, 5.13], rotation=[0, 0, 9]),
            # Low bridge grounds both pylons into the pack silhouette.
            cube([-3.36, 9.72, 4.72], [6.72, 0.34, 0.72], "navy"),
            cube([-1.14, 9.66, 5.36], [2.28, 0.46, 0.18], "glass"),
        ],
    }


def tail_clamp_bone() -> dict:
    """Heavy grounding clamp fitted to the official tail plane."""
    return {
        "name": "ouros_storm_tail_clamp",
        "parent": "tail2",
        "pivot": [0, 9.75, 8.9],
        "cubes": [
            cube([-0.52, 9.18, 7.82], [1.04, 0.76, 2.62], "copper"),
            cube([-0.66, 9.04, 8.00], [1.32, 0.22, 2.24], "charcoal"),
            cube([-0.66, 9.98, 8.00], [1.32, 0.22, 2.24], "charcoal"),
            cube([-0.72, 9.36, 8.56], [1.44, 0.46, 0.90], "brass"),
            cube([-0.78, 9.46, 8.76], [1.56, 0.26, 0.50], "glass"),
            cube([-0.58, 9.22, 7.66], [1.16, 0.62, 0.24], "leather"),
            cube([-0.58, 9.22, 10.40], [1.16, 0.62, 0.24], "leather"),
            cube([-0.30, 9.48, 7.12], [0.22, 0.22, 0.64], "brass"),
            cube([0.08, 9.48, 10.52], [0.22, 0.22, 0.78], "brass"),
            cube([-0.24, 9.40, 8.18], [0.48, 0.30, 0.32], "navy"),
        ],
    }


def tail_vanes_bone() -> dict:
    """Segmented conductor fins along the tail edge without replacing the official tail."""
    return {
        "name": "ouros_storm_tail_vanes",
        "parent": "tail2",
        "pivot": [0, 11.0, 10.0],
        "cubes": [
            cube([-0.42, 10.18, 10.70], [0.84, 0.28, 1.52], "navy"),
            cube([-0.34, 10.54, 11.64], [0.68, 0.24, 1.44], "copper"),
            cube([-0.28, 10.88, 12.52], [0.56, 0.22, 1.26], "brass"),
            cube([-0.22, 11.18, 13.28], [0.44, 0.20, 1.04], "glass"),
            # Small side fins thicken the otherwise flat plane just enough to read in 3/4.
            cube([-0.74, 10.44, 11.10], [0.30, 0.64, 0.92], "charcoal"),
            cube([0.44, 10.44, 11.10], [0.30, 0.64, 0.92], "charcoal"),
            cube([-0.66, 11.02, 12.18], [0.24, 0.52, 0.78], "brass"),
            cube([0.42, 11.02, 12.18], [0.24, 0.52, 0.78], "brass"),
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
