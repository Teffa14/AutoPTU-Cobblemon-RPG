#!/usr/bin/env python3
"""Build Aura Sentinel around the exact official Cobblemon 1.7.3 Lucario model.

The source geometry is authoritative. Existing bones, cubes, pivots, locators,
UVs, hierarchy and animation-facing names are preserved byte-for-JSON value.
Only the geometry identifier and appended `ouros_*` cosmetic bones differ.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
from PIL import Image

PALETTE = {
    "obsidian": (23, 27, 39, 255),
    "steel": (84, 103, 124, 255),
    "silver": (176, 194, 207, 255),
    "gold": (220, 176, 68, 255),
    "aura": (73, 205, 255, 170),
    "royal": (37, 62, 112, 255),
    "cloth": (48, 43, 74, 255),
    "ivory": (222, 220, 202, 255),
}
FACES = ("north", "east", "south", "west", "up", "down")
PIXELS: dict[str, tuple[int, int]] = {}


def mark_uv_usage(geometry: dict) -> set[tuple[int, int]]:
    width = geometry["description"]["texture_width"]
    height = geometry["description"]["texture_height"]
    used: set[tuple[int, int]] = set()

    def mark(x, y, w, h):
        x0, x1 = sorted((int(x), int(x + w)))
        y0, y1 = sorted((int(y), int(y + h)))
        for yy in range(max(0, y0), min(height, y1)):
            for xx in range(max(0, x0), min(width, x1)):
                used.add((xx, yy))

    for bone in geometry["bones"]:
        for item in bone.get("cubes", []):
            dx, dy, dz = item.get("size", [0, 0, 0])
            uv = item.get("uv", [0, 0])
            if isinstance(uv, list):
                u, v = uv
                for rect in (
                    (u + dz, v, dx, dz),
                    (u + dz + dx, v, dx, dz),
                    (u, v + dz, dz, dy),
                    (u + dz, v + dz, dx, dy),
                    (u + dz + dx, v + dz, dz, dy),
                    (u + 2 * dz + dx, v + dz, dx, dy),
                ):
                    mark(*rect)
            else:
                for face in uv.values():
                    if isinstance(face, dict):
                        point = face.get("uv", [0, 0])
                        extent = face.get("uv_size", [1, 1])
                        mark(point[0], point[1], extent[0], extent[1])
    return used


def choose_palette_pixels(geometry: dict) -> dict[str, tuple[int, int]]:
    width = geometry["description"]["texture_width"]
    height = geometry["description"]["texture_height"]
    used = mark_uv_usage(geometry)
    free = [
        (x, y)
        for y in range(height - 1, -1, -1)
        for x in range(width)
        if (x, y) not in used
    ]
    if len(free) < len(PALETTE):
        raise RuntimeError("Official Lucario model has insufficient UV-free texels")
    return {name: free[index] for index, name in enumerate(PALETTE)}


def solid_uv(material: str) -> dict:
    x, y = PIXELS[material]
    return {face: {"uv": [x, y], "uv_size": [1, 1]} for face in FACES}


def cube(origin, size, material, **extra) -> dict:
    value = {"origin": origin, "size": size, "uv": solid_uv(material)}
    value.update({key: item for key, item in extra.items() if item is not None})
    return value


def crown() -> dict:
    return {
        "name": "ouros_aura_crown",
        "parent": "head_angle",
        "pivot": [0, 38.5, -3.2],
        "cubes": [
            cube([-3.70, 38.35, -4.76], [2.80, 0.34, 0.26], "steel", pivot=[-2.30, 38.52, -4.63], rotation=[0, 0, -8]),
            cube([0.90, 38.35, -4.76], [2.80, 0.34, 0.26], "steel", pivot=[2.30, 38.52, -4.63], rotation=[0, 0, 8]),
            cube([-1.00, 39.02, -4.84], [2.00, 0.34, 0.24], "gold"),
            cube([-0.38, 39.34, -4.88], [0.76, 1.34, 0.20], "aura", pivot=[0, 40.01, -4.78], rotation=[0, 0, 45]),
            cube([-4.12, 37.34, -3.74], [0.46, 2.56, 1.72], "obsidian"),
            cube([3.66, 37.34, -3.74], [0.46, 2.56, 1.72], "obsidian"),
            cube([-4.36, 39.50, -2.92], [1.72, 0.34, 1.08], "royal", pivot=[-3.50, 39.67, -2.38], rotation=[0, 0, -24]),
            cube([2.64, 39.50, -2.92], [1.72, 0.34, 1.08], "royal", pivot=[3.50, 39.67, -2.38], rotation=[0, 0, 24]),
            cube([-4.20, 39.88, -2.86], [1.30, 0.16, 0.88], "aura", pivot=[-3.55, 39.96, -2.42], rotation=[0, 0, -24]),
            cube([2.90, 39.88, -2.86], [1.30, 0.16, 0.88], "aura", pivot=[3.55, 39.96, -2.42], rotation=[0, 0, 24]),
        ],
    }


def left_pauldron() -> dict:
    return {
        "name": "ouros_aura_pauldron_left",
        "parent": "shoulder_left",
        "pivot": [3.2, 30.1, -0.4],
        "cubes": [
            cube([2.20, 30.10, -2.88], [5.20, 1.12, 5.00], "royal", pivot=[4.80, 30.66, -0.38], rotation=[0, 0, -10]),
            cube([2.06, 31.04, -2.62], [5.54, 0.28, 4.48], "silver", pivot=[4.83, 31.18, -0.38], rotation=[0, 0, -10]),
            cube([6.62, 30.34, -1.26], [2.48, 0.42, 1.84], "obsidian", pivot=[7.86, 30.55, -0.34], rotation=[0, 0, -34]),
            cube([6.86, 30.72, -1.12], [1.84, 0.18, 1.50], "aura", pivot=[7.78, 30.81, -0.37], rotation=[0, 0, -34]),
            cube([3.02, 29.74, -2.30], [1.12, 0.28, 3.88], "gold", pivot=[3.58, 29.88, -0.36], rotation=[0, 0, -10]),
        ],
    }


def right_pauldron() -> dict:
    return {
        "name": "ouros_aura_pauldron_right",
        "parent": "shoulder_right",
        "pivot": [-3.2, 30.1, -0.4],
        "cubes": [
            cube([-7.10, 30.18, -2.60], [4.70, 0.94, 4.58], "obsidian", pivot=[-4.75, 30.65, -0.31], rotation=[0, 0, 8]),
            cube([-7.20, 30.98, -2.36], [4.88, 0.24, 4.16], "steel", pivot=[-4.76, 31.10, -0.28], rotation=[0, 0, 8]),
            cube([-8.72, 30.34, -1.18], [2.18, 0.36, 1.64], "royal", pivot=[-7.63, 30.52, -0.36], rotation=[0, 0, 28]),
            cube([-8.42, 30.66, -1.06], [1.58, 0.16, 1.34], "gold", pivot=[-7.63, 30.74, -0.39], rotation=[0, 0, 28]),
        ],
    }


def chest_core() -> dict:
    return {
        "name": "ouros_aura_core",
        "parent": "torso3",
        "pivot": [0, 29.0, -3.0],
        "cubes": [
            # Open frame deliberately surrounds the official central chest spike.
            cube([-3.24, 28.14, -3.96], [1.14, 3.62, 0.42], "steel"),
            cube([2.10, 28.14, -3.96], [1.14, 3.62, 0.42], "steel"),
            cube([-2.18, 31.20, -4.00], [4.36, 0.58, 0.46], "gold"),
            cube([-2.18, 27.98, -4.00], [4.36, 0.48, 0.46], "gold"),
            cube([-3.48, 29.16, -4.10], [0.30, 1.52, 0.18], "aura"),
            cube([3.18, 29.16, -4.10], [0.30, 1.52, 0.18], "aura"),
            cube([-1.90, 31.78, -3.82], [3.80, 0.32, 0.34], "silver"),
        ],
    }


def backframe() -> dict:
    return {
        "name": "ouros_aura_backframe",
        "parent": "torso3",
        "pivot": [0, 29.2, 2.0],
        "cubes": [
            cube([-4.72, 25.60, 2.34], [0.66, 7.34, 0.72], "obsidian", pivot=[-4.39, 29.27, 2.70], rotation=[-5, 0, -8]),
            cube([4.06, 25.60, 2.34], [0.66, 7.34, 0.72], "obsidian", pivot=[4.39, 29.27, 2.70], rotation=[-5, 0, 8]),
            cube([-5.18, 31.76, 2.44], [2.18, 0.42, 1.12], "royal", pivot=[-4.09, 31.97, 3.00], rotation=[0, 0, -24]),
            cube([3.00, 31.76, 2.44], [2.18, 0.42, 1.12], "royal", pivot=[4.09, 31.97, 3.00], rotation=[0, 0, 24]),
            cube([-5.00, 32.18, 2.52], [1.72, 0.18, 0.88], "aura", pivot=[-4.14, 32.27, 2.96], rotation=[0, 0, -24]),
            cube([3.28, 32.18, 2.52], [1.72, 0.18, 0.88], "aura", pivot=[4.14, 32.27, 2.96], rotation=[0, 0, 24]),
            cube([-3.92, 24.84, 2.50], [7.84, 0.46, 0.64], "steel"),
            cube([-1.00, 24.72, 3.02], [2.00, 0.24, 0.20], "gold"),
        ],
    }


def left_bracer() -> dict:
    return {
        "name": "ouros_aura_bracer_left",
        "parent": "arm_left2",
        "pivot": [10.2, 29.7, -0.4],
        "cubes": [
            cube([9.00, 27.94, -2.30], [3.20, 3.48, 3.80], "obsidian"),
            cube([9.28, 28.18, -2.46], [2.64, 0.34, 4.12], "silver"),
            cube([11.78, 28.34, -1.02], [0.42, 2.22, 1.40], "gold"),
            cube([12.02, 28.76, -0.74], [0.18, 1.24, 0.84], "aura"),
        ],
    }


def right_bracer() -> dict:
    return {
        "name": "ouros_aura_bracer_right",
        "parent": "arm_right2",
        "pivot": [-10.2, 29.7, -0.4],
        "cubes": [
            cube([-12.20, 27.94, -2.30], [3.20, 3.48, 3.80], "cloth"),
            cube([-11.92, 28.18, -2.46], [2.64, 0.34, 4.12], "steel"),
            cube([-12.20, 28.34, -1.02], [0.42, 2.22, 1.40], "gold"),
            cube([-12.20, 28.76, -0.74], [0.18, 1.24, 0.84], "aura"),
        ],
    }


def waist_mantle() -> dict:
    return {
        "name": "ouros_aura_waist_mantle",
        "parent": "torso",
        "pivot": [0, 21.0, 1.0],
        "cubes": [
            cube([-5.18, 20.62, -3.76], [10.36, 0.66, 0.42], "cloth"),
            cube([-5.28, 21.18, -3.82], [10.56, 0.24, 0.46], "gold"),
            cube([-4.82, 14.80, 3.76], [4.16, 6.36, 0.52], "royal", pivot=[-2.74, 20.52, 4.02], rotation=[-8, 0, 9]),
            cube([0.66, 14.80, 3.76], [4.16, 6.36, 0.52], "cloth", pivot=[2.74, 20.52, 4.02], rotation=[-8, 0, -9]),
            cube([-4.54, 14.86, 4.22], [3.68, 0.28, 0.18], "silver", pivot=[-2.70, 15.00, 4.31], rotation=[-8, 0, 9]),
            cube([0.86, 14.86, 4.22], [3.68, 0.28, 0.18], "silver", pivot=[2.70, 15.00, 4.31], rotation=[-8, 0, -9]),
            cube([-0.86, 20.80, -4.02], [1.72, 1.46, 0.36], "ivory"),
            cube([-0.52, 21.08, -4.18], [1.04, 0.82, 0.16], "aura"),
        ],
    }


def build(source: Path, identifier: str) -> tuple[dict, int]:
    data = json.loads(source.read_text(encoding="utf-8"))
    geometry = data["minecraft:geometry"][0]
    original_count = len(geometry["bones"])
    geometry["description"]["identifier"] = identifier
    geometry["bones"].extend([
        crown(), left_pauldron(), right_pauldron(), chest_core(), backframe(),
        left_bracer(), right_bracer(), waist_mantle(),
    ])
    return data, original_count


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--official", type=Path, required=True)
    parser.add_argument("--model-out", type=Path, required=True)
    parser.add_argument("--overlay-out", type=Path, required=True)
    parser.add_argument("--metadata-out", type=Path, required=True)
    args = parser.parse_args()

    source_data = json.loads(args.official.read_text(encoding="utf-8"))
    geometry = source_data["minecraft:geometry"][0]
    global PIXELS
    PIXELS = choose_palette_pixels(geometry)

    built, original_count = build(args.official, "geometry.ouros_aura_sentinel_lucario")
    args.model_out.parent.mkdir(parents=True, exist_ok=True)
    args.model_out.write_text(json.dumps(built, separators=(",", ":")) + "\n", encoding="utf-8")

    width = geometry["description"]["texture_width"]
    height = geometry["description"]["texture_height"]
    overlay = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    for name, color in PALETTE.items():
        overlay.putpixel(PIXELS[name], color)
    args.overlay_out.parent.mkdir(parents=True, exist_ok=True)
    overlay.save(args.overlay_out, optimize=True)

    metadata = {
        "originalBoneCount": original_count,
        "derivedBoneCount": original_count + 8,
        "cosmeticBones": [
            "ouros_aura_crown", "ouros_aura_pauldron_left", "ouros_aura_pauldron_right",
            "ouros_aura_core", "ouros_aura_backframe", "ouros_aura_bracer_left",
            "ouros_aura_bracer_right", "ouros_aura_waist_mantle",
        ],
        "palettePixels": {name: list(point) for name, point in PIXELS.items()},
    }
    args.metadata_out.parent.mkdir(parents=True, exist_ok=True)
    args.metadata_out.write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
