#!/usr/bin/env python3
"""Render deterministic orthographic PNG previews from a Bedrock geometry JSON.

This is a review renderer, not concept art. It reads the exact .geo.json and PNG
textures used by Cobblemon and rasterizes the model in software so CI can produce
front/left/right/back evidence without a graphical Minecraft client.
"""

from __future__ import annotations

import argparse
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image

FACE_NAMES = ("north", "south", "east", "west", "up", "down")


def rotation_matrix_xyz(deg):
    rx, ry, rz = [math.radians(float(v)) for v in deg]
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    mx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]], dtype=float)
    my = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]], dtype=float)
    mz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]], dtype=float)
    return mz @ my @ mx


def rotate_points(points, pivot, rotation):
    if not rotation:
        return points
    pivot = np.asarray(pivot, dtype=float)
    matrix = rotation_matrix_xyz(rotation)
    return (points - pivot) @ matrix.T + pivot


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
    # Bedrock PNG origin is top-left. Keep face orientation deterministic.
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

    # Quad winding is chosen only for stable normals/shading; both sides are visible.
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
        # Skip geometrically degenerate faces, but keep the two meaningful faces of planes.
        a = verts[1] - verts[0]
        b = verts[2] - verts[0]
        area = np.linalg.norm(np.cross(a, b))
        if area < 1e-8:
            continue
        if rotation:
            verts = rotate_points(verts, pivot or [0, 0, 0], rotation)
        normal = np.cross(verts[1] - verts[0], verts[2] - verts[0])
        nlen = np.linalg.norm(normal)
        if nlen > 0:
            normal /= nlen
        result.append((face_name, verts, face_uvs(uv_rects[face_name], mirror), normal, bone_name))
    return result


def build_faces(model):
    geometry = model["minecraft:geometry"][0]
    faces = []
    for bone in geometry.get("bones", []):
        name = bone.get("name", "")
        for cube in bone.get("cubes", []):
            faces.extend(cube_faces(cube, name))
    return geometry["description"], faces


def camera_basis(view):
    # Model front is -Z in Cobblemon/Bedrock geometry.
    if view == "front":
        right, up, depth = np.array([1, 0, 0.0]), np.array([0, 1, 0.0]), np.array([0, 0, 1.0])
    elif view == "back":
        right, up, depth = np.array([-1, 0, 0.0]), np.array([0, 1, 0.0]), np.array([0, 0, -1.0])
    elif view == "left":
        right, up, depth = np.array([0, 0, -1.0]), np.array([0, 1, 0.0]), np.array([1, 0, 0.0])
    elif view == "right":
        right, up, depth = np.array([0, 0, 1.0]), np.array([0, 1, 0.0]), np.array([-1, 0, 0.0])
    else:
        raise ValueError(view)
    return right, up, depth


def project(points, basis):
    right, up, depth = basis
    return np.stack([points @ right, points @ up, points @ depth], axis=1)


def triangle_bbox(p, width, height):
    xmin = max(0, int(math.floor(float(np.min(p[:, 0])))))
    xmax = min(width - 1, int(math.ceil(float(np.max(p[:, 0])))))
    ymin = max(0, int(math.floor(float(np.min(p[:, 1])))))
    ymax = min(height - 1, int(math.ceil(float(np.max(p[:, 1])))))
    return xmin, xmax, ymin, ymax


def raster_triangle(canvas, zbuf, pts, uvs, normal, texture, tex_w, tex_h, shade):
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


def render(model, base_img, accessory_img, view, output, size=1024):
    desc, faces = build_faces(model)
    basis = camera_basis(view)
    projected_faces = []
    all_xy = []
    for face_name, verts, uvs, normal, bone_name in faces:
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
        # Camera is located at negative depth infinity; smaller projected depth is nearer.
        n = normal / max(np.linalg.norm(normal), 1e-9)
        shade = 0.72 + 0.28 * max(0.0, float(np.dot(n, -light)))
        texture = accessory if bone_name.startswith("ouros_") else base
        tex_h, tex_w, _ = texture.shape
        for idx in ((0, 1, 2), (0, 2, 3)):
            raster_triangle(canvas, zbuf, q[list(idx)], uvs[list(idx)], n, texture, tex_w, tex_h, shade)

    image = Image.fromarray(canvas, mode="RGBA")
    # Tighten visible crop then place on a stable 1024 square transparent canvas.
    alpha = np.asarray(image)[:, :, 3]
    ys, xs = np.where(alpha > 0)
    if len(xs):
        pad = 14
        box = (max(0, xs.min() - pad), max(0, ys.min() - pad), min(size, xs.max() + pad + 1), min(size, ys.max() + pad + 1))
        cropped = image.crop(box)
        target = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        ratio = min((size * 0.9) / cropped.width, (size * 0.9) / cropped.height)
        resized = cropped.resize((max(1, int(cropped.width * ratio)), max(1, int(cropped.height * ratio))), Image.Resampling.NEAREST)
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
    args = parser.parse_args()

    model = json.loads(Path(args.model).read_text(encoding="utf-8"))
    base = Image.open(args.base_texture)
    accessory = Image.open(args.accessory_texture)
    out = Path(args.output_dir)
    for view in ("front", "left", "right", "back"):
        render(model, base, accessory, view, out / f"{view}.png", args.size)
        print(out / f"{view}.png")


if __name__ == "__main__":
    main()
