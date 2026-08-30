#!/usr/bin/env python3
"""Aura Sentinel V5 authored-shape rework.

Starts from the exact current official Lucario geometry and preserves every
biological bone/UV byte-for-byte. V5 deliberately reduces the old box-scaffold
read: fewer macro pieces, more rotated/tapered-looking stepped planes, stronger
negative space, and one clear asymmetric shoulder/relic silhouette.

Official normal/shiny body textures remain byte-identical under the current
texture contract. Added colors live only on the validated accessory overlay.
"""
from __future__ import annotations

import importlib.util
import json
from pathlib import Path

PREV_PATH = Path(__file__).with_name("build_aura_sentinel_v3_current.py")
spec = importlib.util.spec_from_file_location("aura_v4_previous", PREV_PATH)
prev = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(prev)
base = prev.base


def helm_system() -> dict:
    c = base.cube
    # Open ceremonial half-helm. Ear/sensor silhouette stays exposed; the armor
    # follows the cheek/crown line instead of enclosing the head in a box.
    return {"name": "ouros_aura_helm_system", "parent": "head_angle", "pivot": [0, 38.2, -2.1], "cubes": [
        c([-5.10, 34.40, 1.70], [10.20, 2.00, 1.25], "void", pivot=[0,35.4,2.3], rotation=[8,0,0]),
        c([-5.70, 35.50, -2.80], [1.20, 4.10, 4.80], "indigo", pivot=[-5.1,37.2,-0.4], rotation=[0,0,-13]),
        c([4.50, 35.50, -2.80], [1.20, 4.10, 4.80], "cobalt", pivot=[5.1,37.2,-0.4], rotation=[0,0,13]),
        c([-4.75, 37.10, -5.18], [9.50, 0.75, 0.28], "silver", pivot=[0,37.48,-5.04], rotation=[-5,0,0]),
        c([-3.95, 37.55, -5.36], [7.90, 0.34, 0.18], "aura", pivot=[0,37.72,-5.27], rotation=[-5,0,0]),
        c([-7.70, 39.00, -1.25], [3.80, 1.30, 2.25], "amethyst", pivot=[-5.8,39.65,-0.12], rotation=[0,0,-24]),
        c([-9.05, 40.00, -0.95], [3.20, 0.85, 1.75], "cobalt", pivot=[-7.45,40.43,-0.08], rotation=[0,0,-34]),
        c([4.10, 38.95, -1.05], [2.65, 0.85, 1.85], "gold", pivot=[5.43,39.38,-0.12], rotation=[0,0,18]),
    ]}


def mantle_shell() -> dict:
    c = base.cube
    # Crescent yoke: overlapping rotated shoulder plates create a descending
    # contour rather than one rectangular slab across the torso.
    return {"name": "ouros_aura_mantle_shell", "parent": "torso3", "pivot": [0, 29.2, 0.0], "cubes": [
        c([-5.90, 29.00, -2.90], [11.80, 2.00, 5.70], "void", pivot=[0,30.0,-0.05], rotation=[4,0,0]),
        c([-10.80, 28.70, -2.55], [6.10, 2.55, 5.00], "indigo", pivot=[-7.75,29.98,-0.05], rotation=[0,0,-18]),
        c([-13.10, 30.35, -1.70], [5.00, 1.65, 3.50], "amethyst", pivot=[-10.60,31.18,0.05], rotation=[0,0,-31]),
        c([4.55, 28.95, -2.30], [5.10, 2.10, 4.60], "cobalt", pivot=[7.10,30.0,0], rotation=[0,0,13]),
        c([7.60, 30.30, -1.55], [3.60, 1.15, 3.20], "gold", pivot=[9.40,30.88,0.05], rotation=[0,0,25]),
        c([-4.90, 31.00, 2.55], [9.80, 2.10, 1.35], "amethyst", pivot=[0,32.05,3.23], rotation=[-8,0,0]),
        c([-2.90, 32.65, 3.15], [5.80, 0.40, 0.75], "aura", pivot=[0,32.85,3.53], rotation=[-8,0,0]),
    ]}


def breastplate() -> dict:
    c = base.cube
    # Layered V-shaped cuirass. Central chest stays open around Lucario's spike;
    # the outer plates angle inward and leave deliberate negative space.
    return {"name": "ouros_aura_breastplate", "parent": "torso3", "pivot": [0, 27.7, -3.4], "cubes": [
        c([-4.75, 28.10, -4.20], [4.60, 1.65, 0.55], "indigo", pivot=[-2.45,28.93,-3.93], rotation=[0,0,-16]),
        c([0.15, 28.10, -4.20], [4.60, 1.65, 0.55], "cobalt", pivot=[2.45,28.93,-3.93], rotation=[0,0,16]),
        c([-4.20, 25.35, -4.15], [3.60, 3.60, 0.52], "void", pivot=[-2.40,27.15,-3.89], rotation=[0,0,-10]),
        c([0.60, 25.35, -4.15], [3.60, 3.60, 0.52], "void", pivot=[2.40,27.15,-3.89], rotation=[0,0,10]),
        c([-3.25, 24.55, -4.32], [2.70, 1.05, 0.32], "gold", pivot=[-1.90,25.08,-4.16], rotation=[0,0,-20]),
        c([0.55, 24.55, -4.32], [2.70, 1.05, 0.32], "silver", pivot=[1.90,25.08,-4.16], rotation=[0,0,20]),
        c([-0.55, 26.65, -4.58], [1.10, 1.10, 0.22], "aura", pivot=[0,27.20,-4.47], rotation=[0,0,45]),
    ]}


def shrine_frame() -> dict:
    c = base.cube
    # Compact dorsal fan/reliquary. It grows from a broad back root, then splits
    # into rotated leaves; there is no rectangular halo/portal frame.
    return {"name": "ouros_aura_shrine_frame", "parent": "torso3", "pivot": [0, 31.0, 3.4], "cubes": [
        c([-5.80, 25.10, 2.85], [11.60, 2.70, 1.65], "void", pivot=[0,26.45,3.68], rotation=[-8,0,0]),
        c([-4.60, 27.00, 3.20], [3.50, 7.20, 1.20], "indigo", pivot=[-2.85,30.6,3.80], rotation=[-4,0,-15]),
        c([1.10, 27.00, 3.20], [3.50, 7.20, 1.20], "cobalt", pivot=[2.85,30.6,3.80], rotation=[-4,0,15]),
        c([-7.50, 31.70, 3.25], [4.80, 1.35, 1.10], "amethyst", pivot=[-5.10,32.38,3.80], rotation=[0,0,-38]),
        c([2.70, 31.70, 3.25], [4.80, 1.35, 1.10], "gold", pivot=[5.10,32.38,3.80], rotation=[0,0,38]),
        c([-1.35, 33.10, 3.15], [2.70, 2.70, 0.70], "void", pivot=[0,34.45,3.50], rotation=[0,0,45]),
        c([-0.78, 33.67, 3.62], [1.56, 1.56, 0.22], "aura", pivot=[0,34.45,3.73], rotation=[0,0,45]),
    ]}


def armguard(name: str, parent: str, left: bool) -> dict:
    c = base.cube
    s = 1 if left else -1
    # Blade-like bracer follows forearm; asymmetric colored ridge faces outward.
    x0 = 0.65 if left else -4.65
    pivot = [2.65 if left else -2.65, 18.3, -0.2]
    rot = -8 if left else 8
    return {"name": name, "parent": parent, "pivot": pivot, "cubes": [
        c([x0, 15.10, -2.00], [4.00, 5.80, 1.00], "void", pivot=pivot, rotation=[0,0,rot]),
        c([x0 + (0.15 if left else -0.15), 19.60, -1.72], [3.70, 1.25, 2.60], "cobalt" if left else "amethyst", pivot=pivot, rotation=[0,0,rot]),
        c([x0 + (2.85 if left else 0.10), 16.15, -1.35], [0.75, 4.40, 2.40], "indigo", pivot=pivot, rotation=[0,0,rot + (-7 if left else 7)]),
        c([x0 + 0.75, 17.30, -2.18], [2.50, 0.32, 0.22], "aura", pivot=pivot, rotation=[0,0,rot]),
    ]}


def waistcoat() -> dict:
    c = base.cube
    # Belt root plus three overlapping coat leaves. Their different lengths and
    # angles stop the lower silhouette from becoming a rectangular skirt.
    return {"name": "ouros_aura_waistcoat", "parent": "torso", "pivot": [0, 20.2, 1.0], "cubes": [
        c([-5.80, 19.20, -3.50], [11.60, 1.55, 7.00], "void", pivot=[0,19.98,0], rotation=[0,0,0]),
        c([-5.20, 11.80, 2.95], [4.50, 8.20, 0.85], "indigo", pivot=[-2.95,19.1,3.38], rotation=[-8,0,10]),
        c([0.35, 13.00, 3.00], [4.30, 7.00, 0.85], "amethyst", pivot=[2.50,19.15,3.43], rotation=[-8,0,-8]),
        c([-6.15, 14.10, -3.62], [2.10, 5.60, 0.52], "cobalt", pivot=[-5.10,19.0,-3.36], rotation=[0,0,-8]),
        c([4.05, 14.80, -3.62], [2.10, 4.90, 0.52], "gold", pivot=[5.10,19.0,-3.36], rotation=[0,0,8]),
        c([-1.90, 19.65, 3.65], [3.80, 0.35, 0.24], "aura"),
    ]}


def relic_fin() -> dict:
    c = base.cube
    # Dominant left relic wing: a rooted sequence of overlapping wedges built
    # from successively smaller rotated plates, not an external frame.
    return {"name": "ouros_aura_relic_fin", "parent": "torso3", "pivot": [-8.2, 30.5, 2.8], "cubes": [
        c([-9.20, 27.50, 1.55], [3.80, 4.80, 2.45], "void", pivot=[-7.3,29.9,2.78], rotation=[0,0,-14]),
        c([-12.40, 28.40, 2.10], [5.00, 3.10, 1.75], "indigo", pivot=[-9.9,29.95,2.98], rotation=[0,0,-27]),
        c([-15.20, 29.70, 2.35], [4.60, 2.40, 1.55], "amethyst", pivot=[-12.9,30.9,3.13], rotation=[0,0,-38]),
        c([-17.20, 31.20, 2.55], [3.80, 1.75, 1.35], "cobalt", pivot=[-15.3,32.08,3.23], rotation=[0,0,-49]),
        c([-13.30, 30.05, 3.86], [2.85, 0.40, 0.24], "gold", pivot=[-11.88,30.25,3.98], rotation=[0,0,-32]),
        c([-15.60, 31.60, 3.95], [2.55, 0.34, 0.22], "aura", pivot=[-14.33,31.77,4.06], rotation=[0,0,-46]),
    ]}


def greave(name: str, parent: str, left: bool) -> dict:
    c = base.cube
    x = 1.35 if left else -5.65
    pivot = [3.5 if left else -3.5, 6.1, -1.4]
    sign = -1 if left else 1
    accent = "cobalt" if left else "amethyst"
    return {"name": name, "parent": parent, "pivot": pivot, "cubes": [
        c([x, -1.80, -2.05], [4.30, 6.80, 0.90], "void", pivot=pivot, rotation=[-5,0,sign*4]),
        c([x + 0.25, 4.55, -2.15], [3.80, 1.55, 1.15], accent, pivot=pivot, rotation=[-12,0,sign*9]),
        c([x + (3.35 if left else -0.30), 0.10, -1.55], [0.85, 5.20, 2.65], "indigo", pivot=pivot, rotation=[0,0,sign*7]),
        c([x + 0.55, -2.05, -1.65], [3.15, 1.20, 2.10], "gold" if left else "silver", pivot=pivot, rotation=[8,0,sign*5]),
        c([x + 0.95, 2.20, -2.28], [2.35, 0.34, 0.20], "aura", pivot=pivot, rotation=[-5,0,sign*4]),
    ]}


def build_model(source: Path) -> tuple[dict, int]:
    data = json.loads(source.read_text(encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    original = len(geo["bones"])
    geo["description"]["identifier"] = "geometry.ouros_aura_sentinel_lucario"
    geo["bones"].extend([
        helm_system(), mantle_shell(), breastplate(), shrine_frame(),
        armguard("ouros_aura_left_armguard", "arm_left2", True),
        armguard("ouros_aura_right_armguard", "arm_right2", False),
        waistcoat(), relic_fin(),
        greave("ouros_aura_left_greave", "leg_left4", True),
        greave("ouros_aura_right_greave", "leg_right4", False),
    ])
    return data, original


# Keep V4's strict texture-preservation pipeline and output plumbing.
base.helm_system = helm_system
base.mantle_shell = mantle_shell
base.breastplate = breastplate
base.shrine_frame = shrine_frame
base.armguard = armguard
base.waistcoat = waistcoat
base.relic_fin = relic_fin
base.build_model = build_model

if __name__ == "__main__":
    base.main()
