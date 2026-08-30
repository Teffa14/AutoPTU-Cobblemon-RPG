#!/usr/bin/env python3
"""Hash and structurally inspect an external GLB + texture for skin research.

The report intentionally contains metadata only. It never copies third-party
mesh buffers, UV arrays or texture bytes into Ouros production, and it never
opens the species reference gate by itself.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import struct
from pathlib import Path

GLB_MAGIC = 0x46546C67
JSON_CHUNK = 0x4E4F534A
BIN_CHUNK = 0x004E4942


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def png_dimensions(data: bytes) -> list[int] | None:
    if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    return list(struct.unpack(">II", data[16:24]))


def parse_glb(path: Path) -> tuple[dict, int]:
    data = path.read_bytes()
    if len(data) < 20:
        raise SystemExit("invalid GLB: too small")
    magic, version, total_length = struct.unpack_from("<III", data, 0)
    if magic != GLB_MAGIC or version != 2 or total_length != len(data):
        raise SystemExit(
            f"invalid GLB header: magic={magic:#x} version={version} "
            f"declared={total_length} actual={len(data)}"
        )
    offset = 12
    payload: dict | None = None
    bin_bytes = 0
    while offset + 8 <= len(data):
        chunk_length, chunk_type = struct.unpack_from("<II", data, offset)
        offset += 8
        chunk = data[offset : offset + chunk_length]
        offset += chunk_length
        if chunk_type == JSON_CHUNK:
            payload = json.loads(chunk.rstrip(b" \t\r\n\x00").decode("utf-8"))
        elif chunk_type == BIN_CHUNK:
            bin_bytes += len(chunk)
    if payload is None:
        raise SystemExit("invalid GLB: missing JSON chunk")
    return payload, bin_bytes


def accessor_component_count(type_name: str) -> int:
    return {
        "SCALAR": 1,
        "VEC2": 2,
        "VEC3": 3,
        "VEC4": 4,
        "MAT2": 4,
        "MAT3": 9,
        "MAT4": 16,
    }.get(type_name, 0)


def summarize(path: Path, texture: Path | None) -> dict:
    gltf, bin_bytes = parse_glb(path)
    meshes = gltf.get("meshes", []) if isinstance(gltf.get("meshes"), list) else []
    nodes = gltf.get("nodes", []) if isinstance(gltf.get("nodes"), list) else []
    accessors = gltf.get("accessors", []) if isinstance(gltf.get("accessors"), list) else []
    materials = gltf.get("materials", []) if isinstance(gltf.get("materials"), list) else []
    images = gltf.get("images", []) if isinstance(gltf.get("images"), list) else []
    textures = gltf.get("textures", []) if isinstance(gltf.get("textures"), list) else []

    primitive_count = 0
    position_accessor_ids: set[int] = set()
    index_accessor_ids: set[int] = set()
    material_ids: set[int] = set()
    attribute_semantics: set[str] = set()
    mode_counts: dict[str, int] = {}
    for mesh in meshes:
        if not isinstance(mesh, dict):
            continue
        primitives = mesh.get("primitives", [])
        if not isinstance(primitives, list):
            continue
        for primitive in primitives:
            if not isinstance(primitive, dict):
                continue
            primitive_count += 1
            mode = str(primitive.get("mode", 4))
            mode_counts[mode] = mode_counts.get(mode, 0) + 1
            attrs = primitive.get("attributes", {})
            if isinstance(attrs, dict):
                attribute_semantics.update(str(key) for key in attrs)
                pos = attrs.get("POSITION")
                if isinstance(pos, int):
                    position_accessor_ids.add(pos)
            idx = primitive.get("indices")
            if isinstance(idx, int):
                index_accessor_ids.add(idx)
            material = primitive.get("material")
            if isinstance(material, int):
                material_ids.add(material)

    def accessor_count(ids: set[int]) -> int:
        total = 0
        for index in ids:
            if 0 <= index < len(accessors) and isinstance(accessors[index], dict):
                count = accessors[index].get("count")
                if isinstance(count, int):
                    total += count
        return total

    position_bounds: list[dict] = []
    for index in sorted(position_accessor_ids):
        if not (0 <= index < len(accessors)) or not isinstance(accessors[index], dict):
            continue
        accessor = accessors[index]
        position_bounds.append(
            {
                "accessor": index,
                "count": accessor.get("count"),
                "type": accessor.get("type"),
                "min": accessor.get("min"),
                "max": accessor.get("max"),
            }
        )

    material_summaries: list[dict] = []
    for index in sorted(material_ids):
        if not (0 <= index < len(materials)) or not isinstance(materials[index], dict):
            continue
        material = materials[index]
        pbr = material.get("pbrMetallicRoughness", {})
        if not isinstance(pbr, dict):
            pbr = {}
        material_summaries.append(
            {
                "index": index,
                "name": material.get("name"),
                "alphaMode": material.get("alphaMode", "OPAQUE"),
                "doubleSided": bool(material.get("doubleSided", False)),
                "hasBaseColorTexture": isinstance(pbr.get("baseColorTexture"), dict),
                "hasMetallicRoughnessTexture": isinstance(pbr.get("metallicRoughnessTexture"), dict),
                "hasNormalTexture": isinstance(material.get("normalTexture"), dict),
                "hasEmissiveTexture": isinstance(material.get("emissiveTexture"), dict),
                "metallicFactor": pbr.get("metallicFactor"),
                "roughnessFactor": pbr.get("roughnessFactor"),
            }
        )

    report = {
        "model": {
            "path": path.name,
            "sha256": digest(path),
            "sizeBytes": path.stat().st_size,
            "format": "GLB_2_0",
            "binaryChunkBytes": bin_bytes,
        },
        "structure": {
            "meshCount": len(meshes),
            "primitiveCount": primitive_count,
            "nodeCount": len(nodes),
            "accessorCount": len(accessors),
            "materialCount": len(materials),
            "imageCount": len(images),
            "textureObjectCount": len(textures),
            "positionVertexCount": accessor_count(position_accessor_ids),
            "indexCount": accessor_count(index_accessor_ids),
            "triangleCountEstimate": accessor_count(index_accessor_ids) // 3,
            "attributeSemantics": sorted(attribute_semantics),
            "primitiveModeCounts": mode_counts,
            "positionBounds": position_bounds,
            "materials": material_summaries,
            "skinsCount": len(gltf.get("skins", [])) if isinstance(gltf.get("skins"), list) else 0,
            "animationsCount": len(gltf.get("animations", [])) if isinstance(gltf.get("animations"), list) else 0,
        },
        "embeddedAssetNames": {
            "images": [entry.get("name") for entry in images if isinstance(entry, dict) and entry.get("name")],
            "meshes": [entry.get("name") for entry in meshes if isinstance(entry, dict) and entry.get("name")],
            "nodes": [entry.get("name") for entry in nodes if isinstance(entry, dict) and entry.get("name")][:100],
        },
        "eligibilityDecision": "NOT_AUTOMATICALLY_GRANTED",
        "reuseDecision": "NOT_AUTOMATICALLY_GRANTED",
    }
    if texture:
        data = texture.read_bytes()
        report["texture"] = {
            "path": texture.name,
            "sha256": digest(texture),
            "sizeBytes": texture.stat().st_size,
            "dimensions": png_dimensions(data),
        }
    return report


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("model", type=Path)
    parser.add_argument("--texture", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    if not args.model.is_file():
        raise SystemExit(f"missing model: {args.model}")
    if args.texture and not args.texture.is_file():
        raise SystemExit(f"missing texture: {args.texture}")
    report = summarize(args.model, args.texture)
    text = json.dumps(report, indent=2, ensure_ascii=False) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(text, encoding="utf-8")
    print(text, end="")
    if report["structure"]["meshCount"] < 1 or report["structure"]["positionVertexCount"] < 3:
        raise SystemExit("REFERENCE INSPECTION INCOMPLETE: GLB contains no usable mesh geometry")
    if args.texture and not report.get("texture", {}).get("dimensions"):
        raise SystemExit("REFERENCE INSPECTION INCOMPLETE: supplied texture is not a valid PNG")


if __name__ == "__main__":
    main()
