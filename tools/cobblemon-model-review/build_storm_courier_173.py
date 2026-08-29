#!/usr/bin/env python3
"""Build Storm Courier from the exact Cobblemon 1.7.3 Pikachu geometry.

The source model is treated as immutable anatomy. This script changes only the
geometry identifier and appends four Ouros accessory bones. It also creates a
transparent accessory-only atlas using pixels that are unused by the source
model and transparent across the official Pikachu texture layers supplied to
this script.
"""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
from pathlib import Path

from PIL import Image

FACE_NAMES = ("north", "south", "east", "west", "up", "down")
PALETTE = {
    "charcoal": (35, 38, 43, 255),
    "leather": (105, 62, 35, 255),
    "copper": (181, 106, 50, 255),
    "storm_glass": (78, 158, 181, 255),
}


def default_uv_rects(uv, size):
    u, v = [float(x) for x in uv]
    dx, dy, dz = [float(x) for x in size]
    return {
        "west": (u, v + dz, dz, dy),
        "north": (u + dz, v + dz, dx, dy),
        "east": (u + dz + dx, v + dz, dz, dy),
        "south": (u + dz + dx + dz, v + dz, dx, dy),
        "up": (u + dz, v, dx, dz),
        "down": (u + dz + dx, v, dx, dz),
    }


def explicit_uv_rect(face):
    uv = face.get("uv", [0, 0])
    size = face.get("uv_size", [1, 1])
    return float(uv[0]), float(uv[1]), float(size[0]), float(size[1])


def mark_rect(used, rect, width, height):
    u, v, w, h = rect
    x0 = max(0, int(min(u, u + w)))
    x1 = min(width, int(max(u, u + w) + 0.999999))
    y0 = max(0, int(min(v, v + h)))
    y1 = min(height, int(max(v, v + h) + 0.999999))
    for y in range(y0, y1):
        for x in range(x0, x1):
            used.add((x, y))


def occupied_uv_pixels(model, width, height):
    used = set()
    geometry = model["minecraft:geometry"][0]
    for bone in geometry.get("bones", []):
        for cube in bone.get("cubes", []):
            uv_spec = cube.get("uv", [0, 0])
            if isinstance(uv_spec, dict):
                for face_name in FACE_NAMES:
                    if face_name in uv_spec:
                        mark_rect(used, explicit_uv_rect(uv_spec[face_name]), width, height)
            else:
                for rect in default_uv_rects(uv_spec, cube.get("size", [0, 0, 0])).values():
                    mark_rect(used, rect, width, height)
    return used


def load_rgba(path, expected):
    img = Image.open(path).convert("RGBA")
    if img.size != expected:
        raise ValueError(f"{path}: expected {expected}, got {img.size}")
    return img


def find_safe_pixels(model, texture_paths, width, height, count=4):
    used = occupied_uv_pixels(model, width, height)
    images = [load_rgba(path, (width, height)) for path in texture_paths]
    safe = []
    # Prefer the bottom-right area so accessory atlas pixels stay visually isolated.
    for y in range(height - 1, -1, -1):
        for x in range(width - 1, -1, -1):
            if (x, y) in used:
                continue
            if all(img.getpixel((x, y))[3] == 0 for img in images):
                safe.append((x, y))
                if len(safe) == count:
                    return safe
    raise RuntimeError("Could not find enough unused transparent UV pixels")


def solid_uv(pixel):
    x, y = pixel
    return {face: {"uv": [x, y], "uv_size": [1, 1]} for face in FACE_NAMES}


def cube(origin, size, pixel, *, inflate=None, pivot=None, rotation=None, mirror=None):
    result = {"origin": origin, "size": size, "uv": solid_uv(pixel)}
    if inflate is not None:
        result["inflate"] = inflate
    if pivot is not None:
        result["pivot"] = pivot
    if rotation is not None:
        result["rotation"] = rotation
    if mirror is not None:
        result["mirror"] = mirror
    return result


def accessory_bones(pixels):
    charcoal, leather, copper, storm_glass = pixels
    return [
        {
            "name": "ouros_courier_goggles",
            "parent": "head_angle",
            "pivot": [0, 16.4, -4.75],
            "cubes": [
                cube([-4.25, 15.35, -4.93], [3.1, 2.1, 0], storm_glass, inflate=0.03),
                cube([1.15, 15.35, -4.93], [3.1, 2.1, 0], storm_glass, inflate=0.03, mirror=True),
                cube([-1.2, 16.0, -4.91], [2.4, 0.65, 0], charcoal, inflate=0.025),
                cube([-4.7, 16.85, -3.55], [0.7, 0.7, 6.8], charcoal, inflate=0.02),
                cube([4.0, 16.85, -3.55], [0.7, 0.7, 6.8], charcoal, inflate=0.02),
                cube([-4.0, 16.85, 3.0], [8.0, 0.7, 0.7], charcoal, inflate=0.02),
            ],
        },
        {
            "name": "ouros_courier_harness",
            "parent": "torso2",
            "pivot": [0, 8.4, -3.72],
            "cubes": [
                cube([-5.0, 8.0, -3.82], [10.0, 0.8, 0.55], leather, inflate=0.025,
                     pivot=[0, 8.4, -3.72], rotation=[0, 0, -29]),
                cube([-5.0, 7.0, -3.82], [10.0, 0.8, 0.55], leather, inflate=0.025,
                     pivot=[0, 7.4, -3.72], rotation=[0, 0, 24]),
                cube([-4.7, 5.65, -4.02], [9.4, 0.7, 0.55], copper, inflate=0.02),
            ],
        },
        {
            "name": "ouros_courier_pack",
            "parent": "torso2",
            "pivot": [4.0, 8.2, 4.8],
            "cubes": [
                cube([2.25, 5.15, 4.45], [4.1, 6.0, 2.15], leather, inflate=0.03),
                cube([2.55, 10.35, 4.25], [3.5, 1.0, 2.3], copper, inflate=0.025),
                cube([3.65, 8.45, 6.48], [1.0, 1.0, 0.25], charcoal, inflate=0.015),
            ],
        },
        {
            "name": "ouros_courier_tail_clamp",
            "parent": "tail",
            "pivot": [0, 5.5, 8.0],
            "cubes": [
                cube([-0.55, 4.7, 6.5], [1.1, 2.4, 3.8], copper, inflate=0.035),
                cube([-0.62, 5.45, 7.1], [1.24, 0.7, 2.6], charcoal, inflate=0.025),
            ],
        },
    ]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-model", required=True)
    parser.add_argument("--base-texture", required=True)
    parser.add_argument("--shiny-texture", required=True)
    parser.add_argument("--emissive-texture", required=True)
    parser.add_argument("--emissive-shiny-texture", required=True)
    parser.add_argument("--output-model", required=True)
    parser.add_argument("--output-accessory", required=True)
    parser.add_argument("--output-metadata", required=True)
    args = parser.parse_args()

    source_path = Path(args.source_model)
    source = json.loads(source_path.read_text(encoding="utf-8"))
    model = copy.deepcopy(source)
    geometry = model["minecraft:geometry"][0]
    description = geometry["description"]
    width = int(description["texture_width"])
    height = int(description["texture_height"])
    if (width, height) != (128, 64):
        raise ValueError(f"Expected Cobblemon 1.7.3 Pikachu 128x64 atlas, got {(width, height)}")

    source_bones = geometry.get("bones", [])
    source_names = {bone.get("name") for bone in source_bones}
    required_173 = {
        "pikachu", "body", "torso", "torso2", "neck", "head", "head_ai", "head_angle",
        "muzzle", "mouth", "ear_left", "ear_right", "arm_left", "arm_right",
        "tail", "tail2", "tail3", "leg_left", "leg_right", "foot_left", "foot_right",
        "locator_item_face", "locator_item_hat", "locator_tail", "locator_tail_tip",
    }
    missing = sorted(required_173 - source_names)
    if missing:
        raise ValueError(f"Source is not the expected Cobblemon 1.7.3 Pikachu model; missing {missing}")
    if any(name and name.startswith("ouros_") for name in source_names):
        raise ValueError("Source model already contains Ouros geometry")

    texture_paths = [
        Path(args.base_texture), Path(args.shiny_texture),
        Path(args.emissive_texture), Path(args.emissive_shiny_texture),
    ]
    pixels = find_safe_pixels(source, texture_paths, width, height, 4)
    description["identifier"] = "geometry.ouros_storm_courier_pikachu"
    added = accessory_bones(pixels)
    geometry["bones"].extend(added)

    output_model = Path(args.output_model)
    output_model.parent.mkdir(parents=True, exist_ok=True)
    output_model.write_text(json.dumps(model, separators=(",", ":")) + "\n", encoding="utf-8")

    overlay = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    for (name, color), pixel in zip(PALETTE.items(), pixels):
        overlay.putpixel(pixel, color)
    output_accessory = Path(args.output_accessory)
    output_accessory.parent.mkdir(parents=True, exist_ok=True)
    overlay.save(output_accessory, optimize=True)

    # Anatomy must remain structurally identical and in the same order before accessories.
    built_bones = geometry["bones"][:-len(added)]
    if built_bones != source_bones:
        raise AssertionError("Source anatomy changed while building Storm Courier")

    metadata = {
        "format": "ouros.storm-courier-build.v2",
        "source": str(source_path),
        "sourceSha256": hashlib.sha256(source_path.read_bytes()).hexdigest(),
        "sourceAtlas": [width, height],
        "sourceBoneCount": len(source_bones),
        "outputBoneCount": len(geometry["bones"]),
        "addedBones": [bone["name"] for bone in added],
        "accessoryPixels": {name: list(pixel) for name, pixel in zip(PALETTE, pixels)},
        "anatomyPreserved": True,
    }
    output_metadata = Path(args.output_metadata)
    output_metadata.parent.mkdir(parents=True, exist_ok=True)
    output_metadata.write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
