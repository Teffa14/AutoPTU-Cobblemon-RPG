#!/usr/bin/env python3
"""Build Rift Warden V2 from the exact official Cobblemon Gengar geometry.

The official geometry is immutable. This builder changes only the geometry
identifier and appends Ouros-owned cosmetic bones. Biological textures are not
repainted; added equipment samples only UV-free palette texels in a transparent
accessory overlay.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from PIL import Image

PALETTE = {
    "void": (18, 13, 31, 255),
    "obsidian": (39, 30, 58, 255),
    "violet": (106, 60, 170, 255),
    "magenta": (211, 62, 190, 255),
    "rift": (169, 102, 255, 190),
    "silver": (184, 191, 213, 255),
    "bone": (224, 214, 193, 255),
    "ember": (255, 111, 89, 235),
}
FACES = ("north", "east", "south", "west", "up", "down")
PIXELS: dict[str, tuple[int, int]] = {}


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


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


def cube(origin, size, material, *, pivot=None, rotation=None, inflate=None) -> dict:
    value = {"origin": origin, "size": size, "uv": solid_uv(material)}
    if pivot is not None:
        value["pivot"] = pivot
    if rotation is not None:
        value["rotation"] = rotation
    if inflate is not None:
        value["inflate"] = inflate
    return value


def rift_cowl() -> dict:
    # Deep open-face cowl. The rear yoke is the physical root; side towers and
    # crown shards build upward without crossing Gengar's eyes/grin plane.
    cubes = [
        cube([-9.8, 25.0, 5.8], [19.6, 3.2, 5.0], "void"),
        cube([-12.5, 25.8, 5.5], [4.0, 8.8, 5.6], "obsidian", pivot=[-10.5, 30.2, 8.3], rotation=[-3, 0, -10]),
        cube([8.5, 25.8, 5.5], [4.0, 8.8, 5.6], "obsidian", pivot=[10.5, 30.2, 8.3], rotation=[-3, 0, 10]),
        cube([-13.2, 32.4, 5.7], [4.8, 5.8, 5.1], "violet", pivot=[-10.8, 35.3, 8.25], rotation=[-2, 0, -23]),
        cube([8.4, 32.4, 5.7], [4.8, 5.8, 5.1], "violet", pivot=[10.8, 35.3, 8.25], rotation=[-2, 0, 23]),
        cube([-11.8, 37.0, 6.0], [5.8, 3.3, 4.2], "void", pivot=[-8.9, 38.65, 8.1], rotation=[0, 0, -36]),
        cube([6.0, 37.0, 6.0], [5.8, 3.3, 4.2], "void", pivot=[8.9, 38.65, 8.1], rotation=[0, 0, 36]),
        cube([-8.7, 39.1, 6.2], [5.2, 2.1, 3.8], "silver", pivot=[-6.1, 40.15, 8.1], rotation=[0, 0, -24]),
        cube([3.5, 39.1, 6.2], [5.2, 2.1, 3.8], "silver", pivot=[6.1, 40.15, 8.1], rotation=[0, 0, 24]),
        cube([-10.4, 27.2, 5.1], [20.8, 0.42, 0.34], "rift"),
        cube([-11.8, 34.1, 5.0], [3.0, 0.34, 4.8], "magenta", pivot=[-10.3, 34.27, 7.4], rotation=[0, 0, -23]),
        cube([8.8, 34.1, 5.0], [3.0, 0.34, 4.8], "rift", pivot=[10.3, 34.27, 7.4], rotation=[0, 0, 23]),
    ]
    return {"name": "ouros_rift_cowl", "parent": "torso", "pivot": [0, 28.0, 7.5], "cubes": cubes}


def rift_mantle() -> dict:
    # One connected shoulder architecture instead of two isolated pauldrons.
    cubes = [
        cube([-12.8, 21.3, 3.8], [25.6, 4.6, 7.2], "obsidian"),
        cube([-17.0, 20.2, 2.8], [7.0, 5.4, 8.8], "void", pivot=[-13.5, 22.9, 7.2], rotation=[0, 0, 9]),
        cube([10.0, 20.2, 2.8], [7.0, 5.4, 8.8], "void", pivot=[13.5, 22.9, 7.2], rotation=[0, 0, -9]),
        cube([-20.6, 20.2, 3.4], [6.2, 4.2, 7.0], "violet", pivot=[-17.5, 22.3, 6.9], rotation=[0, 0, 18]),
        cube([14.4, 20.2, 3.4], [6.2, 4.2, 7.0], "violet", pivot=[17.5, 22.3, 6.9], rotation=[0, 0, -18]),
        cube([-23.8, 20.5, 4.3], [5.2, 2.7, 5.6], "void", pivot=[-21.2, 21.85, 7.1], rotation=[0, 0, 28]),
        cube([18.6, 20.5, 4.3], [5.2, 2.7, 5.6], "void", pivot=[21.2, 21.85, 7.1], rotation=[0, 0, -28]),
        cube([-16.2, 25.2, 4.3], [7.0, 1.1, 6.0], "silver", pivot=[-12.7, 25.75, 7.3], rotation=[0, 0, 7]),
        cube([9.2, 25.2, 4.3], [7.0, 1.1, 6.0], "silver", pivot=[12.7, 25.75, 7.3], rotation=[0, 0, -7]),
        cube([-23.0, 23.0, 4.1], [4.6, 0.32, 5.8], "magenta", pivot=[-20.7, 23.16, 7.0], rotation=[0, 0, 26]),
        cube([18.4, 23.0, 4.1], [4.6, 0.32, 5.8], "rift", pivot=[20.7, 23.16, 7.0], rotation=[0, 0, -26]),
    ]
    return {"name": "ouros_rift_mantle", "parent": "torso", "pivot": [0, 22.5, 6.5], "cubes": cubes}


def rift_gate() -> dict:
    # Thick portal/reliquary frame. A central backplate and lower crossbar touch
    # the body; pillars overlap that root and continue into the crown.
    cubes = [
        cube([-7.5, 13.2, 8.2], [15.0, 6.6, 4.8], "void"),
        cube([-12.0, 15.0, 9.0], [5.2, 20.5, 4.4], "obsidian", pivot=[-9.4, 25.25, 11.2], rotation=[-4, 0, -3]),
        cube([6.8, 15.0, 9.0], [5.2, 20.5, 4.4], "obsidian", pivot=[9.4, 25.25, 11.2], rotation=[-4, 0, 3]),
        cube([-12.6, 31.8, 9.1], [5.8, 8.0, 4.2], "violet", pivot=[-9.7, 35.8, 11.2], rotation=[-4, 0, -8]),
        cube([6.8, 31.8, 9.1], [5.8, 8.0, 4.2], "violet", pivot=[9.7, 35.8, 11.2], rotation=[-4, 0, 8]),
        cube([-10.6, 38.0, 9.2], [8.2, 3.0, 4.0], "void", pivot=[-6.5, 39.5, 11.2], rotation=[0, 0, -16]),
        cube([2.4, 38.0, 9.2], [8.2, 3.0, 4.0], "void", pivot=[6.5, 39.5, 11.2], rotation=[0, 0, 16]),
        cube([-4.8, 40.0, 9.3], [9.6, 3.2, 3.8], "silver"),
        cube([-10.5, 17.2, 8.6], [1.4, 16.2, 0.34], "rift", pivot=[-9.8, 25.3, 8.77], rotation=[-4, 0, -3]),
        cube([9.1, 17.2, 8.6], [1.4, 16.2, 0.34], "magenta", pivot=[9.8, 25.3, 8.77], rotation=[-4, 0, 3]),
        cube([-6.6, 14.0, 7.9], [13.2, 0.34, 0.32], "rift"),
        cube([-3.2, 41.0, 8.9], [6.4, 0.42, 0.34], "ember"),
    ]
    return {"name": "ouros_rift_gate", "parent": "torso", "pivot": [0, 20.0, 10.4], "cubes": cubes}


def rift_relic_wing() -> dict:
    # Dominant asymmetric signature wing/slab. It grows as an overlapping chain
    # from a torso root rather than floating beside the body.
    cubes = [
        cube([-10.5, 16.0, 6.8], [6.0, 8.0, 5.2], "void"),
        cube([-15.0, 18.0, 7.2], [6.0, 7.0, 4.8], "obsidian", pivot=[-12.0, 21.5, 9.6], rotation=[0, 0, -13]),
        cube([-19.4, 20.5, 7.5], [6.2, 6.4, 4.4], "violet", pivot=[-16.3, 23.7, 9.7], rotation=[0, 0, -25]),
        cube([-23.5, 23.8, 7.7], [6.0, 5.8, 4.0], "void", pivot=[-20.5, 26.7, 9.7], rotation=[0, 0, -38]),
        cube([-27.0, 27.6, 7.9], [5.5, 5.0, 3.6], "obsidian", pivot=[-24.25, 30.1, 9.7], rotation=[0, 0, -50]),
        cube([-29.8, 31.6, 8.1], [5.0, 4.2, 3.2], "violet", pivot=[-27.3, 33.7, 9.7], rotation=[0, 0, -62]),
        cube([-31.2, 35.2, 8.2], [4.5, 3.6, 2.9], "void", pivot=[-28.95, 37.0, 9.65], rotation=[0, 0, -73]),
        cube([-14.5, 19.1, 6.9], [4.0, 0.35, 4.9], "rift", pivot=[-12.5, 19.28, 9.35], rotation=[0, 0, -13]),
        cube([-18.8, 22.0, 7.0], [4.0, 0.34, 4.5], "magenta", pivot=[-16.8, 22.17, 9.25], rotation=[0, 0, -25]),
        cube([-22.9, 25.6, 7.1], [3.8, 0.32, 4.1], "rift", pivot=[-21.0, 25.76, 9.15], rotation=[0, 0, -38]),
        cube([-26.3, 29.5, 7.2], [3.4, 0.30, 3.7], "ember", pivot=[-24.6, 29.65, 9.05], rotation=[0, 0, -50]),
    ]
    return {"name": "ouros_rift_relic_wing", "parent": "torso", "pivot": [-8.0, 18.0, 8.8], "cubes": cubes}


def rift_lower_shroud() -> dict:
    # Broad lower silhouette with overlapping split panels; the root hugs the
    # body so the costume reads through the lower half instead of stopping at shoulders.
    cubes = [
        cube([-10.5, 8.5, 5.4], [21.0, 5.6, 6.6], "obsidian"),
        cube([-12.2, 4.8, 6.8], [8.0, 8.0, 4.5], "void", pivot=[-8.2, 12.0, 9.05], rotation=[-8, 0, 8]),
        cube([-5.0, 3.8, 7.0], [7.0, 8.6, 4.3], "violet", pivot=[-1.5, 11.0, 9.15], rotation=[-10, 0, 3]),
        cube([2.0, 4.4, 6.8], [7.8, 8.2, 4.5], "void", pivot=[5.9, 11.5, 9.05], rotation=[-8, 0, -7]),
        cube([8.8, 5.5, 6.4], [5.0, 7.0, 4.6], "obsidian", pivot=[11.3, 11.5, 8.7], rotation=[-7, 0, -15]),
        cube([-12.0, 4.6, 6.4], [7.5, 0.34, 4.8], "magenta", pivot=[-8.25, 4.77, 8.8], rotation=[-8, 0, 8]),
        cube([-4.6, 3.7, 6.6], [6.3, 0.34, 4.7], "rift", pivot=[-1.45, 3.87, 8.95], rotation=[-10, 0, 3]),
        cube([2.4, 4.2, 6.4], [6.8, 0.34, 4.8], "rift", pivot=[5.8, 4.37, 8.8], rotation=[-8, 0, -7]),
        cube([9.2, 5.2, 6.1], [4.2, 0.32, 4.8], "ember", pivot=[11.3, 5.36, 8.5], rotation=[-7, 0, -15]),
    ]
    return {"name": "ouros_rift_lower_shroud", "parent": "body", "pivot": [0, 11.0, 8.0], "cubes": cubes}


def armguard(name: str, parent: str, side: int) -> dict:
    sx = float(side)
    if side > 0:
        root = [8.0, 18.0, -4.6]
        outer = [13.8, 17.4, -4.8]
        blade = [18.0, 18.2, -2.0]
        rot = 12
    else:
        root = [-15.0, 18.0, -4.6]
        outer = [-20.2, 17.4, -4.8]
        blade = [-22.0, 18.2, -2.0]
        rot = -12
    return {
        "name": name,
        "parent": parent,
        "pivot": [11.0 * sx, 20.25, -0.25],
        "cubes": [
            cube(root, [7.0, 5.8, 9.4], "void", pivot=[11.5 * sx, 20.9, 0.1], rotation=[0, 0, rot]),
            cube(outer, [6.0, 4.8, 8.6], "obsidian", pivot=[16.5 * sx, 20.0, -0.1], rotation=[0, 0, rot * 2]),
            cube(blade, [4.4, 2.0, 4.8], "violet", pivot=[19.8 * sx, 19.2, 0.4], rotation=[0, 0, rot * 3]),
            cube([root[0] + (0.7 if side > 0 else 0.3), 22.8, -4.8], [5.8, 0.36, 9.8], "silver", pivot=[11.5 * sx, 22.98, 0.1], rotation=[0, 0, rot]),
            cube([outer[0] + (0.5 if side > 0 else 0.2), 21.7, -4.9], [5.1, 0.32, 8.8], "rift" if side > 0 else "magenta", pivot=[16.5 * sx, 21.86, -0.1], rotation=[0, 0, rot * 2]),
            cube([blade[0] + (0.2 if side > 0 else 0.1), 19.8, -2.1], [4.0, 0.28, 5.0], "ember" if side < 0 else "rift", pivot=[19.8 * sx, 19.94, 0.4], rotation=[0, 0, rot * 3]),
        ],
    }


def wristguard(name: str, parent: str, side: int) -> dict:
    if side > 0:
        root = [15.0, 15.8, -4.3]
        tip = [20.0, 16.0, -2.4]
        pivot = [16.0, 20.25, 3.25]
    else:
        root = [-21.6, 15.8, -4.3]
        tip = [-23.8, 16.0, -2.4]
        pivot = [-16.0, 20.25, 3.25]
    return {
        "name": name,
        "parent": parent,
        "pivot": pivot,
        "cubes": [
            cube(root, [6.6, 2.0, 8.4], "obsidian"),
            cube([root[0], 17.2, -4.5], [6.6, 1.4, 8.8], "violet"),
            cube(tip, [3.8, 3.0, 4.6], "void"),
            cube([tip[0], 18.4, -2.5], [3.8, 0.32, 4.8], "rift" if side > 0 else "ember"),
        ],
    }


COSMETIC_FACTORIES = [
    rift_cowl,
    rift_mantle,
    rift_gate,
    rift_relic_wing,
    rift_lower_shroud,
    lambda: armguard("ouros_rift_armguard_left", "arm_left", 1),
    lambda: armguard("ouros_rift_armguard_right", "arm_right", -1),
    lambda: wristguard("ouros_rift_wrist_left", "arm_left2", 1),
    lambda: wristguard("ouros_rift_wrist_right", "arm_right2", -1),
]


def build(official_path: Path, model_out: Path, overlay_out: Path, metadata_out: Path) -> None:
    global PIXELS
    doc = json.loads(official_path.read_text(encoding="utf-8"))
    geometry = doc["minecraft:geometry"][0]
    if len(geometry["bones"]) != 78:
        raise RuntimeError(f"Expected exact official Gengar 78-bone model, got {len(geometry['bones'])}")
    PIXELS = choose_palette_pixels(geometry)

    derived = json.loads(json.dumps(doc))
    model = derived["minecraft:geometry"][0]
    original_bones = model["bones"]
    model["description"]["identifier"] = "geometry.ouros_rift_warden_gengar"
    cosmetics = [factory() for factory in COSMETIC_FACTORIES]
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

    cube_count = sum(len(b.get("cubes", [])) for b in cosmetics)
    metadata = {
        "format": "ouros.rift-warden-v2-current-build.v1",
        "species": "cobblemon:gengar",
        "concept": "Rift Warden V2",
        "sourceGeometryIdentifier": geometry["description"]["identifier"],
        "derivedGeometryIdentifier": model["description"]["identifier"],
        "originalBoneCount": 78,
        "derivedBoneCount": len(model["bones"]),
        "cosmeticBones": [b["name"] for b in cosmetics],
        "cosmeticCubeCount": cube_count,
        "textureSize": [width, height],
        "palettePixels": {key: list(value) for key, value in PIXELS.items()},
        "officialTextureBaselineSha256": "7aba3220a0007d5ac3f36bc51611e485a3bedf330319fa474b907fc5b3f77b65",
        "productionBodyTexture": "cobblemon:textures/pokemon/0094_gengar/gengar.png",
        "productionBodyTextureSha256": "7aba3220a0007d5ac3f36bc51611e485a3bedf330319fa474b907fc5b3f77b65",
        "officialShinyTextureSha256": "1a0821422f8bfbe43a02132c658e48213e6adedbc1b5854f125683951deaeb21",
        "bodyTexelRework": "NONE",
        "paletteIntent": "void/obsidian armor masses with violet, silver, rift-cyan-magenta spectral insets and sparse ember fracture accents",
        "materialIntent": "thick matte portal-stone/armor shells, metallic ward edges and translucent spectral seams on added geometry only",
        "accessoryOverlay": str(overlay_out),
        "accessoryOverlaySha256": sha256(overlay_out),
        "artDirection": "deep open-face cowl, connected fortress mantle, thick dorsal rift gate, dominant asymmetric relic wing, lower split shroud and integrated arm/wrist guards; official face, eyes, grin, ears, limbs and tail remain intact",
    }
    metadata_out.parent.mkdir(parents=True, exist_ok=True)
    metadata_out.write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metadata, indent=2))


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
