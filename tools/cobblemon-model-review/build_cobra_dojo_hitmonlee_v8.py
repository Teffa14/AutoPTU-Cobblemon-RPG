#!/usr/bin/env python3
"""Build Hitmonlee Cobra Dojo v8 from exact Cobblemon 1.7.3 Hitmonlee.

V8 is a presentation-only artistic overhaul after v7 failed visual review.
The official 30 Hitmonlee bones remain copied without edits and in order.
This pass adds larger connected masses instead of accessory noise:
- cobra war-cowl framing the head and upper torso;
- dominant asymmetric pauldrons with layered rear fins;
- deep sleeveless war-gi shell and split rear mantle;
- broad champion belt with long front/rear battle skirts;
- integrated shin/foot strike armor grouped to official animated leg bones.

AutoPTU/Ouros remains authoritative for all battle-state facts.
"""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
from pathlib import Path
from PIL import Image

PALETTE = {
    "obsidian": (13, 15, 14, 255),
    "charcoal": (41, 45, 39, 255),
    "gold": (232, 181, 34, 255),
    "gold_dark": (131, 88, 20, 255),
    "ivory": (224, 211, 167, 255),
    "cobra_green": (58, 103, 42, 255),
    "storm_green": (86, 135, 53, 255),
    "shadow": (5, 6, 5, 255),
    "leather": (94, 56, 35, 255),
    "steel": (152, 160, 153, 255),
}
PIXELS = {name: (index, 63) for index, name in enumerate(PALETTE)}
FACES = ("north", "east", "south", "west", "up", "down")


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def uv(material: str) -> dict:
    x, y = PIXELS[material]
    return {face: {"uv": [x, y], "uv_size": [1, 1]} for face in FACES}


def cube(origin, size, material, **extra) -> dict:
    out = {"origin": origin, "size": size, "uv": uv(material)}
    out.update({k: v for k, v in extra.items() if v is not None})
    return out


def war_cowl() -> dict:
    return {
        "name": "ouros_cobra_war_cowl",
        "parent": "torso2",
        "pivot": [0, 20.6, 0],
        "cubes": [
            cube([-5.7, 18.4, -3.55], [1.25, 5.2, 7.1], "obsidian", pivot=[-5.0, 20.7, 0], rotation=[0,0,-9]),
            cube([4.45, 18.4, -3.55], [1.25, 5.2, 7.1], "charcoal", pivot=[5.0, 20.7, 0], rotation=[0,0,9]),
            cube([-4.9, 22.2, -3.7], [9.8, 1.05, 0.34], "shadow"),
            cube([-4.9, 22.2, 3.36], [9.8, 1.05, 0.34], "shadow"),
            cube([-3.95, 22.95, -3.92], [7.9, 0.58, 0.20], "gold"),
            cube([-3.3, 18.1, 3.35], [6.6, 4.0, 0.65], "obsidian", pivot=[0,20.1,3.65], rotation=[-8,0,0]),
            cube([-2.2, 19.0, 3.92], [4.4, 2.8, 0.28], "cobra_green", pivot=[0,20.4,4.0], rotation=[-10,0,0]),
            cube([-0.55, 20.0, 4.22], [1.1, 1.1, 0.18], "gold", pivot=[0,20.55,4.31], rotation=[0,0,45]),
        ],
    }


def pauldron(name: str, left: bool) -> dict:
    s = -1 if left else 1
    x = -8.8 if left else 4.2
    material = "obsidian" if left else "charcoal"
    accent = "gold" if left else "gold_dark"
    return {
        "name": name,
        "parent": "torso2",
        "pivot": [-6.4 if left else 6.4, 18.2, 0],
        "cubes": [
            cube([x,16.55,-3.35],[4.6,2.25,6.7],material,pivot=[x+2.3,17.7,0],rotation=[0,0,s*11]),
            cube([x+0.25,18.45,-3.05],[4.0,0.42,6.1],accent,pivot=[x+2.25,18.66,0],rotation=[0,0,s*11]),
            cube([x+0.55,15.55,2.9],[3.5,3.65,1.05],"shadow",pivot=[x+2.3,17.35,3.42],rotation=[0,s*8,s*12]),
            cube([x+0.95,16.0,3.82],[2.65,2.75,0.38],"cobra_green",pivot=[x+2.28,17.38,4.0],rotation=[0,s*10,s*12]),
        ],
    }


def war_gi() -> dict:
    return {
        "name": "ouros_cobra_war_gi",
        "parent": "torso",
        "pivot": [0,14.8,0],
        "cubes": [
            cube([-4.55,11.5,-3.45],[3.55,6.9,0.62],"obsidian",pivot=[-2.8,14.9,-3.14],rotation=[0,0,-5]),
            cube([1.0,11.5,-3.45],[3.55,6.9,0.62],"charcoal",pivot=[2.8,14.9,-3.14],rotation=[0,0,5]),
            cube([-4.65,11.5,-2.95],[0.65,6.9,5.9],"obsidian"),
            cube([4.0,11.5,-2.95],[0.65,6.9,5.9],"charcoal"),
            cube([-4.35,11.6,3.0],[8.7,6.6,0.65],"shadow"),
            cube([-3.95,12.2,3.66],[7.9,5.3,0.38],"obsidian"),
            cube([-3.45,13.7,-3.75],[0.52,5.0,0.22],"gold",pivot=[-3.2,16.2,-3.64],rotation=[0,0,-34]),
            cube([2.93,13.7,-3.75],[0.52,5.0,0.22],"gold",pivot=[3.2,16.2,-3.64],rotation=[0,0,34]),
            cube([-2.0,14.4,3.95],[4.0,2.5,0.24],"cobra_green",pivot=[0,15.65,4.07],rotation=[0,0,45]),
            cube([-0.35,15.45,4.2],[0.7,1.8,0.18],"gold"),
        ],
    }


def belt_and_skirts() -> dict:
    return {
        "name": "ouros_champion_war_skirts",
        "parent": "torso",
        "pivot": [0,11.7,0],
        "cubes": [
            cube([-4.55,11.15,-3.5],[9.1,1.5,0.55],"shadow"),
            cube([-4.55,11.15,2.95],[9.1,1.5,0.55],"shadow"),
            cube([-1.85,10.85,-3.95],[3.7,1.9,0.48],"gold"),
            cube([-0.55,10.2,-4.18],[1.1,1.0,0.28],"cobra_green"),
            cube([-4.1,4.7,-3.25],[3.35,6.75,0.72],"obsidian",pivot=[-2.45,11.2,-2.9],rotation=[-8,0,7]),
            cube([0.6,5.9,-3.2],[3.25,5.55,0.7],"charcoal",pivot=[2.2,11.2,-2.9],rotation=[-8,0,-6]),
            cube([-4.45,3.9,3.15],[3.8,7.55,0.78],"obsidian",pivot=[-2.55,11.25,3.5],rotation=[-10,0,5]),
            cube([0.45,5.1,3.15],[3.7,6.35,0.78],"charcoal",pivot=[2.3,11.25,3.5],rotation=[-10,0,-5]),
            cube([-3.85,4.1,3.98],[2.85,0.35,0.22],"gold"),
            cube([0.95,5.3,3.98],[2.75,0.35,0.22],"gold_dark"),
        ],
    }


def strike_guard(name: str, parent: str, left: bool, tier: int) -> dict:
    sign = 1 if left else -1
    accent = "gold" if left else "gold_dark"
    return {
        "name": name,
        "parent": parent,
        "pivot": [0,0,0],
        "cubes": [
            cube([-1.9,-0.25,-1.95],[3.8,0.48,3.9],"ivory"),
            cube([-1.7,0.45,-2.15],[3.4,0.4,0.28],accent,pivot=[0,0.65,-2.01],rotation=[0,0,sign*10]),
            cube([1.55 if left else -1.85,-0.15,-1.7],[0.3,1.8,3.4],"steel"),
            cube([-1.4,-0.7,-2.2],[2.8,0.3,0.25],"shadow",pivot=[0,-0.55,-2.08],rotation=[0,0,sign*(8+tier*2)]),
        ],
    }


def foot_armor(name: str, parent: str, left: bool) -> dict:
    accent = "gold" if left else "gold_dark"
    return {
        "name": name,
        "parent": parent,
        "pivot": [0,0,0],
        "cubes": [
            cube([-2.35,-1.0,-3.1],[4.7,1.0,5.6],"leather"),
            cube([-2.1,-1.2,-3.45],[4.2,0.45,1.0],accent),
            cube([-1.75,-1.1,-4.0],[3.5,0.42,0.75],"steel"),
            cube([1.8 if left else -2.25,-1.0,-4.45],[0.45,0.55,1.25],"steel"),
        ],
    }


def build(model_path: Path, normal_path: Path, shiny_path: Path, out_root: Path) -> dict:
    model = json.loads(model_path.read_text())
    geo = model["minecraft:geometry"][0]
    original = copy.deepcopy(geo["bones"])
    cosmetics = [
        war_cowl(),
        pauldron("ouros_cobra_left_pauldron", True),
        pauldron("ouros_cobra_right_pauldron", False),
        war_gi(),
        belt_and_skirts(),
        strike_guard("ouros_left_shin_guard_2", "leg_left2", True, 2),
        strike_guard("ouros_left_shin_guard_3", "leg_left3", True, 3),
        strike_guard("ouros_left_shin_guard_4", "leg_left4", True, 4),
        strike_guard("ouros_right_shin_guard_2", "leg_right2", False, 2),
        strike_guard("ouros_right_shin_guard_3", "leg_right3", False, 3),
        strike_guard("ouros_right_shin_guard_4", "leg_right4", False, 4),
        foot_armor("ouros_left_foot_strike_armor", "foot_left", True),
        foot_armor("ouros_right_foot_strike_armor", "foot_right", False),
    ]
    geo["description"]["identifier"] = "geometry.ouros_cobra_dojo_hitmonlee_v8"
    geo["bones"] = original + cosmetics
    out_root.mkdir(parents=True, exist_ok=True)
    model_out = out_root / "ouros_cobra_dojo_hitmonlee_v8.geo.json"
    model_out.write_text(json.dumps(model, indent=2) + "\n")

    changed = list(PIXELS.values())
    texture_hashes = {}
    for src, target in [(normal_path, "ouros_cobra_dojo_hitmonlee_v8.png"), (shiny_path, "ouros_cobra_dojo_hitmonlee_v8_shiny.png")]:
        im = Image.open(src).convert("RGBA")
        assert im.size == (64,64)
        for name, rgba in PALETTE.items():
            x,y = PIXELS[name]
            assert im.getpixel((x,y))[3] == 0, (src, name, (x,y), im.getpixel((x,y)))
            im.putpixel((x,y), rgba)
        dst = out_root / target
        im.save(dst)
        texture_hashes[target] = sha256(dst)

    report = {
        "concept": "Hitmonlee Cobra Dojo Champion v8",
        "presentationOnly": True,
        "originalBones": len(original),
        "derivedBones": len(original) + len(cosmetics),
        "cosmeticBones": len(cosmetics),
        "cosmeticCubes": sum(len(b.get("cubes",[])) for b in cosmetics),
        "officialModelSha256": sha256(model_path),
        "derivedModelSha256": sha256(model_out),
        "officialNormalSha256": sha256(normal_path),
        "officialShinySha256": sha256(shiny_path),
        "derivedTextures": texture_hashes,
        "uvReservations": changed,
        "artIntent": ["cobra war-cowl", "dominant asymmetric pauldrons", "deep war-gi shell", "long split battle skirts", "integrated strike armor"],
    }
    (out_root / "build-report-v8.json").write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report, indent=2))
    return report


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--model", type=Path, required=True)
    ap.add_argument("--normal", type=Path, required=True)
    ap.add_argument("--shiny", type=Path, required=True)
    ap.add_argument("--output-root", type=Path, required=True)
    a = ap.parse_args()
    build(a.model, a.normal, a.shiny, a.output_root)

if __name__ == "__main__":
    main()
