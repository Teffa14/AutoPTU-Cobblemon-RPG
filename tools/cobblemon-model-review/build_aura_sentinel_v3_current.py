#!/usr/bin/env python3
"""Aura Sentinel v3 correction pass.

The geometry source is still supplied by the generation workflow from the exact
pinned official Cobblemon JAR. This wrapper deliberately keeps the official
normal/shiny body textures byte-identical, then strengthens physical joins in
large cosmetic masses so the design does not read as detached floating pieces.
"""
from __future__ import annotations

import importlib.util
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
        "paletteIntent": "Official Lucario biological texture is preserved exactly. Ouros colors are limited to validated free-texel accessory material swatches.",
        "materialIntent": "Transformation is carried by connected 3D armor, mantle, shrine-frame, coat and relic geometry rather than repainting Lucario anatomy.",
        "allowAlphaSemanticsChange": False,
    }


def helm_system() -> dict:
    bone = refined.helm_system()
    c = base.cube
    # Physical rear neck/cowl bridge. This deliberately overlaps the official
    # head/neck envelope and visually ties the temple crown into the body.
    bone["cubes"].extend([
        c([-4.85, 33.85, 1.65], [9.70, 1.25, 2.15], "void"),
        c([-3.90, 34.70, 2.75], [7.80, 0.55, 1.05], "gold"),
    ])
    return bone


def mantle_shell() -> dict:
    bone = refined.mantle_shell()
    c = base.cube
    # Shoulder-to-back yoke and central spine plate make the mantle a single
    # wearable mass instead of two disconnected pauldrons.
    bone["cubes"].extend([
        c([-6.85, 27.65, 2.35], [13.70, 2.15, 1.55], "void"),
        c([-2.10, 25.85, 3.20], [4.20, 4.25, 1.10], "indigo"),
        c([-1.25, 26.20, 4.20], [2.50, 3.30, 0.28], "aura"),
    ])
    return bone


def shrine_frame() -> dict:
    bone = refined.shrine_frame()
    c = base.cube
    # Root brackets overlap the mantle/collar envelope. The arch is no longer
    # allowed to exist as a visually detached halo behind Lucario.
    bone["cubes"].extend([
        c([-8.55, 27.20, 2.55], [3.55, 3.85, 1.85], "void", pivot=[-6.78, 29.12, 3.48], rotation=[0, 0, -8]),
        c([5.00, 27.20, 2.55], [3.55, 3.85, 1.85], "void", pivot=[6.78, 29.12, 3.48], rotation=[0, 0, 8]),
        c([-5.45, 29.10, 3.05], [10.90, 1.45, 1.75], "indigo"),
        c([-4.35, 30.05, 4.28], [8.70, 0.34, 0.30], "gold"),
    ])
    return bone


def waistcoat() -> dict:
    bone = refined.waistcoat()
    c = base.cube
    # Broad belt-to-coat roots keep both rear tails visibly attached through
    # torso motion instead of starting as isolated hanging panels.
    bone["cubes"].extend([
        c([-5.65, 18.60, 2.85], [5.35, 2.10, 1.45], "indigo"),
        c([0.30, 18.60, 2.85], [5.35, 2.10, 1.45], "amethyst"),
        c([-4.95, 18.95, 4.02], [9.90, 0.42, 0.30], "gold"),
    ])
    return bone


def relic_fin() -> dict:
    bone = refined.relic_fin()
    c = base.cube
    # A deep root plate physically joins the ceremonial fin to the left rear
    # mantle/shrine hardware. No freestanding banner/wing island.
    bone["cubes"].extend([
        c([-10.10, 26.40, 2.70], [5.25, 5.15, 1.75], "void", pivot=[-7.48, 28.98, 3.58], rotation=[0, 0, -10]),
        c([-9.45, 27.10, 4.18], [3.95, 3.70, 0.34], "gold", pivot=[-7.48, 28.95, 4.35], rotation=[0, 0, -10]),
    ])
    return bone


# Preserve the exact official biological textures.
base.remap_texture = preserve_texture
base.texture_metadata = preserved_texture_metadata

# Keep the large v2 artistic language but replace weak/floating joins.
base.helm_system = helm_system
base.mantle_shell = mantle_shell
base.shrine_frame = shrine_frame
base.waistcoat = waistcoat
base.relic_fin = relic_fin

if __name__ == "__main__":
    base.main()
