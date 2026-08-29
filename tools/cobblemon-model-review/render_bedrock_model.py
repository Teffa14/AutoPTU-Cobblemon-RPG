#!/usr/bin/env python3
"""Render deterministic PNG previews from Cobblemon Bedrock geometry.

This is a review renderer, not concept art and not generative imagery. It reads the
exact .geo.json and PNG textures used by Cobblemon, applies optional deterministic
review-pose transforms to the model's real bone hierarchy, and rasterizes the
result in software.

The named review poses are deliberately non-authoritative: they do not change the
production model or Cobblemon animation data. They exist only to let reviewers
inspect the exact model in more useful articulated poses before deciding whether
a cosmetic should ship.
"""

from __future__ import annotations

import argparse
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image

FACE_NAMES = ("north", "south", "east", "west", "up", "down")

# Rotations are applied to the existing named bones. No geometry is invented.
# Camera angles are orthographic yaw/pitch values in degrees.
POSES = {
    "neutral": {
        "camera": (0.0, 0.0),
        "rotations": {},
    },
    "hero_three_quarter": {
        "camera": (34.0, 5.0),
        "rotations": {
            "body": [-2.0, 0.0, -3.0],
            "torso": [-1.0, 0.0, 2.0],
            "head": [-4.0, -7.0, 2.0],
            "ear_left": [3.0, 0.0, -5.0],
            "ear_right": [-2.0, 0.0, 4.0],
            "arm_left": [-8.0, 0.0, -18.0],
            "arm_right": [7.0, 0.0, 14.0],
            "tail": [0.0, -5.0, 9.0],
        },
    },
    "battle_ready": {
        "camera": (-30.0, 3.0),
        "rotations": {
            "body": [-8.0, 0.0, 0.0],
            "torso": [-4.0, 5.0, 0.0],
            "head": [6.0, -8.0, 0.0],
            "ear_left": [-4.0, 0.0, -8.0],
            "ear_right": [-4.0, 0.0, 8.0],
            "arm_left": [-18.0, 0.0, -27.0],
            "arm_right": [-18.0, 0.0, 27.0],
            "leg_left": [8.0, 0.0, -4.0],
            "leg_right": [-7.0, 0.0, 5.0],
            "tail": [0.0, 8.0, 12.0],
        },
    },
    "walking": {
        "camera": (26.0, 1.0),
        "rotations": {
            "body": [-4.0, 0.0, -1.0],
            "torso": [2.0, 0.0, 0.0],
            "head": [2.0, 5.0, 1.0],
            "ear_left": [4.0, 0.0, -4.0],
            "ear_right": [-3.0, 0.0, 3.0],
            "arm_left": [28.0, 0.0, -5.0],
            "arm_right": [-28.0, 0.0, 5.0],
            "leg_left": [-24.0, 0.0, 0.0],
            "leg_right": [24.0, 0.0, 0.0],
            "foot_left": [8.0, 0.0, 0.0],
            "foot_right": [-6.0, 0.0, 0.0],
            "tail": [0.0, -5.0, 7.0],
        },
    },
}


def rotation_matrix_xyz(deg):
    rx, ry, rz = [math.radians(float(v)) for v in deg]
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    mx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]], dtype=float)
    my = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]], dtype=float)
    mz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]], dtype=float)
    return mz @ my @ mx


def homogeneous_rotation_about(pivot, rotation):
    matrix = np.eye(4, dtype=float)
    r = rotation_matrix_xyz(rotation)
    pivot = np.asarray(pivot, dtype=float)
    matrix[:3, :3] = r
    matrix[:3, 3] = pivot - r @ pivot
    return matrix


def transform_points(points, matrix):
    ones = np.ones((len(points), 1), dtype=float)
    hom = np.concatenate([points, ones], axis=1)
    return (hom @ matrix.T)[:, :3]


def rotate_points(points, pivot, rotation):
    if not rotation:
        return points
    return transform_points(points, homogeneous_rotation_about(pivot, rotation))


def default_uv_rects(uv, size):
    u, v = [float(x) for x in uv]
    dx, dy, dz = [float(x) for x in size]
    return {
        "west": (u, v + dz, dz, dy),
        "north": (u + dz, v + dz, dx, dy),
        "east": (u + dz + dx, v + dz, dz, dy),
        "south": (u + dz + dx + dz, v + dz, dx, dy),
        "up": (u + dz, v, dx, dz),
        "down": (u + dz + dx, v, dx, dz),
    }


def explicit_uv_rect(face):
    uv = face.get("uv", [0, 0])
    size = face.get("uv_size", [1, 1])
    return float(uv[0]), float(uv[1]), float(size[0]), float(size[1])


def face_uvs(rect, mirror=False):
    u, v, w, h = rect
    corners = np.array([[u, v], [u + w, v], [u + w, v + h], [u, v + h]], dtype=float)
    if mirror:
        corners = corners[[1, 0, 3, 2]]
    return corners


def cube_faces(cube, bone_name):
    origin = np.asarray(cube.get("origin", [0, 0, 0]), dtype=float)
    size = np.asarray(cube.get("size", [0, 0, 0]), dtype=float)
    inflate = float(cube.get("inflate", 0.0) or 0.0)
    lo = origin.copy()
    hi = origin + size
    for axis in range(3):
        if abs(size[axis]) > 1e-9:
            lo[axis] -= inflate
            hi[axis] += inflate
    x0, y0, z0 = lo
    x1, y1, z1 = hi

    quads = {
        "north": np.array([[x1, y0, z0], [x0, y0, z0], [x0, y1, z0], [x1, y1, z0]], dtype=float),
        "south": np.array([[x0, y0, z1], [x1, y0, z1], [x1, y1, z1], [x0, y1, z1]], dtype=float),
        "east": np.array([[x1, y0, z1], [x1, y0, z0], [x1, y1, z0], [x1, y1, z1]], dtype=float),
        "west": np.array([[x0, y0, z0], [x0, y0, z1], [x0, y1, z1], [x0, y1, z0]], dtype=float),
        "up": np.array([[x0, y1, z1], [x1, y1, z1], [x1, y1, z0], [x0, y1, z0]], dtype=float),
        "down": np.array([[x0, y0, z0], [x1, y0, z0], [x1, y0, z1], [x0, y0, z1]], dtype=float),
    }

    uv_spec = cube.get("uv", [0, 0])
    if isinstance(uv_spec, dict):
        uv_rects = {name: explicit_uv_rect(uv_spec.get(name, {})) for name in FACE_NAMES}
    else:
        uv_rects = default_uv_rects(uv_spec, size)

    pivot = cube.get("pivot")
    rotation = cube.get("rotation")
    mirror = bool(cube.get("mirror", False))
    result = []
    for face_name, verts in quads.items():
        a = verts[1] - verts[0]
        b = verts[2] - verts[0]
        area = np.linalg.norm(np.cross(a, b))
        if area < 1e-8:
            continue
        if rotation:
            verts = rotate_points(verts, pivot or [0, 0, 0], rotation)
        result.append((face_name, verts, face_uvs(uv_rects[face_name], mirror), bone_name))
    return result


def build_bone_matrices(bones, pose_name):
    by_name = {bone.get("name", ""): bone for bone in bones}
    rotations = POSES[pose_name]["rotations"]
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
        rotation = rotations.get(name, [0.0, 0.0, 0.0])
        local = homogeneous_rotation_about(bone.get("pivot", [0, 0, 0]), rotation)
        matrix = parent @ local
        cache[name] = matrix
        active.remove(name)
        return matrix

    for bone_name in by_name:
        resolve(bone_name)
    return cache


def build_faces(model, pose_name="neutral"):
    geometry = model["minecraft:geometry"][0]
    bones = geometry.get("bones", [])
    matrices = build_bone_matrices(bones, pose_name)
    faces = []
    for bone in bones:
        name = bone.get("name", "")
        bone_matrix = matrices[name]
        for cube in bone.get("cubes", []):
            for face_name, verts, uvs, bone_name in cube_faces(cube, name):
                verts = transform_points(verts, bone_matrix)
                normal = np.cross(verts[1] - verts[0], verts[2] - verts[0])
                nlen = np.linalg.norm(normal)
                if nlen > 0:
                    normal /= nlen
                faces.append((face_name, verts, uvs, normal, bone_name))
    return geometry["description"], faces


def camera_basis(yaw_deg=0.0, pitch_deg=0.0):
    yaw = math.radians(float(yaw_deg))
    pitch = math.radians(float(pitch_deg))
    depth = np.array(
        [math.sin(yaw) * math.cos(pitch), -math.sin(pitch), math.cos(yaw) * math.cos(pitch)],
        dtype=float,
    )
    depth /= np.linalg.norm(depth)
    up_world = np.array([0.0, 1.0, 0.0], dtype=float)
    right = np.cross(up_world, depth)
    right /= np.linalg.norm(right)
    up = np.cross(depth, right)
    up /= np.linalg.norm(up)
    return right, up, depth


def view_angles(view):
    return {
        "front": (0.0, 0.0),
        "left": (90.0, 0.0),
        "right": (-90.0, 0.0),
        "back": (180.0, 0.0),
    }[view]


def project(points, basis):
    right, up, depth = basis
    return np.stack([points @ right, points @ up, points @ depth], axis=1)


def triangle_bbox(p, width, height):
    xmin = max(0, int(math.floor(float(np.min(p[:, 0])))))
    xmax = min(width - 1, int(math.ceil(float(np.max(p[:, 0])))))
    ymin = max(0, int(math.floor(float(np.min(p[:, 1])))))
    ymax = min(height - 1, int(math.ceil(float(np.max(p[:, 1])))))
    return xmin, xmax, ymin, ymax


def raster_triangle(canvas, zbuf, pts, uvs, texture, tex_w, tex_h, shade):
    h, w, _ = canvas.shape
    xmin, xmax, ymin, ymax = triangle_bbox(pts, w, h)
    if xmin > xmax or ymin > ymax:
        return
    x0, y0 = pts[0, :2]
    x1, y1 = pts[1, :2]
    x2, y2 = pts[2, :2]
    denom = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
    if abs(denom) < 1e-9:
        return
    for py in range(ymin, ymax + 1):
        y = py + 0.5
        for px in range(xmin, xmax + 1):
            x = px + 0.5
            a = ((y1 - y2) * (x - x2) + (x2 - x1) * (y - y2)) / denom
            b = ((y2 - y0) * (x - x2) + (x0 - x2) * (y - y2)) / denom
            c = 1.0 - a - b
            if a < -1e-6 or b < -1e-6 or c < -1e-6:
                continue
            z = a * pts[0, 2] + b * pts[1, 2] + c * pts[2, 2]
            if z >= zbuf[py, px]:
                continue
            uv = a * uvs[0] + b * uvs[1] + c * uvs[2]
            tx = int(math.floor(uv[0])) % tex_w
            ty = int(math.floor(uv[1])) % tex_h
            rgba = texture[ty, tx].astype(float)
            if rgba[3] <= 0:
                continue
            rgba[:3] = np.clip(rgba[:3] * shade, 0, 255)
            alpha = rgba[3] / 255.0
            if alpha >= 0.999:
                canvas[py, px] = rgba.astype(np.uint8)
            else:
                dst = canvas[py, px].astype(float)
                out_a = alpha + (dst[3] / 255.0) * (1 - alpha)
                if out_a <= 1e-9:
                    continue
                out_rgb = (rgba[:3] * alpha + dst[:3] * (dst[3] / 255.0) * (1 - alpha)) / out_a
                canvas[py, px, :3] = np.clip(out_rgb, 0, 255).astype(np.uint8)
                canvas[py, px, 3] = int(round(out_a * 255))
            zbuf[py, px] = z


def render(model, base_img, accessory_img, output, size=1024, *, pose="neutral", yaw=0.0, pitch=0.0):
    _, faces = build_faces(model, pose)
    basis = camera_basis(yaw, pitch)
    projected_faces = []
    all_xy = []
    for _, verts, uvs, normal, bone_name in faces:
        p = project(verts, basis)
        projected_faces.append((p, uvs, normal, bone_name))
        all_xy.append(p[:, :2])
    xy = np.concatenate(all_xy, axis=0)
    xmin, ymin = np.min(xy, axis=0)
    xmax, ymax = np.max(xy, axis=0)
    span_x = max(1e-6, xmax - xmin)
    span_y = max(1e-6, ymax - ymin)
    margin = 0.11
    scale = min((size * (1 - 2 * margin)) / span_x, (size * (1 - 2 * margin)) / span_y)
    center_x = (xmin + xmax) / 2
    center_y = (ymin + ymax) / 2

    canvas = np.zeros((size, size, 4), dtype=np.uint8)
    zbuf = np.full((size, size), np.inf, dtype=float)

    base = np.asarray(base_img.convert("RGBA"), dtype=np.uint8)
    accessory = np.asarray(accessory_img.convert("RGBA"), dtype=np.uint8)
    light = np.array([-0.35, 0.72, -0.6], dtype=float)
    light /= np.linalg.norm(light)

    for p, uvs, normal, bone_name in projected_faces:
        q = p.copy()
        q[:, 0] = (q[:, 0] - center_x) * scale + size / 2
        q[:, 1] = size / 2 - (q[:, 1] - center_y) * scale
        n = normal / max(np.linalg.norm(normal), 1e-9)
        shade = 0.72 + 0.28 * max(0.0, float(np.dot(n, -light)))
        texture = accessory if bone_name.startswith("ouros_") else base
        tex_h, tex_w, _ = texture.shape
        for idx in ((0, 1, 2), (0, 2, 3)):
            raster_triangle(canvas, zbuf, q[list(idx)], uvs[list(idx)], texture, tex_w, tex_h, shade)

    image = Image.fromarray(canvas, mode="RGBA")
    alpha = np.asarray(image)[:, :, 3]
    ys, xs = np.where(alpha > 0)
    if len(xs):
        pad = 14
        box = (
            max(0, xs.min() - pad),
            max(0, ys.min() - pad),
            min(size, xs.max() + pad + 1),
            min(size, ys.max() + pad + 1),
        )
        cropped = image.crop(box)
        target = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        ratio = min((size * 0.9) / cropped.width, (size * 0.9) / cropped.height)
        resized = cropped.resize(
            (max(1, int(cropped.width * ratio)), max(1, int(cropped.height * ratio))),
            Image.Resampling.NEAREST,
        )
        target.alpha_composite(resized, ((size - resized.width) // 2, (size - resized.height) // 2))
        image = target
    Path(output).parent.mkdir(parents=True, exist_ok=True)
    image.save(output, optimize=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", required=True)
    parser.add_argument("--base-texture", required=True)
    parser.add_argument("--accessory-texture", required=True)
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--size", type=int, default=1024)
    parser.add_argument(
        "--pose-set",
        choices=("orthographic", "decision", "all"),
        default="orthographic",
        help="orthographic=four neutral views; decision=hero/battle/walking; all=both sets",
    )
    args = parser.parse_args()

    model = json.loads(Path(args.model).read_text(encoding="utf-8"))
    base = Image.open(args.base_texture)
    accessory = Image.open(args.accessory_texture)
    out = Path(args.output_dir)

    if args.pose_set in ("orthographic", "all"):
        for view in ("front", "left", "right", "back"):
            yaw, pitch = view_angles(view)
            path = out / f"{view}.png"
            render(model, base, accessory, path, args.size, pose="neutral", yaw=yaw, pitch=pitch)
            print(path)

    if args.pose_set in ("decision", "all"):
        for pose_name in ("hero_three_quarter", "battle_ready", "walking"):
            yaw, pitch = POSES[pose_name]["camera"]
            path = out / "poses" / f"{pose_name}.png"
            render(model, base, accessory, path, args.size, pose=pose_name, yaw=yaw, pitch=pitch)
            print(path)


if __name__ == "__main__":
    main()
