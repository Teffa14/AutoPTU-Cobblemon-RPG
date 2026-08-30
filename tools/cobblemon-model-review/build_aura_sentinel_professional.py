#!/usr/bin/env python3
"""Stable entrypoint for the Aura Sentinel professional builder."""
from __future__ import annotations
import hashlib
import importlib.util
from pathlib import Path

IMPL = Path(__file__).with_name("_aura_sentinel_professional_impl.py")
spec = importlib.util.spec_from_file_location("aura_sentinel_professional_impl", IMPL)
impl = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(impl)

_original_find = impl.find_path_by_hash

def _find_official_asset(zf, expected: str) -> str:
    if expected == impl.AUX_HASHES["MODEL_LICENSE"]:
        path = "assets/cobblemon/bedrock/pokemon/models/0448_lucario/license"
        actual = hashlib.sha256(zf.read(path)).hexdigest()
        if actual != expected:
            raise SystemExit(f"official Lucario model-license drift: expected={expected} actual={actual}")
        return path
    return _original_find(zf, expected)

impl.find_path_by_hash = _find_official_asset


def _professional_v3_cosmetics() -> list[dict]:
    """Broad-cloak pass: large connected masses, open anatomy and quiet supports."""
    c = impl.cube
    return [
        {
            "name": "ouros_aura_cowl",
            "parent": "head_angle",
            "pivot": [-1.5, 37.0, 1.8],
            "cubes": [
                c([-5.3, 34.8, 1.5], [4.5, 4.6, 2.0], "cloth", pivot=[-3.0, 37.1, 2.5], rotation=[-7, -5, -12]),
                c([0.6, 35.1, 1.5], [4.0, 4.1, 1.8], "cloth", pivot=[2.6, 37.1, 2.4], rotation=[-7, 5, 10]),
                c([-4.7, 37.2, -3.4], [3.0, 0.7, 0.55], "gold", pivot=[-3.2, 37.5, -3.1], rotation=[0, -5, -16]),
            ],
        },
        {
            "name": "ouros_aura_mantle_clasp",
            "parent": "shoulder_right",
            "pivot": [-4.0, 30.0, 0.0],
            "cubes": [
                c([-9.2, 28.0, -2.5], [6.6, 3.6, 5.4], "lacquer", pivot=[-5.9, 29.8, 0.2], rotation=[3, -10, -20]),
                c([-9.8, 26.6, -1.8], [5.9, 2.8, 4.8], "cloth", pivot=[-6.8, 28.0, 0.6], rotation=[6, -13, -32]),
                c([-8.9, 30.2, -2.7], [4.8, 0.5, 4.5], "gold", pivot=[-6.5, 30.5, -0.4], rotation=[3, -10, -20]),
            ],
        },
        {
            "name": "ouros_aura_sweeping_cloak",
            "parent": "torso3",
            "pivot": [-4.0, 27.5, 3.2],
            "cubes": [
                c([-10.4, 26.2, 2.0], [11.0, 4.2, 1.75], "cloth", pivot=[-4.9, 28.3, 2.9], rotation=[-10, -4, -13]),
                c([-11.2, 22.6, 2.45], [10.5, 4.1, 1.5], "cloth", pivot=[-5.9, 24.7, 3.2], rotation=[-13, -4, -22]),
                c([-10.9, 19.0, 2.8], [9.7, 4.0, 1.3], "lacquer", pivot=[-6.0, 21.0, 3.45], rotation=[-16, -4, -31]),
                c([-10.0, 15.7, 3.05], [8.5, 3.8, 1.1], "cloth", pivot=[-5.7, 17.6, 3.6], rotation=[-19, -4, -40]),
                c([-8.7, 12.9, 3.25], [7.0, 3.3, 0.95], "cloth", pivot=[-5.2, 14.5, 3.72], rotation=[-22, -4, -49]),
                c([-7.2, 10.7, 3.4], [5.4, 2.7, 0.8], "gold", pivot=[-4.5, 12.0, 3.8], rotation=[-24, -4, -57]),
            ],
        },
        {
            "name": "ouros_aura_open_cuirass",
            "parent": "torso3",
            "pivot": [0, 27.8, -3.8],
            "cubes": [
                c([-5.0, 27.1, -4.45], [5.4, 2.5, 0.72], "lacquer", pivot=[-2.3, 28.4, -4.1], rotation=[0, -2, -29]),
                c([-0.2, 27.0, -4.42], [5.1, 2.4, 0.70], "cloth", pivot=[2.35, 28.2, -4.08], rotation=[0, 2, 27]),
                c([-0.95, 26.4, -4.76], [1.9, 1.9, 0.30], "aura", pivot=[0, 27.35, -4.6], rotation=[0, 0, 45]),
            ],
        },
        {
            "name": "ouros_aura_left_vambrace",
            "parent": "arm_left2",
            "pivot": [10.8, 29.3, -0.5],
            "cubes": [
                c([9.0, 28.0, -2.2], [4.0, 1.3, 3.2], "lacquer", pivot=[11.0, 28.7, -0.6], rotation=[-7, 0, -8]),
                c([10.1, 30.0, -2.0], [2.5, 0.7, 2.4], "gold", pivot=[11.35, 30.4, -0.8], rotation=[-9, 0, -5]),
            ],
        },
        {
            "name": "ouros_aura_right_vambrace",
            "parent": "arm_right2",
            "pivot": [-10.8, 29.3, -0.5],
            "cubes": [
                c([-13.0, 28.0, -2.2], [4.0, 1.3, 3.2], "lacquer", pivot=[-11.0, 28.7, -0.6], rotation=[-7, 0, 8]),
                c([-12.6, 30.0, -2.0], [2.5, 0.7, 2.4], "gold", pivot=[-11.35, 30.4, -0.8], rotation=[-9, 0, 5]),
            ],
        },
        {
            "name": "ouros_aura_waist_sash",
            "parent": "torso",
            "pivot": [-1.1, 19.0, 2.8],
            "cubes": [
                c([-5.5, 18.0, 1.7], [8.5, 1.55, 2.5], "lacquer", pivot=[-1.3, 18.8, 2.9], rotation=[-8, 0, -11]),
                c([-5.0, 15.0, 2.5], [6.8, 2.2, 1.8], "cloth", pivot=[-1.6, 16.1, 3.4], rotation=[-12, 0, -22]),
            ],
        },
        {
            "name": "ouros_aura_left_greave",
            "parent": "leg_left4",
            "pivot": [3.4, 4.0, -1.2],
            "cubes": [
                c([1.2, 0.1, -2.1], [4.3, 1.35, 2.7], "lacquer", pivot=[3.4, 0.8, -0.75], rotation=[-8, 0, -7]),
                c([1.8, 4.4, -2.1], [3.2, 0.85, 2.2], "gold", pivot=[3.4, 4.8, -1.0], rotation=[-11, 0, -4]),
            ],
        },
        {
            "name": "ouros_aura_right_greave",
            "parent": "leg_right4",
            "pivot": [-3.4, 4.0, -1.2],
            "cubes": [
                c([-5.5, 0.1, -2.1], [4.3, 1.35, 2.7], "lacquer", pivot=[-3.4, 0.8, -0.75], rotation=[-8, 0, 7]),
                c([-5.0, 4.4, -2.1], [3.2, 0.85, 2.2], "gold", pivot=[-3.4, 4.8, -1.0], rotation=[-11, 0, 4]),
            ],
        },
    ]


impl.cosmetics = _professional_v3_cosmetics

if __name__ == "__main__":
    impl.main()
