#!/usr/bin/env python3
"""Inspect a public external skin package without copying its assets into production.

The tool emits metadata, hashes and non-expressive structural metrics only. It never
extracts or commits third-party model/texture bytes. This supports STUDY_ONLY license
research and the same-species pre-model gate.
"""
from __future__ import annotations

import argparse
import hashlib
import io
import json
import math
import re
import zipfile
from collections import Counter
from pathlib import Path

from PIL import Image

MODEL_SUFFIXES = (".geo.json", ".bbmodel", ".model.json")
TEXTURE_SUFFIXES = (".png",)
ANIMATION_SUFFIXES = (".animation.json",)
AUX_TOKENS = ("resolver", "poser", "species", "feature")


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def classify(path: str) -> str:
    low = path.lower()
    if low.endswith(MODEL_SUFFIXES):
        return "MODEL"
    if low.endswith(TEXTURE_SUFFIXES):
        return "TEXTURE"
    if low.endswith(ANIMATION_SUFFIXES):
        return "ANIMATION"
    if low.endswith(".json") and any(token in low for token in AUX_TOKENS):
        return "AUX_JSON"
    if any(token in low for token in ("license", "copying", "notice")):
        return "LICENSE"
    return "OTHER"


def load_json(data: bytes):
    try:
        return json.loads(data.decode("utf-8"))
    except Exception:
        return None


def json_identifiers(parsed) -> dict:
    if parsed is None:
        return {}
    text = json.dumps(parsed, separators=(",", ":"))
    identifiers = sorted(set(re.findall(r"(?:geometry|animation)\.[A-Za-z0-9_.:-]+", text)))
    aspects = sorted(set(re.findall(r'"aspects":\[(.*?)\]', text)))
    return {"identifiers": identifiers[:50], "aspectFragments": aspects[:50]}


def geometry_metrics(parsed) -> dict:
    if not isinstance(parsed, dict):
        return {}
    geometries = parsed.get("minecraft:geometry")
    if not isinstance(geometries, list) or not geometries:
        return {}
    geometry = geometries[0]
    bones = geometry.get("bones", [])
    if not isinstance(bones, list):
        return {}

    cubes = []
    rotated = 0
    parented = 0
    locator_count = 0
    bone_cube_counts = []
    min_xyz = [math.inf, math.inf, math.inf]
    max_xyz = [-math.inf, -math.inf, -math.inf]
    size_hist = Counter()

    for bone in bones:
        if not isinstance(bone, dict):
            continue
        if bone.get("parent"):
            parented += 1
        locators = bone.get("locators", {})
        if isinstance(locators, dict):
            locator_count += len(locators)
        bcubes = bone.get("cubes", [])
        if not isinstance(bcubes, list):
            bcubes = []
        bone_cube_counts.append(len(bcubes))
        for cube in bcubes:
            if not isinstance(cube, dict):
                continue
            cubes.append(cube)
            rotation = cube.get("rotation", [0, 0, 0])
            if isinstance(rotation, list) and any(abs(float(v)) > 1e-6 for v in rotation[:3]):
                rotated += 1
            origin = cube.get("origin", [0, 0, 0])
            size = cube.get("size", [0, 0, 0])
            if isinstance(origin, list) and isinstance(size, list) and len(origin) >= 3 and len(size) >= 3:
                try:
                    vals = [float(origin[i]) for i in range(3)]
                    dims = [float(size[i]) for i in range(3)]
                    for i in range(3):
                        min_xyz[i] = min(min_xyz[i], vals[i])
                        max_xyz[i] = max(max_xyz[i], vals[i] + dims[i])
                    bucket = tuple(round(d, 1) for d in sorted(dims, reverse=True))
                    size_hist[bucket] += 1
                except (TypeError, ValueError):
                    pass

    bounds = None
    if cubes and all(math.isfinite(v) for v in min_xyz + max_xyz):
        bounds = {
            "min": [round(v, 3) for v in min_xyz],
            "max": [round(v, 3) for v in max_xyz],
            "span": [round(max_xyz[i] - min_xyz[i], 3) for i in range(3)],
        }
    return {
        "boneCount": len(bones),
        "parentedBoneCount": parented,
        "locatorCount": locator_count,
        "cubeCount": len(cubes),
        "rotatedCubeCount": rotated,
        "rotatedCubeRatio": round(rotated / len(cubes), 4) if cubes else 0.0,
        "bonesWithCubes": sum(v > 0 for v in bone_cube_counts),
        "maxCubesOnOneBone": max(bone_cube_counts, default=0),
        "bounds": bounds,
        "topCubeSizeFamilies": [
            {"size": list(size), "count": count}
            for size, count in size_hist.most_common(8)
        ],
    }


def texture_metrics(data: bytes) -> dict:
    try:
        image = Image.open(io.BytesIO(data)).convert("RGBA")
    except Exception:
        return {}
    pixels = list(image.getdata())
    total = len(pixels)
    opaque = sum(p[3] == 255 for p in pixels)
    transparent = sum(p[3] == 0 for p in pixels)
    partial = total - opaque - transparent
    rgb = [(p[0], p[1], p[2]) for p in pixels if p[3] > 0]
    unique = len(set(rgb))
    if rgb:
        values = [max(c) for c in rgb]
        min_value, max_value = min(values), max(values)
        mean_value = round(sum(values) / len(values), 2)
    else:
        min_value = max_value = 0
        mean_value = 0.0
    return {
        "width": image.width,
        "height": image.height,
        "opaquePixelCount": opaque,
        "transparentPixelCount": transparent,
        "partialAlphaPixelCount": partial,
        "uniqueVisibleRgbCount": unique,
        "visibleValueRange": [min_value, max_value],
        "meanVisibleValue": mean_value,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("package", type=Path)
    parser.add_argument("--species", required=True)
    parser.add_argument("--project", required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--source-url", required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    raw = args.package.read_bytes()
    species = args.species.casefold()
    records = []
    with zipfile.ZipFile(args.package) as zf:
        for info in zf.infolist():
            if info.is_dir():
                continue
            path = info.filename
            low = path.casefold()
            kind = classify(path)
            if species not in low and kind != "LICENSE":
                continue
            data = zf.read(info)
            record = {"path": path, "kind": kind, "size": len(data), "sha256": sha256(data)}
            if kind in {"MODEL", "ANIMATION", "AUX_JSON"}:
                parsed = load_json(data)
                record.update(json_identifiers(parsed))
                if kind == "MODEL":
                    record["geometryMetrics"] = geometry_metrics(parsed)
            elif kind == "TEXTURE":
                record["textureMetrics"] = texture_metrics(data)
            records.append(record)

    report = {
        "format": "ouros.external-reference-package-audit.v2",
        "project": args.project,
        "sourceVersion": args.version,
        "sourceUrl": args.source_url,
        "species": args.species.lower(),
        "packageFilename": args.package.name,
        "packageSha256": sha256(raw),
        "matchingFiles": sorted(records, key=lambda r: (r["kind"], r["path"])),
        "modelCount": sum(r["kind"] == "MODEL" for r in records),
        "textureCount": sum(r["kind"] == "TEXTURE" for r in records),
        "animationCount": sum(r["kind"] == "ANIMATION" for r in records),
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
