#!/usr/bin/env python3
"""Inspect a public external skin package without importing its assets into production.

This tool is research-only. It hashes and summarizes candidate MODEL/TEXTURE files
inside a ZIP so same-species reference dossiers can cite real inspected assets.
It never grants artistic eligibility or reuse rights by itself.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import struct
import zipfile
from pathlib import Path

MODEL_SUFFIXES = (".geo.json", ".bbmodel")
JSON_SUFFIXES = (".json",)
IMAGE_SUFFIXES = (".png",)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def lower_path(name: str) -> str:
    return name.replace("\\", "/").lower()


def matches_target(name: str, terms: list[str]) -> bool:
    lowered = lower_path(name)
    return any(term.lower() in lowered for term in terms)


def png_dimensions(data: bytes) -> list[int] | None:
    if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    width, height = struct.unpack(">II", data[16:24])
    return [width, height]


def count_bedrock_geometry(payload: object) -> dict | None:
    if not isinstance(payload, dict):
        return None
    geos = payload.get("minecraft:geometry")
    if not isinstance(geos, list) or not geos:
        return None
    geo = geos[0]
    if not isinstance(geo, dict):
        return None
    bones = geo.get("bones", [])
    if not isinstance(bones, list):
        bones = []
    cube_count = 0
    bone_names: list[str] = []
    for bone in bones:
        if not isinstance(bone, dict):
            continue
        name = bone.get("name")
        if isinstance(name, str):
            bone_names.append(name)
        cubes = bone.get("cubes", [])
        if isinstance(cubes, list):
            cube_count += len(cubes)
    description = geo.get("description", {}) if isinstance(geo.get("description"), dict) else {}
    return {
        "format": "bedrock_geo_json",
        "identifier": description.get("identifier"),
        "textureWidth": description.get("texture_width"),
        "textureHeight": description.get("texture_height"),
        "boneCount": len(bones),
        "cubeCount": cube_count,
        "boneNames": bone_names,
    }


def classify_json(path: str, data: bytes) -> tuple[str, dict | None]:
    try:
        payload = json.loads(data.decode("utf-8"))
    except Exception:
        return "JSON_UNPARSED", None
    geometry = count_bedrock_geometry(payload)
    if geometry:
        return "MODEL", geometry
    lowered = lower_path(path)
    if "animation" in lowered:
        return "ANIMATION", {"jsonTopLevelKeys": list(payload)[:20] if isinstance(payload, dict) else []}
    if "resolver" in lowered:
        return "RESOLVER", {"jsonTopLevelKeys": list(payload)[:20] if isinstance(payload, dict) else []}
    if "poser" in lowered:
        return "POSER", {"jsonTopLevelKeys": list(payload)[:20] if isinstance(payload, dict) else []}
    return "JSON", {"jsonTopLevelKeys": list(payload)[:20] if isinstance(payload, dict) else []}


def inspect_zip(package: Path, terms: list[str]) -> dict:
    files: list[dict] = []
    with zipfile.ZipFile(package) as zf:
        for info in zf.infolist():
            if info.is_dir() or not matches_target(info.filename, terms):
                continue
            name = info.filename
            lowered = lower_path(name)
            if not (lowered.endswith(JSON_SUFFIXES) or lowered.endswith(IMAGE_SUFFIXES) or lowered.endswith(MODEL_SUFFIXES)):
                continue
            data = zf.read(info)
            entry: dict = {
                "path": name,
                "size": len(data),
                "sha256": sha256(data),
            }
            if lowered.endswith(IMAGE_SUFFIXES):
                entry["kind"] = "TEXTURE"
                entry["imageDimensions"] = png_dimensions(data)
            else:
                kind, metadata = classify_json(name, data)
                entry["kind"] = kind
                if metadata:
                    entry["metadata"] = metadata
            files.append(entry)
    files.sort(key=lambda item: (item["kind"], item["path"]))
    return {
        "package": {
            "path": package.name,
            "size": package.stat().st_size,
            "sha256": sha256(package.read_bytes()),
        },
        "targetTerms": terms,
        "candidateFiles": files,
        "modelFiles": [item for item in files if item["kind"] == "MODEL"],
        "textureFiles": [item for item in files if item["kind"] == "TEXTURE"],
        "animationFiles": [item for item in files if item["kind"] == "ANIMATION"],
        "resolverFiles": [item for item in files if item["kind"] == "RESOLVER"],
        "poserFiles": [item for item in files if item["kind"] == "POSER"],
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("package", type=Path)
    parser.add_argument("--species", required=True)
    parser.add_argument("--alias", action="append", default=[])
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()

    if not args.package.is_file():
        raise SystemExit(f"missing package: {args.package}")

    terms = [args.species, *args.alias]
    report = inspect_zip(args.package, terms)
    report["species"] = args.species.lower()
    report["inspectionScope"] = "STUDY_ONLY_EXTERNAL_PACKAGE"
    report["eligibilityDecision"] = "NOT_AUTOMATICALLY_GRANTED"
    report["reuseDecision"] = "NOT_AUTOMATICALLY_GRANTED"

    text = json.dumps(report, indent=2, ensure_ascii=False) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(text, encoding="utf-8")
    print(text, end="")

    if not report["modelFiles"] or not report["textureFiles"]:
        raise SystemExit("REFERENCE INSPECTION INCOMPLETE: no target MODEL+TEXTURE pair found")


if __name__ == "__main__":
    main()
