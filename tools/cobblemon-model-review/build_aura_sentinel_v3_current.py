#!/usr/bin/env python3
"""Aura Sentinel current-model artistic correction pass.

The exact official Lucario geometry supplied by the pinned Cobblemon JAR stays
immutable. This pass deliberately solves the whole costume instead of piling
thin detail around the previous silhouette: one connected helmet/cowl, one
connected shoulder mantle, a compact shrine-back system, a rooted asymmetric
relic fin, split coat masses and leg-following greaves.

Official normal/shiny biological textures are copied byte-for-byte. Added Ouros
materials only use UV-free texels selected from the exact official geometry.
"""
from __future__ import annotations

import importlib.util
import json
import shutil
from pathlib import Path

REFINED_PATH = Path(__file__).with_name("build_aura_sentinel_v2_refined.py")
spec = importlib.util.spec_from_file_location("aura_v2_refined", REFINED_PATH)
refined = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(refined)
base = refined.base


def preserve_texture(source: Path, target: Path, shiny: bool) -> dict:
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, target)
    return {"changedPixels": 0, "occupiedPixels": None, "dimensions": None}


def preserved_texture_metadata(official: Path, derived: Path, shiny: bool, stats: dict) -> dict:
    return {
        "officialTextureBaselineSha256": base.sha256(official),
        "derivedTexture": derived.name,
        "derivedTextureSha256": base.sha256(derived),
        "bodyTexelRework": "NONE",
        "paletteIntent": "Official Lucario biological pixels stay exact. Midnight indigo, cobalt, gold, amethyst and aura-cyan belong only to appended equipment geometry on verified UV-free texels.",
        "materialIntent": "One connected shrine-sentinel armor language: cowl/visor, mantle, cuirass, dorsal shrine, asymmetric relic fin, split coat, arm guards and articulated greaves. No biological repaint.",
        "allowAlphaSemanticsChange": False,
    }


def helm_system() -> dict:
    c = base.cube
    return {"name": "ouros_aura_helm_system", "parent": "head_angle", "pivot": [0, 38.5, -2.0], "cubes": [
        # Deep cowl physically overlaps the official rear head envelope.
        c([-5.50, 34.00, 1.60], [11.00, 6.80, 1.70], "void"),
        c([-5.90, 34.00, -2.00], [1.20, 5.80, 4.20], "indigo"),
        c([4.70, 34.00, -2.00], [1.20, 5.80, 4.20], "indigo"),
        # One continuous open-face visor rather than scattered facial bars.
        c([-5.00, 36.50, -5.05], [10.00, 1.60, 0.35], "silver"),
        c([-5.20, 38.00, -5.12], [10.40, 0.70, 0.38], "gold"),
        c([-4.50, 36.85, -5.30], [9.00, 0.45, 0.20], "aura"),
        c([-5.85, 33.70, -4.05], [1.15, 3.20, 3.20], "void", pivot=[-5.28, 35.30, -2.45], rotation=[0, 0, -9]),
        c([4.70, 33.70, -4.05], [1.15, 3.20, 3.20], "void", pivot=[5.28, 35.30, -2.45], rotation=[0, 0, 9]),
        # Rear crown is a solid continuation of the cowl.
        c([-5.00, 39.20, 1.80], [10.00, 1.20, 1.50], "cobalt"),
        c([-3.70, 39.52, 3.08], [7.40, 0.35, 0.28], "aura"),
        # Asymmetry is carried by two rooted crest masses, not floating slivers.
        c([-7.50, 38.35, -1.40], [3.10, 2.45, 3.00], "amethyst", pivot=[-5.95, 39.58, 0.10], rotation=[0, 0, -15]),
        c([-8.35, 39.95, -1.10], [3.20, 1.65, 2.40], "cobalt", pivot=[-6.75, 40.78, 0.10], rotation=[0, 0, -22]),
        c([4.35, 38.45, -1.30], [2.70, 1.85, 2.70], "gold", pivot=[5.70, 39.38, 0.05], rotation=[0, 0, 12]),
    ]}


def mantle_shell() -> dict:
    c = base.cube
    return {"name": "ouros_aura_mantle_shell", "parent": "torso3", "pivot": [0, 29.5, 0.0], "cubes": [
        # Single deep yoke. Both pauldrons overlap it so the shoulder system is one mass.
        c([-9.00, 28.00, -3.40], [18.00, 3.00, 7.20], "void"),
        c([-11.00, 27.50, -2.80], [4.60, 4.20, 6.00], "indigo", pivot=[-8.70, 29.60, 0.20], rotation=[0, 0, -10]),
        c([6.40, 27.80, -2.60], [4.30, 3.60, 5.60], "cobalt", pivot=[8.55, 29.60, 0.20], rotation=[0, 0, 9]),
        c([-10.85, 30.65, -2.45], [4.70, 0.75, 5.40], "silver", pivot=[-8.50, 31.03, 0.25], rotation=[0, 0, -10]),
        c([6.25, 30.70, -2.30], [4.55, 0.70, 5.20], "gold", pivot=[8.53, 31.05, 0.30], rotation=[0, 0, 9]),
        # High connected collar ties head equipment to the dorsal shrine.
        c([-6.50, 30.20, 2.80], [13.00, 3.40, 1.60], "amethyst"),
        c([-5.30, 33.00, 3.02], [10.60, 0.65, 1.12], "gold"),
        # Large left sentinel tower and lower right counter-mass stay rooted in pauldrons.
        c([-10.60, 30.15, 1.55], [2.45, 5.45, 2.65], "amethyst", pivot=[-9.38, 32.88, 2.88], rotation=[0, 0, -7]),
        c([8.15, 29.45, 1.40], [2.65, 3.25, 2.65], "indigo", pivot=[9.48, 31.08, 2.73], rotation=[0, 0, 8]),
        c([-1.40, 28.05, 3.55], [2.80, 3.20, 0.30], "aura"),
    ]}


def breastplate() -> dict:
    # Keep the broad v2 cuirass; it already reads as one connected chest mass.
    return refined.breastplate()


def shrine_frame() -> dict:
    c = base.cube
    return {"name": "ouros_aura_shrine_frame", "parent": "torso3", "pivot": [0, 31.0, 3.5], "cubes": [
        # Thick lower roots overlap the mantle/back envelope. No freestanding halo posts.
        c([-7.60, 25.00, 2.90], [3.10, 10.00, 1.80], "void"),
        c([4.50, 25.00, 2.90], [3.10, 10.00, 1.80], "void"),
        c([-7.60, 34.20, 2.90], [15.20, 2.30, 1.80], "indigo"),
        c([-6.80, 24.20, 3.00], [13.60, 2.00, 1.55], "indigo"),
        c([-5.00, 29.00, 3.35], [10.00, 2.00, 1.30], "cobalt"),
        # Side architecture is made of broad plates attached directly to the roots.
        c([-10.60, 30.35, 2.75], [4.20, 4.90, 1.85], "amethyst", pivot=[-8.50, 32.80, 3.68], rotation=[0, 0, -14]),
        c([6.35, 31.00, 2.78], [3.45, 3.95, 1.75], "cobalt", pivot=[8.08, 32.98, 3.66], rotation=[0, 0, 12]),
        c([-9.85, 33.65, 2.82], [3.45, 2.80, 1.72], "gold", pivot=[-8.13, 35.05, 3.68], rotation=[0, 0, -10]),
        c([6.55, 33.85, 2.82], [3.10, 2.55, 1.70], "silver", pivot=[8.10, 35.13, 3.67], rotation=[0, 0, 10]),
        # Central reliquary is embedded into the top beam.
        c([-1.55, 34.55, 2.55], [3.10, 3.10, 0.60], "void", pivot=[0, 36.10, 2.85], rotation=[0, 0, 45]),
        c([-0.95, 35.15, 2.95], [1.90, 1.90, 0.26], "aura", pivot=[0, 36.10, 3.08], rotation=[0, 0, 45]),
        c([-1.10, 25.70, 4.30], [2.20, 7.80, 0.28], "aura"),
    ]}


def waistcoat() -> dict:
    # The refined split coat already has broad belt roots; keep it as the lower torso mass.
    bone = refined.waistcoat()
    c = base.cube
    bone["cubes"].extend([
        c([-5.65, 18.60, 2.85], [5.35, 2.10, 1.45], "indigo"),
        c([0.30, 18.60, 2.85], [5.35, 2.10, 1.45], "amethyst"),
        c([-4.95, 18.95, 4.02], [9.90, 0.42, 0.30], "gold"),
    ])
    return bone


def relic_fin() -> dict:
    c = base.cube
    return {"name": "ouros_aura_relic_fin", "parent": "torso3", "pivot": [-8.0, 31.0, 3.8], "cubes": [
        # One stepped asymmetric relic wing. Every outer plate overlaps the previous plate.
        c([-8.00, 27.40, 1.55], [3.80, 4.85, 2.85], "void"),
        c([-11.00, 27.90, 2.25], [4.30, 6.10, 2.05], "indigo", pivot=[-8.85, 30.95, 3.28], rotation=[0, 0, -12]),
        c([-13.55, 29.00, 2.45], [4.10, 4.75, 1.85], "amethyst", pivot=[-11.50, 31.38, 3.38], rotation=[0, 0, -18]),
        c([-15.55, 30.20, 2.65], [3.55, 3.35, 1.65], "cobalt", pivot=[-13.78, 31.88, 3.48], rotation=[0, 0, -24]),
        c([-12.10, 28.85, 4.05], [1.05, 5.10, 0.30], "gold", pivot=[-11.58, 31.40, 4.20], rotation=[0, 0, -16]),
        c([-14.15, 30.20, 4.18], [1.55, 2.45, 0.24], "aura", pivot=[-13.38, 31.43, 4.30], rotation=[0, 0, -22]),
    ]}


def greave(name: str, parent: str, left: bool) -> dict:
    c = base.cube
    if left:
        x = 1.10
        side_a, side_b = 1.00, 4.90
        accent = "cobalt"
        knee_pivot = [3.50, 6.20, -1.55]
    else:
        x = -5.90
        side_a, side_b = -6.00, -2.10
        accent = "amethyst"
        knee_pivot = [-3.50, 6.20, -1.55]
    return {"name": name, "parent": parent, "pivot": knee_pivot, "cubes": [
        # Shell overlaps the official shin volume and follows the official articulated leg bone.
        c([x, -2.40, -2.10], [4.80, 8.40, 1.10], "void"),
        c([side_a, -2.30, -1.55], [1.10, 8.20, 3.40], accent),
        c([side_b, -2.30, -1.55], [1.00, 8.20, 3.40], "indigo"),
        c([x + 0.20, 5.15, -2.20], [4.40, 2.05, 1.20], accent, pivot=knee_pivot, rotation=[-8, 0, 0]),
        c([x + 0.20, 4.75, -2.36], [4.40, 0.50, 0.28], "gold"),
        c([x + 1.00, 0.00, -2.48], [2.80, 2.80, 0.22], "aura"),
    ]}


def build_model(source: Path) -> tuple[dict, int]:
    data = json.loads(source.read_text(encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    original = len(geo["bones"])
    geo["description"]["identifier"] = "geometry.ouros_aura_sentinel_lucario"
    geo["bones"].extend([
        helm_system(),
        mantle_shell(),
        breastplate(),
        shrine_frame(),
        base.armguard("ouros_aura_left_armguard", "arm_left2", True),
        base.armguard("ouros_aura_right_armguard", "arm_right2", False),
        waistcoat(),
        relic_fin(),
        greave("ouros_aura_left_greave", "leg_left4", True),
        greave("ouros_aura_right_greave", "leg_right4", False),
    ])
    return data, original


# Preserve the exact official biological textures.
base.remap_texture = preserve_texture
base.texture_metadata = preserved_texture_metadata

# Explicitly replace every major presentation system used by base.main().
base.helm_system = helm_system
base.mantle_shell = mantle_shell
base.breastplate = breastplate
base.shrine_frame = shrine_frame
base.waistcoat = waistcoat
base.relic_fin = relic_fin
base.build_model = build_model

if __name__ == "__main__":
    base.main()
