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


def _professional_v2_cosmetics() -> list[dict]:
    """Second authored pass: one continuous rear mantle, open chest, quiet limbs."""
    c = impl.cube
    return [
        {
            "name": "ouros_aura_circlet",
            "parent": "head_angle",
            "pivot": [0, 38.0, -1.8],
            "cubes": [
                c([-4.7, 37.0, -3.7], [3.2, 0.8, 0.65], "lacquer", pivot=[-3.1, 37.4, -3.35], rotation=[0, -6, -18]),
                c([1.5, 37.0, -3.7], [3.2, 0.8, 0.65], "lacquer", pivot=[3.1, 37.4, -3.35], rotation=[0, 6, 18]),
                c([-4.4, 38.1, -1.4], [1.25, 2.8, 1.8], "gold", pivot=[-3.8, 38.4, -0.5], rotation=[-8, -6, -24]),
            ],
        },
        {
            "name": "ouros_aura_left_pauldron",
            "parent": "shoulder_right",
            "pivot": [-3.2, 30.0, -0.3],
            "cubes": [
                c([-8.6, 28.8, -2.7], [5.8, 2.4, 5.1], "lacquer", pivot=[-5.7, 30.0, -0.15], rotation=[2, -8, -18]),
                c([-9.4, 27.5, -2.2], [5.1, 1.9, 4.5], "cloth", pivot=[-6.8, 28.5, 0.0], rotation=[5, -12, -31]),
                c([-8.7, 30.45, -2.85], [4.7, 0.45, 4.6], "gold", pivot=[-6.35, 30.7, -0.55], rotation=[2, -8, -18]),
            ],
        },
        {
            "name": "ouros_aura_mantle_root",
            "parent": "torso3",
            "pivot": [-3.8, 29.5, 2.7],
            "cubes": [
                c([-7.2, 27.8, 1.7], [8.6, 2.5, 2.2], "cloth", pivot=[-3.0, 29.0, 2.8], rotation=[-9, -3, -12]),
                c([-8.0, 25.5, 2.2], [8.2, 2.4, 2.0], "cloth", pivot=[-3.9, 26.7, 3.2], rotation=[-12, -4, -20]),
                c([-7.6, 23.1, 2.65], [7.4, 2.3, 1.8], "lacquer", pivot=[-3.9, 24.3, 3.55], rotation=[-15, -4, -28]),
                c([-6.9, 21.0, 2.95], [6.5, 1.9, 1.6], "cloth", pivot=[-3.7, 22.0, 3.75], rotation=[-18, -4, -35]),
            ],
        },
        {
            "name": "ouros_aura_mantle_fall",
            "parent": "torso",
            "pivot": [-3.1, 20.0, 3.5],
            "cubes": [
                c([-6.2, 19.0, 2.9], [5.9, 2.0, 1.55], "cloth", pivot=[-3.3, 20.0, 3.65], rotation=[-18, -4, -31]),
                c([-5.9, 16.8, 3.15], [5.3, 2.0, 1.4], "cloth", pivot=[-3.3, 17.8, 3.85], rotation=[-20, -4, -37]),
                c([-5.5, 14.6, 3.35], [4.7, 1.9, 1.25], "lacquer", pivot=[-3.2, 15.5, 3.95], rotation=[-22, -4, -43]),
                c([-4.9, 12.6, 3.5], [4.0, 1.7, 1.1], "cloth", pivot=[-2.9, 13.45, 4.05], rotation=[-24, -4, -49]),
                c([-4.2, 10.9, 3.65], [3.3, 1.45, 0.95], "gold", pivot=[-2.55, 11.6, 4.1], rotation=[-26, -4, -55]),
            ],
        },
        {
            "name": "ouros_aura_split_cuirass",
            "parent": "torso3",
            "pivot": [0, 28.0, -3.8],
            "cubes": [
                c([-4.6, 28.1, -4.45], [4.9, 1.65, 0.62], "lacquer", pivot=[-2.2, 28.9, -4.14], rotation=[0, -2, -31]),
                c([0.0, 28.0, -4.42], [4.5, 1.55, 0.60], "cloth", pivot=[2.25, 28.8, -4.12], rotation=[0, 2, 29]),
                c([-3.6, 25.5, -4.48], [3.9, 1.45, 0.58], "cloth", pivot=[-1.65, 26.2, -4.18], rotation=[0, -2, -42]),
                c([0.0, 25.6, -4.46], [3.6, 1.35, 0.56], "lacquer", pivot=[1.8, 26.25, -4.18], rotation=[0, 2, 40]),
                c([-0.85, 27.0, -4.76], [1.7, 1.7, 0.28], "aura", pivot=[0, 27.85, -4.62], rotation=[0, 0, 45]),
            ],
        },
        {
            "name": "ouros_aura_left_vambrace",
            "parent": "arm_left2",
            "pivot": [10.6, 29.5, -0.4],
            "cubes": [
                c([9.1, 28.1, -2.15], [3.8, 1.15, 3.1], "lacquer", pivot=[11.0, 28.7, -0.6], rotation=[-6, 0, -8]),
                c([10.0, 30.1, -1.95], [2.6, 0.75, 2.4], "gold", pivot=[11.3, 30.5, -0.75], rotation=[-8, 0, -5]),
            ],
        },
        {
            "name": "ouros_aura_right_vambrace",
            "parent": "arm_right2",
            "pivot": [-10.6, 29.5, -0.4],
            "cubes": [
                c([-12.9, 28.1, -2.15], [3.8, 1.15, 3.1], "lacquer", pivot=[-11.0, 28.7, -0.6], rotation=[-6, 0, 8]),
                c([-12.6, 30.1, -1.95], [2.6, 0.75, 2.4], "gold", pivot=[-11.3, 30.5, -0.75], rotation=[-8, 0, 5]),
            ],
        },
        {
            "name": "ouros_aura_waist_tabard",
            "parent": "torso",
            "pivot": [-1.1, 19.4, 2.7],
            "cubes": [
                c([-5.4, 18.5, 1.7], [8.3, 1.35, 2.35], "lacquer", pivot=[-1.3, 19.2, 2.9], rotation=[-7, 0, -10]),
                c([-4.9, 15.8, 2.45], [6.5, 1.6, 1.8], "cloth", pivot=[-1.7, 16.6, 3.35], rotation=[-11, 0, -19]),
                c([-4.1, 13.6, 2.95], [5.0, 1.35, 1.5], "gold", pivot=[-1.6, 14.3, 3.7], rotation=[-14, 0, -27]),
            ],
        },
        {
            "name": "ouros_aura_left_greave",
            "parent": "leg_left4",
            "pivot": [3.4, 4.0, -1.2],
            "cubes": [
                c([1.25, 0.1, -2.05], [4.25, 1.25, 2.65], "lacquer", pivot=[3.4, 0.8, -0.7], rotation=[-8, 0, -7]),
                c([1.8, 4.4, -2.1], [3.2, 0.85, 2.2], "gold", pivot=[3.4, 4.8, -1.0], rotation=[-11, 0, -4]),
            ],
        },
        {
            "name": "ouros_aura_right_greave",
            "parent": "leg_right4",
            "pivot": [-3.4, 4.0, -1.2],
            "cubes": [
                c([-5.5, 0.1, -2.05], [4.25, 1.25, 2.65], "lacquer", pivot=[-3.4, 0.8, -0.7], rotation=[-8, 0, 7]),
                c([-5.0, 4.4, -2.1], [3.2, 0.85, 2.2], "gold", pivot=[-3.4, 4.8, -1.0], rotation=[-11, 0, 4]),
            ],
        },
    ]


impl.cosmetics = _professional_v2_cosmetics

if __name__ == "__main__":
    impl.main()
