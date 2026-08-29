#!/usr/bin/env python3
"""Render review PNGs using real Cobblemon Bedrock animation transforms.

This supplements render_bedrock_model.py with the bind-pose bone rotations and
animation position/rotation transforms needed by newer Cobblemon models. It is
intended for deterministic review evidence only; production still uses the real
Cobblemon poser and animation runtime.
"""

from __future__ import annotations

import argparse
import json
import math as pymath
from pathlib import Path

import numpy as np
from PIL import Image

import render_bedrock_model as base


class MolangMath:
    @staticmethod
    def sin(value):
        return pymath.sin(pymath.radians(float(value)))

    @staticmethod
    def cos(value):
        return pymath.cos(pymath.radians(float(value)))

    @staticmethod
    def tan(value):
        return pymath.tan(pymath.radians(float(value)))

    @staticmethod
    def abs(value):
        return abs(value)

    @staticmethod
    def clamp(value, lo, hi):
        return max(float(lo), min(float(hi), float(value)))

    @staticmethod
    def min(*values):
        return min(values)

    @staticmethod
    def max(*values):
        return max(values)

    @staticmethod
    def floor(value):
        return pymath.floor(value)

    @staticmethod
    def ceil(value):
        return pymath.ceil(value)

    @staticmethod
    def sqrt(value):
        return pymath.sqrt(value)


MATH = MolangMath()
ANIMATIONS = {}
POSES = {
    "standing": ("animation.pikachu.ground_idle", 0.0, 34.0, 5.0),
    "battle": ("animation.pikachu.battle_idle", 0.0, -30.0, 3.0),
    "walking": ("animation.pikachu.ground_walk", 0.14, 26.0, 1.0),
}
HIDDEN_ROOTS = {"mouth_open", "acting_teeth", "eyelid_left", "eyelid_right"}


def eval_scalar(value, anim_time):
    if isinstance(value, (int, float)):
        return float(value)
    if not isinstance(value, str):
        raise TypeError(f"Unsupported Molang scalar {value!r}")
    expression = value.replace("query.anim_time", "anim_time").replace("q.anim_time", "anim_time")
    return float(eval(expression, {"__builtins__": {}}, {"math": MATH, "anim_time": float(anim_time)}))


def eval_vector(value, anim_time):
    if isinstance(value, list):
        return [eval_scalar(v, anim_time) for v in value]
    if not isinstance(value, dict):
        raise TypeError(f"Unsupported animation vector {value!r}")

    numeric = sorted((float(k), k) for k in value.keys())
    if not numeric:
        return [0.0, 0.0, 0.0]
    before = [item for item in numeric if item[0] <= anim_time + 1e-9]
    key = before[-1][1] if before else numeric[0][1]
    chosen = value[key]
    if isinstance(chosen, dict):
        chosen = chosen.get("post", chosen.get("pre"))
    return eval_vector(chosen, anim_time)


def translation_matrix(offset):
    matrix = np.eye(4, dtype=float)
    matrix[:3, 3] = np.asarray(offset, dtype=float)
    return matrix


def add_vectors(a, b):
    return [float(a[i]) + float(b[i]) for i in range(3)]


def descendants(bones, roots):
    children = {}
    for bone in bones:
        children.setdefault(bone.get("parent"), []).append(bone.get("name"))
    hidden = set()
    stack = list(roots)
    while stack:
        name = stack.pop()
        if name in hidden:
            continue
        hidden.add(name)
        stack.extend(children.get(name, []))
    return hidden


def build_bone_matrices(bones, animation_name, anim_time):
    by_name = {bone.get("name", ""): bone for bone in bones}
    animation = ANIMATIONS[animation_name]
    animated = animation.get("bones", {})
    cache = {}
    active = set()

    def resolve(name):
        if name in cache:
            return cache[name]
        if name in active:
            raise ValueError(f"bone parent cycle at {name}")
        active.add(name)
        bone = by_name[name]
        parent_name = bone.get("parent")
        parent = resolve(parent_name) if parent_name in by_name else np.eye(4, dtype=float)

        bind_rotation = bone.get("rotation", [0.0, 0.0, 0.0])
        anim = animated.get(name, {})
        anim_rotation = eval_vector(anim.get("rotation", [0.0, 0.0, 0.0]), anim_time)
        anim_position = eval_vector(anim.get("position", [0.0, 0.0, 0.0]), anim_time)
        rotation = add_vectors(bind_rotation, anim_rotation)
        pivot = bone.get("pivot", [0.0, 0.0, 0.0])
        local = translation_matrix(anim_position) @ base.homogeneous_rotation_about(pivot, rotation)
        matrix = parent @ local
        cache[name] = matrix
        active.remove(name)
        return matrix

    for bone_name in by_name:
        resolve(bone_name)
    return cache


def build_faces(model, pose_name="standing"):
    animation_name, anim_time, _, _ = POSES[pose_name]
    geometry = model["minecraft:geometry"][0]
    bones = geometry.get("bones", [])
    matrices = build_bone_matrices(bones, animation_name, anim_time)
    hidden = descendants(bones, HIDDEN_ROOTS)
    faces = []
    for bone in bones:
        name = bone.get("name", "")
        if name in hidden:
            continue
        bone_matrix = matrices[name]
        for cube in bone.get("cubes", []):
            for face_name, verts, uvs, bone_name in base.cube_faces(cube, name):
                verts = base.transform_points(verts, bone_matrix)
                normal = np.cross(verts[1] - verts[0], verts[2] - verts[0])
                nlen = np.linalg.norm(normal)
                if nlen > 0:
                    normal /= nlen
                faces.append((face_name, verts, uvs, normal, bone_name))
    return geometry["description"], faces


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", required=True)
    parser.add_argument("--animation-file", required=True)
    parser.add_argument("--base-texture", required=True)
    parser.add_argument("--accessory-texture", required=True)
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--size", type=int, default=1024)
    args = parser.parse_args()

    global ANIMATIONS
    model = json.loads(Path(args.model).read_text(encoding="utf-8"))
    animation_doc = json.loads(Path(args.animation_file).read_text(encoding="utf-8"))
    ANIMATIONS = animation_doc["animations"]
    for name, (animation_name, _, _, _) in POSES.items():
        if animation_name not in ANIMATIONS:
            raise KeyError(f"Missing official animation for {name}: {animation_name}")

    base.build_faces = build_faces
    base_img = Image.open(args.base_texture)
    accessory_img = Image.open(args.accessory_texture)
    out = Path(args.output_dir)

    # Four review directions use Cobblemon's real non-battle standing state.
    for view in ("front", "left", "right", "back"):
        yaw, pitch = base.view_angles(view)
        path = out / f"{view}.png"
        base.render(model, base_img, accessory_img, path, args.size, pose="standing", yaw=yaw, pitch=pitch)
        print(path)

    for output_name, pose_name in (
        ("hero_three_quarter", "standing"),
        ("battle_ready", "battle"),
        ("walking", "walking"),
    ):
        _, _, yaw, pitch = POSES[pose_name]
        path = out / "poses" / f"{output_name}.png"
        base.render(model, base_img, accessory_img, path, args.size, pose=pose_name, yaw=yaw, pitch=pitch)
        print(path)


if __name__ == "__main__":
    main()
