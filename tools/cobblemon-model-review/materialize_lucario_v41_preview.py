#!/usr/bin/env python3
"""Materialize the Lucario V41 scene seed for Blockbench preview only.

This is not a production builder and it does not claim Blockbench authorship. It
removes every historical Ouros cosmetic bone, preserves the first 87 official
Lucario bones JSON-equivalently, appends V41 preview geometry, and emits a
professional-style manifest used only to obtain real matched-camera Blockbench
evidence.

The preview supports three presentation primitives:
- ordinary Bedrock cubes;
- zero-thickness Bedrock planes whose visible contour is cut by an alpha sprite;
- explicit indexed ``poly_mesh`` only as a compatibility experiment.

The alpha-plane path is deliberately different from the rejected box-stacking
workflow: geometry supplies a small number of animation-parented cloth surfaces,
while a deterministic accessory atlas supplies tapered/scalloped/irregular edge
shape. Sprite atlas regions are allocated only inside pixels that are transparent
in the exact official-derived body texture, so cosmetic masks do not repaint the
biological Lucario surface.

The 2026-08-31 Blockbench 5.1.6 experiment showed that a materialized poly_mesh
could survive structural validation yet be silently omitted by the Bedrock codec.
Therefore poly_mesh remains non-production until an external viewer/runtime test
proves otherwise.

Presentation only. AutoPTU/Ouros remains authoritative for battle facts.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "tools/cobblemon-model-review/v41/lucario_v41_scene_seed.json"
CURRENT_MODEL = ROOT / "fabric-adapter/src/main/resources/assets/cobblemon/bedrock/pokemon/models/0448_lucario/ouros_aura_sentinel_lucario.geo.json"
BODY = ROOT / "fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/0448_lucario/ouros_aura_sentinel.png"
OFFICIAL_BONES = 87
OFFICIAL_MODEL_SHA256 = "ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9"
OFFICIAL_NORMAL_SHA256 = "98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a"


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def parse_sprite_masks(raw: object, palette_slots: set[int]) -> dict[str, dict]:
    if raw is None:
        return {}
    if not isinstance(raw, dict):
        raise SystemExit("spriteMasks must be an object")
    parsed: dict[str, dict] = {}
    for name, spec in raw.items():
        if not isinstance(name, str) or not name:
            raise SystemExit("spriteMasks contains an invalid name")
        if not isinstance(spec, dict):
            raise SystemExit(f"spriteMasks.{name}: expected object")
        rows = spec.get("pixels")
        legend = spec.get("legend")
        if not isinstance(rows, list) or not rows or any(not isinstance(row, str) for row in rows):
            raise SystemExit(f"spriteMasks.{name}.pixels must be non-empty string rows")
        width = len(rows[0])
        if width < 2 or any(len(row) != width for row in rows):
            raise SystemExit(f"spriteMasks.{name}: rows must have one width >= 2")
        if len(rows) < 2:
            raise SystemExit(f"spriteMasks.{name}: sprite height must be >= 2")
        if not isinstance(legend, dict) or not legend:
            raise SystemExit(f"spriteMasks.{name}.legend must be non-empty")
        parsed_legend: dict[str, int] = {}
        for char, slot in legend.items():
            if not isinstance(char, str) or len(char) != 1 or char == ".":
                raise SystemExit(f"spriteMasks.{name}: legend keys must be one non-dot character")
            if not isinstance(slot, int) or slot not in palette_slots:
                raise SystemExit(f"spriteMasks.{name}: legend slot {slot!r} is not declared in palette")
            parsed_legend[char] = slot
        unknown = {char for row in rows for char in row if char != "." and char not in parsed_legend}
        if unknown:
            raise SystemExit(f"spriteMasks.{name}: unknown sprite characters {sorted(unknown)!r}")
        parsed[name] = {
            "pixels": rows,
            "legend": parsed_legend,
            "width": width,
            "height": len(rows),
        }
    return parsed


def allocate_sprite_regions(
    base_texture: Path,
    sprites: dict[str, dict],
    reserved: set[tuple[int, int]],
) -> dict[str, dict[str, int]]:
    if not sprites:
        return {}
    image = Image.open(base_texture).convert("RGBA")
    width, height = image.size
    alpha = image.getchannel("A")
    occupied = set(reserved)
    placements: dict[str, dict[str, int]] = {}

    # Larger masks allocate first. Ties remain deterministic by name.
    ordered = sorted(
        sprites.items(),
        key=lambda item: (-(item[1]["width"] * item[1]["height"]), item[0]),
    )
    for name, spec in ordered:
        sw, sh = spec["width"], spec["height"]
        found: tuple[int, int] | None = None
        # Prefer lower atlas rows because the official model leaves substantial
        # transparent accessory-safe space there; all candidates are still
        # verified pixel-by-pixel against baseline alpha.
        for y in range(height - sh, -1, -1):
            for x in range(0, width - sw + 1):
                coords = [(x + dx, y + dy) for dy in range(sh) for dx in range(sw)]
                if any(coord in occupied for coord in coords):
                    continue
                if any(alpha.getpixel(coord) != 0 for coord in coords):
                    continue
                found = (x, y)
                occupied.update(coords)
                break
            if found is not None:
                break
        if found is None:
            raise SystemExit(
                f"spriteMasks.{name}: no {sw}x{sh} fully transparent atlas region exists in {base_texture}"
            )
        placements[name] = {"x": found[0], "y": found[1], "width": sw, "height": sh}
    return placements


def write_accessory_overlay(
    path: Path,
    palette: dict[str, list[int]],
    sprites: dict[str, dict],
    placements: dict[str, dict[str, int]],
) -> None:
    width, height = Image.open(BODY).size
    image = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    pixels = image.load()

    # Existing 1px palette slots remain available for ordinary solid cosmetic faces.
    for raw_slot, rgba in palette.items():
        slot = int(raw_slot)
        if not 0 <= slot < width:
            raise SystemExit(f"palette slot out of range: {slot}")
        if not isinstance(rgba, list) or len(rgba) != 4 or any(not isinstance(v, int) or not 0 <= v <= 255 for v in rgba):
            raise SystemExit(f"invalid RGBA palette value for slot {slot}: {rgba!r}")
        pixels[slot, height - 1] = tuple(rgba)

    for name, spec in sprites.items():
        rect = placements[name]
        for dy, row in enumerate(spec["pixels"]):
            for dx, char in enumerate(row):
                if char == ".":
                    continue
                rgba = palette[str(spec["legend"][char])]
                pixels[rect["x"] + dx, rect["y"] + dy] = tuple(rgba)

    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG", optimize=True)


def solid_uv(slot: int) -> dict:
    face = {"uv": [slot, 63], "uv_size": [1, 1]}
    return {name: dict(face) for name in ("north", "east", "south", "west", "up", "down")}


def sprite_uv(rect: dict[str, int], size: list[float], where: str) -> dict:
    zeros = [axis for axis, value in enumerate(size) if abs(float(value)) <= 1e-9]
    if len(zeros) != 1:
        raise SystemExit(f"{where}: uvSprite requires exactly one zero-size plane axis")
    face = {
        "uv": [rect["x"], rect["y"]],
        "uv_size": [rect["width"], rect["height"]],
    }
    axis = zeros[0]
    if axis == 0:
        return {"east": dict(face), "west": dict(face)}
    if axis == 1:
        return {"up": dict(face), "down": dict(face)}
    return {"north": dict(face), "south": dict(face)}


def face_normal(positions: list[list[float]], vertices: list[int], where: str) -> list[float]:
    a = positions[vertices[0]]
    b = positions[vertices[1]]
    c = positions[vertices[2]]
    ab = [b[i] - a[i] for i in range(3)]
    ac = [c[i] - a[i] for i in range(3)]
    cross = [
        ab[1] * ac[2] - ab[2] * ac[1],
        ab[2] * ac[0] - ab[0] * ac[2],
        ab[0] * ac[1] - ab[1] * ac[0],
    ]
    length = math.sqrt(sum(v * v for v in cross))
    if length <= 1e-8:
        raise SystemExit(f"{where}: degenerate poly_mesh face")
    return [round(v / length, 6) for v in cross]


def build_poly_mesh(raw: dict, palette_slots: set[int], name: str) -> dict:
    positions_raw = raw.get("positions")
    faces = raw.get("faces")
    if not isinstance(positions_raw, list) or len(positions_raw) < 3:
        raise SystemExit(f"{name}.polyMesh: positions must contain at least 3 vertices")
    positions: list[list[float]] = []
    for index, position in enumerate(positions_raw):
        if not isinstance(position, list) or len(position) != 3:
            raise SystemExit(f"{name}.polyMesh.positions[{index}]: expected 3 numbers")
        positions.append([float(value) for value in position])
    if not isinstance(faces, list) or not faces:
        raise SystemExit(f"{name}.polyMesh: faces must be non-empty")

    double_sided = raw.get("doubleSided", True)
    if not isinstance(double_sided, bool):
        raise SystemExit(f"{name}.polyMesh.doubleSided must be boolean")

    normals: list[list[float]] = []
    uvs: list[list[float]] = []
    polys: list[list[list[int]]] = []
    for face_index, face in enumerate(faces):
        where = f"{name}.polyMesh.faces[{face_index}]"
        if not isinstance(face, dict):
            raise SystemExit(f"{where}: face must be an object")
        vertices = face.get("vertices")
        slot = face.get("uvSlot")
        if not isinstance(vertices, list) or len(vertices) not in (3, 4):
            raise SystemExit(f"{where}: vertices must contain 3 or 4 indices")
        if len(set(vertices)) != len(vertices) or any(not isinstance(v, int) or not 0 <= v < len(positions) for v in vertices):
            raise SystemExit(f"{where}: invalid or duplicate vertex index")
        if not isinstance(slot, int) or slot not in palette_slots:
            raise SystemExit(f"{where}: uvSlot {slot!r} is not declared in palette")
        normal = face_normal(positions, vertices, where)
        normal_index = len(normals)
        normals.append(normal)
        uv_index = len(uvs)
        uvs.append([slot + 0.5, 63.5])
        polys.append([[vertex, normal_index, uv_index] for vertex in vertices])
        if double_sided:
            reverse_normal_index = len(normals)
            normals.append([-value for value in normal])
            polys.append([[vertex, reverse_normal_index, uv_index] for vertex in reversed(vertices)])

    return {
        "normalized_uvs": False,
        "positions": positions,
        "normals": normals,
        "uvs": uvs,
        "polys": polys,
    }


def build_bone(
    raw: dict,
    palette_slots: set[int],
    sprite_placements: dict[str, dict[str, int]],
) -> dict:
    name = raw.get("name")
    parent = raw.get("parent")
    pivot = raw.get("pivot")
    elements = raw.get("elements", [])
    mesh_raw = raw.get("polyMesh")
    if not isinstance(name, str) or not name.startswith("ouros_v41_"):
        raise SystemExit(f"invalid V41 bone name: {name!r}")
    if not isinstance(parent, str) or not parent:
        raise SystemExit(f"{name}: parent is required")
    if not isinstance(pivot, list) or len(pivot) != 3:
        raise SystemExit(f"{name}: pivot must contain 3 numbers")
    if not isinstance(elements, list):
        raise SystemExit(f"{name}: elements must be a list")
    if not elements and mesh_raw is None:
        raise SystemExit(f"{name}: requires cubes/planes and/or polyMesh")

    cubes = []
    for index, element in enumerate(elements):
        where = f"{name}.elements[{index}]"
        origin = element.get("origin")
        size = element.get("size")
        slot = element.get("uvSlot")
        sprite_name = element.get("uvSprite")
        if not isinstance(origin, list) or len(origin) != 3:
            raise SystemExit(f"{where}: origin must contain 3 numbers")
        if not isinstance(size, list) or len(size) != 3:
            raise SystemExit(f"{where}: size must contain 3 numbers")
        numeric_size = [float(v) for v in size]
        if any(v < 0 for v in numeric_size) or all(v <= 1e-9 for v in numeric_size):
            raise SystemExit(f"{where}: size values must be non-negative and not all zero")
        zero_axes = sum(1 for v in numeric_size if abs(v) <= 1e-9)
        if zero_axes > 1:
            raise SystemExit(f"{where}: only one zero-size axis is allowed for a Bedrock plane")

        if sprite_name is not None:
            if slot is not None:
                raise SystemExit(f"{where}: use uvSlot or uvSprite, not both")
            if not isinstance(sprite_name, str) or sprite_name not in sprite_placements:
                raise SystemExit(f"{where}: unknown uvSprite {sprite_name!r}")
            cube_uv = sprite_uv(sprite_placements[sprite_name], numeric_size, where)
        else:
            if not isinstance(slot, int) or slot not in palette_slots:
                raise SystemExit(f"{where}: uvSlot {slot!r} is not declared in palette")
            cube_uv = solid_uv(slot)

        cube = {"origin": origin, "size": size, "uv": cube_uv}
        element_pivot = element.get("pivot")
        rotation = element.get("rotation")
        if element_pivot is not None:
            if not isinstance(element_pivot, list) or len(element_pivot) != 3:
                raise SystemExit(f"{where}: pivot must contain 3 numbers")
            cube["pivot"] = element_pivot
        if rotation is not None:
            if not isinstance(rotation, list) or len(rotation) != 3:
                raise SystemExit(f"{where}: rotation must contain 3 numbers")
            cube["rotation"] = rotation
        cubes.append(cube)

    bone = {"name": name, "parent": parent, "pivot": pivot}
    if cubes:
        bone["cubes"] = cubes
    if mesh_raw is not None:
        if not isinstance(mesh_raw, dict):
            raise SystemExit(f"{name}.polyMesh must be an object")
        bone["poly_mesh"] = build_poly_mesh(mesh_raw, palette_slots, name)
    return bone


def load_base() -> tuple[dict, list[dict], int]:
    payload = json.loads(CURRENT_MODEL.read_text(encoding="utf-8"))
    geometries = payload.get("minecraft:geometry")
    if not isinstance(geometries, list) or len(geometries) != 1:
        raise SystemExit("current Lucario model must contain exactly one geometry")
    geometry = geometries[0]
    bones = geometry.get("bones")
    if not isinstance(bones, list) or len(bones) < OFFICIAL_BONES:
        raise SystemExit("current Lucario model does not contain the 87-bone official prefix")
    official = bones[:OFFICIAL_BONES]
    historical = bones[OFFICIAL_BONES:]
    if any(str(bone.get("name", "")).startswith("ouros_v41_") for bone in historical):
        raise SystemExit("refusing to seed V41 from a prior V41 cosmetic result")
    if any(not str(bone.get("name", "")).startswith("ouros_") for bone in historical):
        raise SystemExit("historical suffix contains a non-Ouros bone; official prefix boundary is unsafe")
    return payload, official, len(historical)


def write_manifest(
    path: Path,
    candidate: Path,
    overlay: Path,
    bone_count: int,
    cube_count: int,
    mesh_count: int,
) -> None:
    data = {
        "format": "ouros.cobblemon-professional-skin-review.v1",
        "species": "lucario",
        "nationalDex": 448,
        "concept": "Blue/White Maid Lucario V41 — alpha-cloth preview",
        "authorityBoundary": "PRESENTATION_ONLY_AUTOPTU_AUTHORITATIVE",
        "artStatus": "USER REJECTED — REWORK REQUIRED",
        "ownerApproval": {
            "required": True,
            "approved": False,
            "approvedHeadSha": None,
            "evidenceSetSha256": None,
            "approvalRecord": None,
        },
        "referenceDossier": "docs/cobblemon-skin-reference-dossiers/0448_lucario.json",
        "officialSource": {
            "modrinthProjectId": "MdwFAVRL",
            "modrinthVersionId": "kF7CvxTo",
            "version": "1.7.3",
            "minecraftVersion": "1.21.1",
            "loader": "fabric",
            "jarFilename": "Cobblemon-fabric-1.7.3+1.21.1.jar",
            "jarSha256": "f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3",
            "jarSha512": "7b5376f5f48177db53790237b6fb25378806972b5d3b756151b4d8f2d3c27238d6b587b77da422bc1780bfd358b4702e74369fd82cef2a35301b4b68a2f13c2e",
            "releaseChannel": "release",
            "enforceLatestCompatibleStable": True,
            "modelPath": "assets/cobblemon/bedrock/pokemon/models/0448_lucario/lucario.geo.json",
            "modelSha256": OFFICIAL_MODEL_SHA256,
            "officialBoneCount": OFFICIAL_BONES,
            "referenceTexture": {
                "path": "assets/cobblemon/textures/pokemon/0448_lucario/lucario.png",
                "sha256": OFFICIAL_NORMAL_SHA256,
            },
            "animationPath": "assets/cobblemon/bedrock/pokemon/animations/0448_lucario/lucario.animation.json",
            "animationSha256": "ddf880b0830d7649f8cd8811c1c7e2b7fcdee156c850bbeb398f064995fa8563",
            "auxiliaryAssets": [
                {"role": "POSER", "path": "assets/cobblemon/bedrock/pokemon/posers/0448_lucario/lucario.json", "sha256": "7cd9642b38fd1c3e2518cc7f30cd1ea221cac9c89e4b413551151418a4e3c07d"},
                {"role": "RESOLVER", "path": "assets/cobblemon/bedrock/pokemon/resolvers/0448_lucario/0_lucario_base.json", "sha256": "a1785270f9f21378e6287b30e3e309de4daa348f21e33fcb8a8b03a134508e81"},
                {"role": "MODEL_LICENSE", "path": "assets/cobblemon/bedrock/pokemon/models/0448_lucario/license", "sha256": "fb8e971d1895863ec9fc5f3cfc526c64af980bd6c93d0a1615c7969df46a6660"},
            ],
        },
        "production": {
            "modelPath": candidate.relative_to(ROOT).as_posix(),
            "modelSha256": sha256(candidate),
            "productionBoneCount": OFFICIAL_BONES + bone_count,
            "cosmeticBoneCount": bone_count,
            "cosmeticCubeCount": cube_count,
            "cosmeticPolyMeshCount": mesh_count,
            "attachmentGate": {"anchorGap": 1.5, "pieceGap": 1.0},
            "textures": [
                {
                    "role": "BODY",
                    "path": BODY.relative_to(ROOT).as_posix(),
                    "sha256": OFFICIAL_NORMAL_SHA256,
                    "derivation": "OFFICIAL_IDENTICAL",
                    "officialBaselineSha256": OFFICIAL_NORMAL_SHA256,
                },
                {
                    "role": "OVERLAY",
                    "path": overlay.relative_to(ROOT).as_posix(),
                    "sha256": sha256(overlay),
                    "derivation": "ACCESSORY_OVERLAY_ALPHA_MASKED",
                },
            ],
            "runtimeAssets": [],
        },
        "blockbench": {
            "version": "5.1.6",
            "appImageSha256": "c6dd92036f3c10495df53911a74e5b00a1d557ea13e506084177ef55a5cd7c0e",
            "matchedCamera": True,
            "gameplayResolution": 160,
            "heroAnimation": "animation.lucario.ground_idle",
            "heroAnimationTime": 0.35,
            "battleAnimation": "animation.lucario.battle_idle",
            "battleAnimationTime": 0.35,
            "requiredEvidenceNames": [
                "official_reference_three_quarter.png",
                "hero_three_quarter.png",
                "battle_ready_three_quarter.png",
                "hero_front.png",
                "hero_back.png",
                "hero_gameplay_160.png",
            ],
            "technicalVisualFloor": {
                "minimumPixelDifferenceRatio": 0.08,
                "minimumSilhouetteDeltaRatio": 0.04,
            },
        },
        "evidence": {
            "artifactName": "lucario-v41-cloth-flow-blockbench-preview",
            "reviewContractFile": "review-contract.json",
            "pngHashManifestFile": "png-sha256.txt",
            "requiredFiles": [
                "official_reference_three_quarter.png",
                "hero_three_quarter.png",
                "battle_ready_three_quarter.png",
                "hero_front.png",
                "hero_back.png",
                "hero_gameplay_160.png",
                "contact_sheet.png",
                "review-contract.json",
                "png-sha256.txt",
            ],
        },
    }
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--workdir", type=Path, default=Path(".v41-preview"))
    args = parser.parse_args()
    workdir = args.workdir if args.workdir.is_absolute() else ROOT / args.workdir
    workdir.mkdir(parents=True, exist_ok=True)

    source = json.loads(SOURCE.read_text(encoding="utf-8"))
    if source.get("species") != "lucario" or source.get("nationalDex") != 448:
        raise SystemExit("V41 scene seed is not Lucario #448")
    if source.get("authoringState") != "PREVIEW_SCENE_SEED_NOT_BLOCKBENCH_AUTHORED":
        raise SystemExit("preview source must not claim Blockbench authorship")
    baseline = source.get("officialBaseline", {})
    if baseline.get("modelSha256") != OFFICIAL_MODEL_SHA256 or baseline.get("officialBoneCount") != OFFICIAL_BONES:
        raise SystemExit("V41 scene seed official baseline drifted")

    palette = source.get("palette")
    if not isinstance(palette, dict) or len(palette) < 3:
        raise SystemExit("V41 scene seed palette is missing")
    palette_slots = {int(slot) for slot in palette}
    sprites = parse_sprite_masks(source.get("spriteMasks"), palette_slots)
    body_width, body_height = Image.open(BODY).size
    reserved_palette = {(slot, body_height - 1) for slot in palette_slots if 0 <= slot < body_width}
    sprite_placements = allocate_sprite_regions(BODY, sprites, reserved_palette)

    raw_bones = source.get("bones")
    if not isinstance(raw_bones, list) or not raw_bones:
        raise SystemExit("V41 scene seed has no bones")
    extras = [build_bone(raw, palette_slots, sprite_placements) for raw in raw_bones]
    names = [bone["name"] for bone in extras]
    if len(names) != len(set(names)):
        raise SystemExit("V41 scene seed contains duplicate bone names")

    payload, official, historical_count = load_base()
    geometry = payload["minecraft:geometry"][0]
    geometry["description"]["identifier"] = "geometry.ouros_lucario_v41_alpha_cloth_preview"
    geometry["bones"] = official + extras

    candidate = workdir / "lucario_v41_preview.geo.json"
    candidate.write_text(json.dumps(payload, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    overlay = workdir / "lucario_v41_accessories.png"
    write_accessory_overlay(overlay, palette, sprites, sprite_placements)
    cube_count = sum(len(bone.get("cubes", [])) for bone in extras)
    plane_count = sum(
        1
        for bone in extras
        for cube in bone.get("cubes", [])
        if sum(1 for value in cube.get("size", []) if abs(float(value)) <= 1e-9) == 1
    )
    mesh_count = sum(1 for bone in extras if "poly_mesh" in bone)
    manifest = workdir / "0448_lucario.json"
    write_manifest(manifest, candidate, overlay, len(extras), cube_count, mesh_count)

    report = {
        "status": "MATERIALIZED_FOR_BLOCKBENCH_PREVIEW",
        "productionCandidate": False,
        "ownerReviewEligible": False,
        "source": SOURCE.relative_to(ROOT).as_posix(),
        "historicalCosmeticBonesRemoved": historical_count,
        "officialBonesPreserved": len(official),
        "v41CosmeticBones": len(extras),
        "v41CosmeticCubesAndPlanes": cube_count,
        "v41AlphaMaskedPlanes": plane_count,
        "v41CosmeticPolyMeshes": mesh_count,
        "spritePlacements": sprite_placements,
        "candidateModel": candidate.relative_to(ROOT).as_posix(),
        "candidateModelSha256": sha256(candidate),
        "overlay": overlay.relative_to(ROOT).as_posix(),
        "overlaySha256": sha256(overlay),
        "manifest": manifest.relative_to(ROOT).as_posix(),
    }
    (workdir / "materialization-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
