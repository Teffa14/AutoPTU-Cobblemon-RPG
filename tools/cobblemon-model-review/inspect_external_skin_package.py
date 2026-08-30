#!/usr/bin/env python3
"""Inspect a public external skin package without importing its assets into production.

Research-only. The tool hashes target MODEL/TEXTURE/ANIMATION/POSER/RESOLVER
files and emits structural summaries that are useful for technique study. When
an official Cobblemon model is supplied it also reports added/common/changed
bone structure without reproducing third-party geometry coordinates.

This tool never grants artistic eligibility or reuse rights by itself.
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


def bedrock_geo(payload: object) -> dict | None:
    if not isinstance(payload, dict):
        return None
    geos = payload.get("minecraft:geometry")
    if not isinstance(geos, list) or not geos or not isinstance(geos[0], dict):
        return None
    return geos[0]


def bone_map(geo: dict) -> dict[str, dict]:
    result: dict[str, dict] = {}
    for bone in geo.get("bones", []):
        if isinstance(bone, dict) and isinstance(bone.get("name"), str):
            result[bone["name"]] = bone
    return result


def summarize_bone(bone: dict) -> dict:
    cubes = bone.get("cubes", []) if isinstance(bone.get("cubes"), list) else []
    rotated = sum(1 for cube in cubes if isinstance(cube, dict) and cube.get("rotation"))
    inflated = sum(1 for cube in cubes if isinstance(cube, dict) and cube.get("inflate") not in (None, 0, 0.0))
    sizes: list[float] = []
    for cube in cubes:
        if not isinstance(cube, dict):
            continue
        size = cube.get("size")
        if isinstance(size, list):
            sizes.extend(float(value) for value in size if isinstance(value, (int, float)))
    return {
        "name": bone.get("name"),
        "parent": bone.get("parent"),
        "hasPivot": isinstance(bone.get("pivot"), list),
        "hasBoneRotation": bool(bone.get("rotation")),
        "cubeCount": len(cubes),
        "rotatedCubeCount": rotated,
        "inflatedCubeCount": inflated,
        "largestCubeAxis": round(max(sizes), 3) if sizes else None,
    }


def summarize_geometry(payload: object) -> dict | None:
    geo = bedrock_geo(payload)
    if geo is None:
        return None
    bones = geo.get("bones", []) if isinstance(geo.get("bones"), list) else []
    cube_count = 0
    bone_names: list[str] = []
    for bone in bones:
        if not isinstance(bone, dict):
            continue
        if isinstance(bone.get("name"), str):
            bone_names.append(bone["name"])
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


def classify_json(path: str, data: bytes) -> tuple[str, dict | None, object | None]:
    try:
        payload = json.loads(data.decode("utf-8"))
    except Exception:
        return "JSON_UNPARSED", None, None
    geometry = summarize_geometry(payload)
    if geometry:
        return "MODEL", geometry, payload
    lowered = lower_path(path)
    if "animation" in lowered:
        return "ANIMATION", {"jsonTopLevelKeys": list(payload)[:20] if isinstance(payload, dict) else []}, payload
    if "resolver" in lowered:
        return "RESOLVER", {"jsonTopLevelKeys": list(payload)[:20] if isinstance(payload, dict) else []}, payload
    if "poser" in lowered:
        return "POSER", {"jsonTopLevelKeys": list(payload)[:20] if isinstance(payload, dict) else []}, payload
    return "JSON", {"jsonTopLevelKeys": list(payload)[:20] if isinstance(payload, dict) else []}, payload


def compare_models(external_payload: object, official_payload: object) -> dict | None:
    external_geo = bedrock_geo(external_payload)
    official_geo = bedrock_geo(official_payload)
    if external_geo is None or official_geo is None:
        return None
    external = bone_map(external_geo)
    official = bone_map(official_geo)
    external_names = list(external)
    official_names = list(official)
    added = [name for name in external_names if name not in official]
    missing = [name for name in official_names if name not in external]
    common = [name for name in external_names if name in official]
    changed_common = [name for name in common if external[name] != official[name]]
    added_summaries = [summarize_bone(external[name]) for name in added]
    return {
        "officialBoneCount": len(official_names),
        "externalBoneCount": len(external_names),
        "addedBoneCount": len(added),
        "addedBoneNames": added,
        "addedBoneSummaries": added_summaries,
        "missingOfficialBoneCount": len(missing),
        "missingOfficialBoneNames": missing,
        "changedCommonBoneCount": len(changed_common),
        "changedCommonBoneNames": changed_common,
        "externalPreservesOfficialBoneOrderPrefix": external_names[: len(official_names)] == official_names,
    }


def animation_summary(payload: object, target_bones: set[str]) -> dict:
    if not isinstance(payload, dict) or not isinstance(payload.get("animations"), dict):
        return {"animationCount": 0, "animationsTouchingTargetBones": []}
    touching: list[dict] = []
    for animation_name, animation in payload["animations"].items():
        if not isinstance(animation, dict):
            continue
        bones = animation.get("bones", {})
        if not isinstance(bones, dict):
            continue
        names = sorted(set(bones) & target_bones)
        if names:
            touching.append({"animation": animation_name, "targetBones": names})
    return {
        "animationCount": len(payload["animations"]),
        "animationsTouchingTargetBones": touching,
    }


def poser_summary(payload: object) -> dict:
    if not isinstance(payload, dict):
        return {}
    poses = payload.get("poses", {})
    animations = payload.get("animations", {})
    return {
        "poseNames": sorted(poses) if isinstance(poses, dict) else [],
        "animationAliases": sorted(animations) if isinstance(animations, dict) else [],
    }


def resolver_summary(payload: object) -> dict:
    if not isinstance(payload, dict):
        return {}
    variations = payload.get("variations", [])
    summarized: list[dict] = []
    if isinstance(variations, list):
        for variation in variations:
            if not isinstance(variation, dict):
                continue
            summarized.append(
                {
                    "aspects": variation.get("aspects"),
                    "model": variation.get("model"),
                    "poser": variation.get("poser"),
                    "texture": variation.get("texture"),
                    "layerCount": len(variation.get("layers", [])) if isinstance(variation.get("layers"), list) else 0,
                }
            )
    return {
        "species": payload.get("species"),
        "order": payload.get("order"),
        "variations": summarized,
    }


def inspect_zip(package: Path, terms: list[str], official_payload: object | None) -> dict:
    files: list[dict] = []
    parsed: list[tuple[dict, object | None]] = []
    license_entries: list[dict] = []
    with zipfile.ZipFile(package) as zf:
        for info in zf.infolist():
            if info.is_dir():
                continue
            lowered_name = lower_path(info.filename)
            if any(token in lowered_name.rsplit("/", 1)[-1] for token in ("license", "licence", "copying")):
                license_entries.append({"path": info.filename, "size": info.file_size})
            if not matches_target(info.filename, terms):
                continue
            if not (lowered_name.endswith(JSON_SUFFIXES) or lowered_name.endswith(IMAGE_SUFFIXES) or lowered_name.endswith(MODEL_SUFFIXES)):
                continue
            data = zf.read(info)
            entry: dict = {"path": info.filename, "size": len(data), "sha256": sha256(data)}
            payload: object | None = None
            if lowered_name.endswith(IMAGE_SUFFIXES):
                entry["kind"] = "TEXTURE"
                entry["imageDimensions"] = png_dimensions(data)
            else:
                kind, metadata, payload = classify_json(info.filename, data)
                entry["kind"] = kind
                if metadata:
                    entry["metadata"] = metadata
            files.append(entry)
            parsed.append((entry, payload))

    files.sort(key=lambda item: (item["kind"], item["path"]))
    model_pairs = [(entry, payload) for entry, payload in parsed if entry["kind"] == "MODEL" and payload is not None]
    external_payload = model_pairs[0][1] if model_pairs else None
    model_comparison = compare_models(external_payload, official_payload) if official_payload is not None else None
    target_bones = set(model_comparison.get("addedBoneNames", [])) if model_comparison else set()

    structural: dict = {}
    if model_comparison:
        structural["modelComparison"] = model_comparison
    for entry, payload in parsed:
        if entry["kind"] == "ANIMATION" and payload is not None:
            structural.setdefault("animations", {})[entry["path"]] = animation_summary(payload, target_bones)
        elif entry["kind"] == "POSER" and payload is not None:
            structural.setdefault("posers", {})[entry["path"]] = poser_summary(payload)
        elif entry["kind"] == "RESOLVER" and payload is not None:
            structural.setdefault("resolvers", {})[entry["path"]] = resolver_summary(payload)

    return {
        "package": {"path": package.name, "size": package.stat().st_size, "sha256": sha256(package.read_bytes())},
        "targetTerms": terms,
        "candidateFiles": files,
        "modelFiles": [item for item in files if item["kind"] == "MODEL"],
        "textureFiles": [item for item in files if item["kind"] == "TEXTURE"],
        "animationFiles": [item for item in files if item["kind"] == "ANIMATION"],
        "resolverFiles": [item for item in files if item["kind"] == "RESOLVER"],
        "poserFiles": [item for item in files if item["kind"] == "POSER"],
        "packageLicenseEntries": license_entries,
        "structuralStudy": structural,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("package", type=Path)
    parser.add_argument("--species", required=True)
    parser.add_argument("--alias", action="append", default=[])
    parser.add_argument("--official-model", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()

    if not args.package.is_file():
        raise SystemExit(f"missing package: {args.package}")

    official_payload: object | None = None
    if args.official_model:
        if not args.official_model.is_file():
            raise SystemExit(f"missing official model: {args.official_model}")
        official_payload = json.loads(args.official_model.read_text(encoding="utf-8"))

    terms = [args.species, *args.alias]
    report = inspect_zip(args.package, terms, official_payload)
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
