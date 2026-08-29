#!/usr/bin/env python3
"""Build Rift Warden around the exact official Cobblemon 1.7.3 Gengar model.

All official Gengar bones remain JSON-equivalent and in original order. The only
model changes are the geometry identifier and appended `ouros_*` cosmetic bones.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
from PIL import Image

PALETTE = {
    "void": (24, 18, 40, 255),
    "obsidian": (42, 35, 61, 255),
    "violet": (111, 68, 172, 255),
    "magenta": (214, 70, 197, 255),
    "rift": (172, 105, 255, 175),
    "silver": (176, 184, 205, 255),
    "bone": (218, 210, 190, 255),
    "ember": (255, 120, 92, 220),
}
FACES = ("north", "east", "south", "west", "up", "down")
PIXELS: dict[str, tuple[int, int]] = {}


def mark_uv_usage(geometry: dict) -> set[tuple[int, int]]:
    width = int(geometry["description"]["texture_width"])
    height = int(geometry["description"]["texture_height"])
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
    width = int(geometry["description"]["texture_width"])
    height = int(geometry["description"]["texture_height"])
    used = mark_uv_usage(geometry)
    free = [(x, y) for y in range(height - 1, -1, -1) for x in range(width) if (x, y) not in used]
    if len(free) < len(PALETTE):
        raise RuntimeError("Official Gengar model has insufficient UV-free texels")
    return {name: free[index] for index, name in enumerate(PALETTE)}


def solid_uv(material: str) -> dict:
    x, y = PIXELS[material]
    return {face: {"uv": [x, y], "uv_size": [1, 1]} for face in FACES}


def cube(origin, size, material, **extra) -> dict:
    value = {"origin": origin, "size": size, "uv": solid_uv(material)}
    value.update({key: item for key, item in extra.items() if item is not None})
    return value


def rift_halo() -> dict:
    """Broken planar halo behind the head; large signature silhouette without covering face."""
    cubes = [
        cube([-12.8, 31.2, 10.7], [6.4, 0.62, 0.74], "void", pivot=[-9.6, 31.51, 11.07], rotation=[0, 0, -32]),
        cube([-15.2, 35.3, 10.7], [6.2, 0.62, 0.74], "violet", pivot=[-12.1, 35.61, 11.07], rotation=[0, 0, -58]),
        cube([-11.8, 39.1, 10.7], [5.8, 0.62, 0.74], "obsidian", pivot=[-8.9, 39.41, 11.07], rotation=[0, 0, -110]),
        cube([6.4, 31.2, 10.7], [6.4, 0.62, 0.74], "void", pivot=[9.6, 31.51, 11.07], rotation=[0, 0, 32]),
        cube([9.0, 35.3, 10.7], [6.2, 0.62, 0.74], "violet", pivot=[12.1, 35.61, 11.07], rotation=[0, 0, 58]),
        cube([6.0, 39.1, 10.7], [5.8, 0.62, 0.74], "obsidian", pivot=[8.9, 39.41, 11.07], rotation=[0, 0, 110]),
        cube([-12.2, 31.9, 10.52], [5.2, 0.18, 0.22], "rift", pivot=[-9.6, 31.99, 10.63], rotation=[0, 0, -32]),
        cube([-14.5, 35.9, 10.52], [4.8, 0.18, 0.22], "magenta", pivot=[-12.1, 35.99, 10.63], rotation=[0, 0, -58]),
        cube([7.0, 31.9, 10.52], [5.2, 0.18, 0.22], "rift", pivot=[9.6, 31.99, 10.63], rotation=[0, 0, 32]),
        cube([9.7, 35.9, 10.52], [4.8, 0.18, 0.22], "magenta", pivot=[12.1, 35.99, 10.63], rotation=[0, 0, 58]),
        # Deliberate missing top-center segment keeps the crown broken rather than a generic ring.
        cube([-1.05, 41.0, 10.54], [2.10, 1.70, 0.26], "rift", pivot=[0, 41.85, 10.67], rotation=[0, 0, 45]),
    ]
    return {"name": "ouros_rift_halo", "parent": "torso", "pivot": [0, 31.0, 10.5], "cubes": cubes}


def rift_collar() -> dict:
    """Rear/side collar frames the ears while leaving the official face unobstructed."""
    return {
        "name": "ouros_rift_collar",
        "parent": "torso",
        "pivot": [0, 27.0, 6.0],
        "cubes": [
            cube([-12.0, 26.0, 6.3], [3.0, 7.2, 2.8], "void", pivot=[-10.5, 29.6, 7.7], rotation=[-4, 0, -18]),
            cube([9.0, 26.0, 6.3], [3.0, 7.2, 2.8], "obsidian", pivot=[10.5, 29.6, 7.7], rotation=[-4, 0, 18]),
            cube([-11.5, 32.4, 6.5], [4.0, 0.42, 2.4], "silver", pivot=[-9.5, 32.61, 7.7], rotation=[0, 0, -18]),
            cube([7.5, 32.4, 6.5], [4.0, 0.42, 2.4], "silver", pivot=[9.5, 32.61, 7.7], rotation=[0, 0, 18]),
            cube([-12.25, 28.4, 6.08], [0.26, 2.9, 0.30], "rift"),
            cube([11.99, 28.4, 6.08], [0.26, 2.9, 0.30], "magenta"),
        ],
    }


def shroud_left() -> dict:
    return {
        "name": "ouros_rift_shroud_left",
        "parent": "arm_left",
        "pivot": [11.0, 20.25, -0.25],
        "cubes": [
            cube([8.2, 21.6, -5.0], [8.9, 2.0, 9.6], "obsidian", pivot=[12.65, 22.6, -0.2], rotation=[0, 0, 11]),
            cube([9.0, 23.25, -4.6], [7.4, 0.34, 8.8], "violet", pivot=[12.7, 23.42, -0.2], rotation=[0, 0, 11]),
            cube([15.0, 22.0, -2.1], [4.8, 0.52, 3.6], "void", pivot=[17.4, 22.26, -0.3], rotation=[0, 0, 38]),
            cube([15.65, 22.55, -1.8], [3.6, 0.18, 3.0], "rift", pivot=[17.45, 22.64, -0.3], rotation=[0, 0, 38]),
        ],
    }


def shroud_right() -> dict:
    return {
        "name": "ouros_rift_shroud_right",
        "parent": "arm_right",
        "pivot": [-11.0, 20.25, -0.25],
        "cubes": [
            cube([-17.1, 21.7, -4.8], [8.2, 1.65, 9.2], "void", pivot=[-13.0, 22.525, -0.2], rotation=[0, 0, -8]),
            cube([-16.5, 23.15, -4.4], [7.2, 0.30, 8.4], "silver", pivot=[-12.9, 23.30, -0.2], rotation=[0, 0, -8]),
            cube([-19.5, 21.7, -1.9], [4.1, 0.46, 3.2], "violet", pivot=[-17.45, 21.93, -0.3], rotation=[0, 0, -31]),
            cube([-19.0, 22.15, -1.6], [3.1, 0.16, 2.6], "ember", pivot=[-17.45, 22.23, -0.3], rotation=[0, 0, -31]),
        ],
    }


def back_pylons() -> dict:
    return {
        "name": "ouros_rift_back_pylons",
        "parent": "torso",
        "pivot": [0, 21.0, 10.0],
        "cubes": [
            cube([-10.2, 16.0, 10.7], [2.2, 17.8, 2.0], "void", pivot=[-9.1, 24.9, 11.7], rotation=[-7, 0, -6]),
            cube([8.0, 16.0, 10.7], [2.2, 17.8, 2.0], "obsidian", pivot=[9.1, 24.9, 11.7], rotation=[-7, 0, 6]),
            cube([-10.55, 32.4, 10.4], [2.9, 4.6, 2.6], "violet", pivot=[-9.1, 34.7, 11.7], rotation=[-7, 0, -6]),
            cube([7.65, 32.4, 10.4], [2.9, 4.6, 2.6], "void", pivot=[9.1, 34.7, 11.7], rotation=[-7, 0, 6]),
            cube([-9.55, 18.0, 10.35], [0.90, 13.8, 0.24], "rift", pivot=[-9.1, 24.9, 10.47], rotation=[-7, 0, -6]),
            cube([8.65, 18.0, 10.35], [0.90, 13.8, 0.24], "magenta", pivot=[9.1, 24.9, 10.47], rotation=[-7, 0, 6]),
            cube([-2.6, 8.0, 11.0], [5.2, 2.2, 1.8], "silver"),
            cube([-1.1, 8.35, 10.75], [2.2, 1.45, 0.24], "rift", pivot=[0, 9.075, 10.87], rotation=[0, 0, 45]),
        ],
    }


def wrist_left() -> dict:
    return {
        "name": "ouros_rift_wrist_left",
        "parent": "arm_left2",
        "pivot": [16.0, 20.25, 3.25],
        "cubes": [
            cube([15.45, 16.4, -4.25], [6.0, 1.1, 8.0], "void"),
            cube([15.7, 17.28, -4.45], [5.5, 0.25, 8.4], "silver"),
            cube([20.7, 17.5, -1.0], [0.42, 2.8, 1.8], "rift"),
        ],
    }


def wrist_right() -> dict:
    return {
        "name": "ouros_rift_wrist_right",
        "parent": "arm_right2",
        "pivot": [-16.0, 20.25, 3.25],
        "cubes": [
            cube([-21.45, 16.4, -4.25], [6.0, 1.1, 8.0], "obsidian"),
            cube([-21.2, 17.28, -4.45], [5.5, 0.25, 8.4], "violet"),
            cube([-21.12, 17.5, -1.0], [0.42, 2.8, 1.8], "ember"),
        ],
    }


def shadow_mantle() -> dict:
    return {
        "name": "ouros_rift_shadow_mantle",
        "parent": "body",
        "pivot": [0, 10.0, 9.5],
        "cubes": [
            cube([-10.8, 7.0, 10.4], [7.8, 11.0, 0.58], "void", pivot=[-6.9, 17.2, 10.69], rotation=[-10, 0, 10]),
            cube([3.0, 7.0, 10.4], [7.8, 11.0, 0.58], "obsidian", pivot=[6.9, 17.2, 10.69], rotation=[-10, 0, -10]),
            cube([-10.25, 6.8, 10.88], [6.9, 0.24, 0.24], "magenta", pivot=[-6.8, 6.92, 11.0], rotation=[-10, 0, 10]),
            cube([3.35, 6.8, 10.88], [6.9, 0.24, 0.24], "rift", pivot=[6.8, 6.92, 11.0], rotation=[-10, 0, -10]),
            cube([-1.25, 4.2, 11.0], [2.5, 4.2, 0.52], "violet", pivot=[0, 8.0, 11.26], rotation=[-12, 0, 45]),
        ],
    }


COSMETIC_BONES = [
    rift_halo,
    rift_collar,
    shroud_left,
    shroud_right,
    back_pylons,
    wrist_left,
    wrist_right,
    shadow_mantle,
]


def build(official_path: Path, model_out: Path, overlay_out: Path, metadata_out: Path) -> None:
    global PIXELS
    doc = json.loads(official_path.read_text(encoding="utf-8"))
    geometry = doc["minecraft:geometry"][0]
    PIXELS = choose_palette_pixels(geometry)

    derived = json.loads(json.dumps(doc))
    model = derived["minecraft:geometry"][0]
    original_bones = model["bones"]
    original_count = len(original_bones)
    model["description"]["identifier"] = "geometry.ouros_rift_warden_gengar"
    cosmetics = [factory() for factory in COSMETIC_BONES]
    model["bones"] = original_bones + cosmetics

    model_out.parent.mkdir(parents=True, exist_ok=True)
    model_out.write_text(json.dumps(derived, separators=(",", ":")) + "\n", encoding="utf-8")

    width = int(geometry["description"]["texture_width"])
    height = int(geometry["description"]["texture_height"])
    overlay = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    for material, pixel in PIXELS.items():
        overlay.putpixel(pixel, PALETTE[material])
    overlay_out.parent.mkdir(parents=True, exist_ok=True)
    overlay.save(overlay_out, optimize=True)

    metadata = {
        "format": "ouros.cobblemon-skin-build.v1",
        "species": "cobblemon:gengar",
        "concept": "Rift Warden",
        "sourceGeometryIdentifier": geometry["description"]["identifier"],
        "derivedGeometryIdentifier": model["description"]["identifier"],
        "originalBoneCount": original_count,
        "derivedBoneCount": len(model["bones"]),
        "cosmeticBones": [b["name"] for b in cosmetics],
        "cosmeticCubeCount": sum(len(b.get("cubes", [])) for b in cosmetics),
        "palettePixels": {key: list(value) for key, value in PIXELS.items()},
        "textureSize": [width, height],
        "artDirection": "broken rift halo, asymmetric spectral shoulder shrouds, twin back pylons, warded wrists and split shadow mantle; official face, mouth, ears, limbs and tail remain unobstructed",
    }
    metadata_out.parent.mkdir(parents=True, exist_ok=True)
    metadata_out.write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--official", type=Path, required=True)
    parser.add_argument("--model-out", type=Path, required=True)
    parser.add_argument("--overlay-out", type=Path, required=True)
    parser.add_argument("--metadata-out", type=Path, required=True)
    args = parser.parse_args()
    build(args.official, args.model_out, args.overlay_out, args.metadata_out)


if __name__ == "__main__":
    main()
