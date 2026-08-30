#!/usr/bin/env python3
"""Build Storm Courier v4 as a full visual transformation around official Pikachu.

The exact Cobblemon male/female geometry remains untouched except for the derived
geometry identifier and appended `ouros_*` bones. Full-surface textures are
created from the exact official normal/shiny textures without changing their
size, alpha footprint or the model UV layout.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image

PALETTE = {
    "stormcloth": (35, 58, 72, 255),
    "stormcloth_light": (69, 104, 116, 255),
    "charcoal": (28, 34, 41, 255),
    "copper": (184, 96, 48, 255),
    "brass": (221, 178, 72, 255),
    "insulator": (202, 212, 203, 255),
    "electric": (99, 221, 239, 220),
    "deep_navy": (20, 36, 52, 255),
}
PIXELS = {name: (index, 63) for index, name in enumerate(PALETTE)}
FACES = ("north", "east", "south", "west", "up", "down")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def solid_uv(material: str) -> dict:
    x, y = PIXELS[material]
    return {face: {"uv": [x, y], "uv_size": [1, 1]} for face in FACES}


def cube(origin, size, material, **extra) -> dict:
    value = {"origin": origin, "size": size, "uv": solid_uv(material)}
    value.update({key: item for key, item in extra.items() if item is not None})
    return value


def head_system() -> dict:
    """One connected visor/cowl silhouette rather than separate goggles and blocks."""
    return {
        "name": "ouros_storm_head_system",
        "parent": "head_angle",
        "pivot": [0, 16.0, 0.0],
        "cubes": [
            # Rear hood mass.
            cube([-5.25, 13.35, 3.28], [10.50, 5.82, 1.05], "stormcloth"),
            cube([-4.65, 18.84, 2.80], [9.30, 0.58, 1.52], "deep_navy"),
            cube([-5.55, 13.20, -0.20], [1.22, 5.65, 3.82], "stormcloth"),
            cube([4.33, 13.20, -0.20], [1.22, 5.65, 3.82], "stormcloth"),
            # Large translucent visor and heavy brow. Face remains visible beneath it.
            cube([-4.28, 14.35, -4.72], [8.56, 2.10, 0.18], "electric"),
            cube([-4.62, 16.42, -4.78], [9.24, 0.72, 0.36], "deep_navy"),
            cube([-0.52, 14.30, -4.84], [1.04, 2.28, 0.22], "brass"),
            # Cheek-side armored rails visually connect visor to cowl.
            cube([-5.18, 13.18, -3.86], [0.76, 3.60, 4.26], "copper"),
            cube([4.42, 13.18, -3.86], [0.76, 3.60, 4.26], "copper"),
            cube([-5.42, 12.72, -1.72], [1.08, 1.42, 3.46], "charcoal"),
            cube([4.34, 12.72, -1.72], [1.08, 1.42, 3.46], "charcoal"),
            # Asymmetric weather crest kept outside the official ears.
            cube([-6.74, 18.28, 1.05], [3.10, 0.64, 1.72], "copper", pivot=[-5.19, 18.60, 1.91], rotation=[0, 0, -27]),
            cube([-6.30, 18.94, 1.20], [2.40, 0.30, 1.30], "electric", pivot=[-5.10, 19.09, 1.85], rotation=[0, 0, -27]),
            cube([3.76, 18.50, 1.18], [2.30, 0.48, 1.38], "brass", pivot=[4.91, 18.74, 1.87], rotation=[0, 0, 20]),
        ],
    }


def mantle_shell() -> dict:
    """Large continuous shoulder yoke, collar and split stormcoat."""
    return {
        "name": "ouros_storm_mantle_shell",
        "parent": "torso2",
        "pivot": [0, 9.8, 0.6],
        "cubes": [
            # Connected shoulder yoke.
            cube([-5.70, 10.18, -2.92], [11.40, 1.42, 6.20], "deep_navy"),
            cube([-7.82, 9.52, -2.54], [3.08, 2.18, 5.50], "stormcloth", pivot=[-6.28, 10.61, 0.21], rotation=[0, 0, -11]),
            cube([4.74, 9.72, -2.42], [2.52, 1.76, 5.22], "stormcloth_light", pivot=[6.00, 10.60, 0.19], rotation=[0, 0, 9]),
            cube([-7.98, 11.35, -2.22], [3.28, 0.34, 4.80], "copper", pivot=[-6.34, 11.52, 0.18], rotation=[0, 0, -11]),
            cube([4.70, 11.26, -2.16], [2.72, 0.30, 4.62], "brass", pivot=[6.06, 11.41, 0.15], rotation=[0, 0, 9]),
            # High rear collar connects head and coat visually.
            cube([-4.72, 10.92, 2.76], [9.44, 2.28, 1.18], "charcoal"),
            cube([-3.94, 12.84, 2.94], [7.88, 0.44, 0.92], "copper"),
            # Split long coat, intentionally unequal for a clear rear silhouette.
            cube([-4.72, 4.42, 3.86], [4.22, 6.82, 0.76], "stormcloth", pivot=[-2.61, 10.48, 4.24], rotation=[-8, 0, 8]),
            cube([0.42, 5.16, 3.88], [4.08, 6.08, 0.76], "deep_navy", pivot=[2.46, 10.50, 4.26], rotation=[-8, 0, -7]),
            cube([-4.46, 4.42, 4.58], [3.72, 0.38, 0.24], "brass", pivot=[-2.60, 4.61, 4.70], rotation=[-8, 0, 8]),
            cube([0.66, 5.16, 4.60], [3.58, 0.34, 0.24], "copper", pivot=[2.45, 5.33, 4.72], rotation=[-8, 0, -7]),
        ],
    }


def torso_suit() -> dict:
    """Broad shallow shell panels make the torso read as a garment, not straps."""
    return {
        "name": "ouros_storm_torso_suit",
        "parent": "torso2",
        "pivot": [0, 8.0, 0.0],
        "cubes": [
            cube([-3.86, 6.20, -4.05], [7.72, 4.30, 0.48], "stormcloth"),
            cube([-4.26, 6.10, -3.42], [0.68, 4.42, 6.70], "deep_navy"),
            cube([3.58, 6.10, -3.42], [0.68, 4.42, 6.70], "deep_navy"),
            cube([-3.78, 6.08, 3.10], [7.56, 4.36, 0.56], "stormcloth_light"),
            cube([-4.12, 5.46, -3.52], [8.24, 1.10, 7.08], "charcoal"),
            cube([-4.24, 5.66, -3.72], [8.48, 0.30, 0.38], "copper"),
            # Central front seam makes the suit read as constructed clothing.
            cube([-0.28, 6.36, -4.30], [0.56, 3.78, 0.30], "brass"),
            cube([-2.92, 9.86, -4.24], [5.84, 0.34, 0.30], "insulator"),
        ],
    }


def storm_core() -> dict:
    """Large chest power core connected to the suit by conductor ribs."""
    return {
        "name": "ouros_storm_core",
        "parent": "torso2",
        "pivot": [0, 8.25, -4.45],
        "cubes": [
            cube([-1.72, 7.04, -4.48], [3.44, 2.72, 0.50], "charcoal", pivot=[0, 8.40, -4.23], rotation=[0, 0, 45]),
            cube([-1.18, 7.58, -4.76], [2.36, 1.64, 0.28], "electric", pivot=[0, 8.40, -4.62], rotation=[0, 0, 45]),
            cube([-3.72, 8.28, -4.32], [2.56, 0.46, 0.34], "copper", pivot=[-2.44, 8.51, -4.15], rotation=[0, 0, 18]),
            cube([1.16, 8.28, -4.32], [2.56, 0.46, 0.34], "brass", pivot=[2.44, 8.51, -4.15], rotation=[0, 0, -18]),
            cube([-3.54, 7.18, -4.30], [2.20, 0.38, 0.30], "brass", pivot=[-2.44, 7.37, -4.15], rotation=[0, 0, -24]),
            cube([1.34, 7.18, -4.30], [2.20, 0.38, 0.30], "copper", pivot=[2.44, 7.37, -4.15], rotation=[0, 0, 24]),
        ],
    }


def power_frame() -> dict:
    """One large expedition machine integrating pack body, bridge and batteries."""
    return {
        "name": "ouros_storm_power_frame",
        "parent": "torso2",
        "pivot": [0, 9.0, 4.8],
        "cubes": [
            cube([-4.24, 6.00, 4.02], [8.48, 6.28, 2.54], "charcoal"),
            cube([-3.72, 6.48, 6.42], [7.44, 5.32, 0.44], "stormcloth"),
            cube([-4.50, 10.96, 4.18], [9.00, 1.44, 2.62], "deep_navy"),
            cube([-4.70, 6.74, 4.24], [1.02, 3.94, 2.16], "copper"),
            cube([3.68, 7.38, 4.24], [1.02, 3.30, 2.16], "brass"),
            cube([-3.18, 7.04, 6.78], [6.36, 0.48, 0.26], "copper"),
            cube([-3.18, 10.62, 6.78], [6.36, 0.48, 0.26], "brass"),
            cube([-1.78, 8.00, 6.88], [3.56, 2.20, 0.22], "electric"),
            # Wide bridge makes both pylons read as components of the same machine.
            cube([-6.44, 11.72, 4.28], [12.88, 0.72, 1.68], "deep_navy"),
            cube([-5.94, 12.46, 4.52], [11.88, 0.26, 1.20], "copper"),
        ],
    }


def left_pylon() -> dict:
    return {
        "name": "ouros_storm_left_pylon",
        "parent": "torso2",
        "pivot": [-5.70, 13.5, 4.9],
        "cubes": [
            cube([-6.18, 10.92, 4.34], [0.96, 6.58, 1.12], "charcoal", pivot=[-5.70, 14.21, 4.90], rotation=[0, 0, -8]),
            cube([-6.44, 12.34, 4.08], [1.48, 0.54, 1.64], "copper", pivot=[-5.70, 12.61, 4.90], rotation=[0, 0, -8]),
            cube([-6.38, 14.16, 4.14], [1.36, 1.62, 1.52], "electric", pivot=[-5.70, 14.97, 4.90], rotation=[0, 0, -8]),
            cube([-8.52, 16.82, 4.10], [3.00, 0.54, 1.62], "brass", pivot=[-7.02, 17.09, 4.91], rotation=[0, 0, -22]),
            cube([-5.72, 16.94, 4.12], [2.64, 0.48, 1.58], "copper", pivot=[-4.40, 17.18, 4.91], rotation=[0, 0, 28]),
        ],
    }


def right_pylon() -> dict:
    return {
        "name": "ouros_storm_right_pylon",
        "parent": "torso2",
        "pivot": [5.55, 13.0, 4.9],
        "cubes": [
            cube([5.08, 10.94, 4.36], [0.94, 5.42, 1.08], "charcoal", pivot=[5.55, 13.65, 4.90], rotation=[0, 0, 7]),
            cube([4.82, 12.02, 4.10], [1.46, 0.50, 1.60], "brass", pivot=[5.55, 12.27, 4.90], rotation=[0, 0, 7]),
            cube([4.90, 13.62, 4.18], [1.30, 1.44, 1.44], "electric", pivot=[5.55, 14.34, 4.90], rotation=[0, 0, 7]),
            cube([5.58, 15.74, 4.14], [2.66, 0.48, 1.54], "copper", pivot=[6.91, 15.98, 4.91], rotation=[0, 0, 20]),
        ],
    }


def tail_conductor() -> dict:
    """A single continuous grounding spine that visually follows the official tail."""
    return {
        "name": "ouros_storm_tail_conductor",
        "parent": "tail2",
        "pivot": [0, 10.4, 10.0],
        "cubes": [
            cube([-0.92, 8.72, 7.32], [1.84, 1.54, 3.78], "charcoal"),
            cube([-1.08, 9.10, 8.02], [2.16, 0.78, 1.62], "copper"),
            cube([-0.62, 9.86, 10.18], [1.24, 0.54, 2.56], "deep_navy"),
            cube([-0.54, 10.44, 11.76], [1.08, 0.48, 2.40], "brass"),
            cube([-0.46, 10.96, 13.20], [0.92, 0.44, 2.10], "electric"),
            cube([-1.82, 10.02, 11.08], [1.24, 1.06, 1.42], "charcoal", pivot=[-1.20, 10.55, 11.79], rotation=[0, 0, -22]),
            cube([0.58, 10.02, 11.08], [1.24, 1.06, 1.42], "charcoal", pivot=[1.20, 10.55, 11.79], rotation=[0, 0, 22]),
            cube([-1.58, 10.78, 12.62], [1.08, 0.92, 1.28], "copper", pivot=[-1.04, 11.24, 13.26], rotation=[0, 0, -24]),
            cube([0.50, 10.78, 12.62], [1.08, 0.92, 1.28], "brass", pivot=[1.04, 11.24, 13.26], rotation=[0, 0, 24]),
        ],
    }


def build_model(source: Path, identifier: str) -> dict:
    data = json.loads(source.read_text(encoding="utf-8"))
    geometry = data["minecraft:geometry"][0]
    geometry["description"]["identifier"] = identifier
    geometry["bones"].extend(
        [
            head_system(),
            mantle_shell(),
            torso_suit(),
            storm_core(),
            power_frame(),
            left_pylon(),
            right_pylon(),
            tail_conductor(),
        ]
    )
    return data


def transform_pixel(r: int, g: int, b: int, a: int, x: int, y: int) -> tuple[int, int, int, int]:
    if a == 0:
        return (r, g, b, a)

    high = max(r, g, b)
    low = min(r, g, b)
    luminance = (299 * r + 587 * g + 114 * b) // 1000

    # Preserve tiny near-white highlights while making them cool insulation.
    if low > 205:
        v = max(194, min(238, luminance))
        return (v - 8, v, min(255, v + 4), a)

    # Keep black facial definition readable, with a subtle blue-charcoal material cast.
    if high < 64:
        v = max(18, min(54, luminance + 8))
        return (v - 3, v + 1, v + 7, a)

    # Red/orange cheek and warm markings become high-energy copper/amber accents.
    if r > g * 1.28 and r > b * 1.55:
        factor = max(0.55, min(1.15, luminance / 150))
        return (
            min(238, int(202 * factor)),
            min(166, int(105 * factor)),
            min(78, int(48 * factor)),
            a,
        )

    # Pikachu yellow body becomes stormcloth while retaining original light/shadow values.
    if r > 105 and g > 78 and b < min(r, g) * 0.78:
        t = max(0.0, min(1.0, (luminance - 70) / 170))
        base = (31, 53, 68)
        light = (91, 126, 132)
        rr = int(base[0] + (light[0] - base[0]) * t)
        gg = int(base[1] + (light[1] - base[1]) * t)
        bb = int(base[2] + (light[2] - base[2]) * t)
        # Very subtle woven modulation on already-visible texels only.
        weave = 5 if (x + 2 * y) % 5 == 0 else 0
        return (min(255, rr + weave), min(255, gg + weave), min(255, bb + weave), a)

    # Browns/dark warm stripes become conductive charcoal/copper breakup.
    if r > b * 1.2 and g >= b and luminance < 150:
        if (x + y) % 4 == 0:
            return (143, 77, 47, a)
        v = max(34, min(88, luminance))
        return (v, v - 3, min(110, v + 12), a)

    # Other opaque pixels receive a restrained cool treatment instead of remaining unrelated.
    v = max(34, min(190, luminance))
    return (int(v * 0.72), int(v * 0.88), min(220, int(v * 1.02)), a)


def derive_texture(source: Path, output: Path) -> None:
    image = Image.open(source).convert("RGBA")
    result = Image.new("RGBA", image.size, (0, 0, 0, 0))
    for y in range(image.height):
        for x in range(image.width):
            result.putpixel((x, y), transform_pixel(*image.getpixel((x, y)), x, y))
    output.parent.mkdir(parents=True, exist_ok=True)
    result.save(output, optimize=True)


def write_overlay(output: Path) -> None:
    overlay = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    for name, color in PALETTE.items():
        overlay.putpixel(PIXELS[name], color)
    output.parent.mkdir(parents=True, exist_ok=True)
    overlay.save(output, optimize=True)


def write_metadata(official: Path, derived: Path, output: Path, variant: str) -> None:
    metadata = {
        "concept": "Storm Courier v4 full transformation",
        "variant": variant,
        "officialTextureBaselineSha256": sha256(official),
        "derivedTexture": derived.name,
        "derivedTextureSha256": sha256(derived),
        "bodyTexelRework": "Reworks visible official Pikachu body texels into a coherent cool stormcloth/conductive-field palette while preserving face definition, original UV mapping and transparency footprint.",
        "paletteIntent": "Deep navy and blue-grey stormcloth, charcoal conductors, weathered copper/brass hardware, controlled cyan electric accents.",
        "materialIntent": "The body and attached equipment must read as one weatherproof storm-runner suit and expedition power system rather than unchanged fur plus isolated accessories.",
        "allowAlphaSemanticsChange": False,
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--male", type=Path, required=True)
    parser.add_argument("--female", type=Path, required=True)
    parser.add_argument("--base-texture", type=Path, required=True)
    parser.add_argument("--shiny-texture", type=Path, required=True)
    parser.add_argument("--male-out", type=Path, required=True)
    parser.add_argument("--female-out", type=Path, required=True)
    parser.add_argument("--normal-texture-out", type=Path, required=True)
    parser.add_argument("--shiny-texture-out", type=Path, required=True)
    parser.add_argument("--overlay-out", type=Path, required=True)
    parser.add_argument("--normal-metadata-out", type=Path, required=True)
    parser.add_argument("--shiny-metadata-out", type=Path, required=True)
    args = parser.parse_args()

    for source, output, identifier in (
        (args.male, args.male_out, "geometry.ouros_storm_courier_pikachu_male"),
        (args.female, args.female_out, "geometry.ouros_storm_courier_pikachu_female"),
    ):
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(
            json.dumps(build_model(source, identifier), separators=(",", ":")) + "\n",
            encoding="utf-8",
        )

    derive_texture(args.base_texture, args.normal_texture_out)
    derive_texture(args.shiny_texture, args.shiny_texture_out)
    write_overlay(args.overlay_out)
    write_metadata(args.base_texture, args.normal_texture_out, args.normal_metadata_out, "normal")
    write_metadata(args.shiny_texture, args.shiny_texture_out, args.shiny_metadata_out, "shiny")


if __name__ == "__main__":
    main()
