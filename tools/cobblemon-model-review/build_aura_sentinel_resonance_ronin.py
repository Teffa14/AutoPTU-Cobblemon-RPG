#!/usr/bin/env python3
"""Build the Lucario Aura Sentinel V5 "Resonance Ronin" candidate.

This is a presentation-only cosmetic builder. It deliberately discards every
historical Ouros cosmetic bone and preserves only the immutable 87-bone official
Lucario prefix before authoring a new, non-portal, whole-body silhouette.

The builder never owns battle state. AutoPTU/Ouros remains authoritative for
combatants, legality, HP/status, tactical positions, RNG, damage and outcomes.
"""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MODEL = ROOT / "fabric-adapter/src/main/resources/assets/cobblemon/bedrock/pokemon/models/0448_lucario/ouros_aura_sentinel_lucario.geo.json"
BODY = ROOT / "fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/0448_lucario/ouros_aura_sentinel.png"
SHINY = ROOT / "fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/0448_lucario/ouros_aura_sentinel_shiny.png"
OVERLAY = ROOT / "fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/0448_lucario/ouros_aura_sentinel_accessories.png"
RESOLVER = ROOT / "fabric-adapter/src/main/resources/assets/cobblemon/bedrock/pokemon/resolvers/0448_lucario/90_ouros_aura_sentinel.json"
MANIFEST = ROOT / "docs/cobblemon-skin-review-manifests/0448_lucario.json"

OFFICIAL_BONES = 87
OFFICIAL_MODEL_SHA256 = "ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9"
OFFICIAL_NORMAL_SHA256 = "98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a"
OFFICIAL_SHINY_SHA256 = "b87aaef14b35139b43446e1a85f7031a9594c5443a6a99c03e36e77cab75e84d"

PALETTE = {
    80: (19, 24, 43, 255),      # midnight
    81: (48, 43, 91, 255),      # indigo
    82: (64, 88, 133, 255),     # blue steel
    83: (194, 210, 224, 255),   # silver
    84: (224, 174, 73, 255),    # antique gold
    85: (63, 221, 235, 255),    # aura cyan
    86: (151, 94, 216, 255),    # amethyst
    87: (236, 230, 211, 255),   # ivory
}


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def uv(x: int) -> dict:
    face = {"uv": [x, 63], "uv_size": [1, 1]}
    return {name: dict(face) for name in ("north", "east", "south", "west", "up", "down")}


def cube(origin, size, color, *, pivot=None, rotation=None, inflate=None, mirror=None):
    out = {"origin": list(origin), "size": list(size), "uv": uv(color)}
    if pivot is not None:
        out["pivot"] = list(pivot)
    if rotation is not None:
        out["rotation"] = list(rotation)
    if inflate is not None:
        out["inflate"] = inflate
    if mirror is not None:
        out["mirror"] = mirror
    return out


def bone(name: str, parent: str, pivot, cubes) -> dict:
    return {"name": name, "parent": parent, "pivot": list(pivot), "cubes": cubes}


def png_chunk(kind: bytes, payload: bytes) -> bytes:
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)


def write_overlay(path: Path) -> None:
    width, height = 128, 64
    pixels = bytearray(width * height * 4)
    for x, rgba in PALETTE.items():
        idx = ((63 * width) + x) * 4
        pixels[idx:idx + 4] = bytes(rgba)
    raw = bytearray()
    stride = width * 4
    for y in range(height):
        raw.append(0)
        raw.extend(pixels[y * stride:(y + 1) * stride])
    payload = b"\x89PNG\r\n\x1a\n"
    payload += png_chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    payload += png_chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    payload += png_chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(payload)


def resonance_bones() -> list[dict]:
    # 1) Open circlet. It hugs the head, leaves the muzzle/eyes unobstructed and
    # avoids the rejected deep-cowl / rectangular-frame silhouette.
    circlet = bone("ouros_resonance_circlet", "head_angle", [0, 38.0, -1.6], [
        cube((-4.25, 37.8, -4.82), (3.2, 0.55, 0.30), 84, pivot=(-2.65, 38.1, -4.67), rotation=(0, 0, -8)),
        cube((1.05, 37.8, -4.82), (3.2, 0.55, 0.30), 84, pivot=(2.65, 38.1, -4.67), rotation=(0, 0, 8)),
        cube((-4.15, 38.25, -2.85), (0.62, 2.15, 2.25), 81, pivot=(-3.84, 39.32, -1.72), rotation=(0, 0, -11)),
        cube((3.53, 38.25, -2.85), (0.62, 2.15, 2.25), 81, pivot=(3.84, 39.32, -1.72), rotation=(0, 0, 11)),
        cube((-3.65, 39.6, -1.35), (7.30, 0.55, 3.40), 80),
        cube((-2.85, 40.0, 1.55), (5.70, 0.45, 1.75), 82),
        cube((-0.65, 40.18, 2.65), (1.30, 3.70, 0.65), 86, pivot=(0, 40.4, 2.95), rotation=(0, 0, 12)),
        cube((-0.42, 43.35, 2.75), (0.84, 2.10, 0.42), 85, pivot=(0, 43.4, 2.96), rotation=(0, 0, -14)),
        cube((-5.05, 38.75, -1.1), (1.30, 1.45, 2.25), 83, pivot=(-4.4, 39.47, 0.0), rotation=(0, 0, -18)),
        cube((3.75, 38.75, -1.1), (1.30, 1.45, 2.25), 83, pivot=(4.4, 39.47, 0.0), rotation=(0, 0, 18)),
        cube((-3.0, 39.45, 2.95), (6.0, 0.35, 0.30), 85),
    ])

    # 2) Asymmetric shawl. This is the primary torso silhouette: layered cloth
    # and plate masses wrap the shoulders instead of forming a portal/cage.
    shawl = bone("ouros_resonance_shawl", "torso3", [0, 30.0, 0.0], [
        cube((-8.3, 28.6, -3.0), (9.1, 2.75, 6.6), 80, pivot=(-3.75, 30.0, 0.3), rotation=(0, 0, -7)),
        cube((0.0, 29.0, -2.75), (7.0, 2.10, 6.0), 81, pivot=(3.5, 30.05, 0.25), rotation=(0, 0, 6)),
        cube((-10.2, 29.25, -2.2), (3.2, 3.55, 5.1), 82, pivot=(-8.6, 31.0, 0.35), rotation=(0, 0, -14)),
        cube((-11.45, 30.65, -1.6), (2.7, 2.45, 4.3), 86, pivot=(-10.1, 31.85, 0.55), rotation=(0, 0, -20)),
        cube((5.7, 29.4, -2.0), (3.0, 2.55, 4.6), 82, pivot=(7.2, 30.65, 0.3), rotation=(0, 0, 10)),
        cube((-6.2, 31.0, 2.6), (12.4, 2.15, 1.65), 80),
        cube((-5.15, 32.55, 2.9), (10.3, 0.55, 1.15), 84),
        cube((-8.8, 32.0, 1.9), (3.25, 2.55, 2.05), 83, pivot=(-7.18, 33.27, 2.92), rotation=(0, 0, -10)),
        cube((5.35, 31.9, 2.05), (2.9, 1.95, 1.95), 83, pivot=(6.8, 32.88, 3.0), rotation=(0, 0, 9)),
        cube((-1.0, 28.5, -3.75), (2.0, 4.75, 0.42), 85, pivot=(0, 30.8, -3.54), rotation=(0, 0, 26)),
    ])

    # 3) Tapered cuirass wrapping the chest spike instead of boxing it in.
    cuirass = bone("ouros_resonance_cuirass", "torso3", [0, 27.7, -3.0], [
        cube((-4.75, 24.6, -4.15), (3.45, 5.55, 0.55), 81, pivot=(-3.02, 27.35, -3.88), rotation=(0, 0, -6)),
        cube((1.30, 24.6, -4.15), (3.45, 5.55, 0.55), 81, pivot=(3.02, 27.35, -3.88), rotation=(0, 0, 6)),
        cube((-4.45, 24.5, -3.65), (0.75, 5.8, 5.75), 80),
        cube((3.70, 24.5, -3.65), (0.75, 5.8, 5.75), 80),
        cube((-3.55, 29.75, -4.38), (2.15, 0.55, 0.35), 84, pivot=(-2.48, 30.02, -4.2), rotation=(0, 0, -10)),
        cube((1.40, 29.75, -4.38), (2.15, 0.55, 0.35), 84, pivot=(2.48, 30.02, -4.2), rotation=(0, 0, 10)),
        cube((-3.1, 25.2, -4.42), (1.1, 3.65, 0.30), 82, pivot=(-2.55, 27.02, -4.27), rotation=(0, 0, -12)),
        cube((2.0, 25.2, -4.42), (1.1, 3.65, 0.30), 82, pivot=(2.55, 27.02, -4.27), rotation=(0, 0, 12)),
        cube((-1.55, 25.25, -4.55), (3.1, 0.42, 0.25), 85),
    ])

    # 4) A sweeping resonance banner: one diagonal, attached dorsal gesture. It
    # changes the rear/three-quarter read without recreating a shrine frame.
    banner = bone("ouros_resonance_banner", "torso3", [-4.5, 31.2, 3.3], [
        cube((-6.0, 30.0, 2.85), (4.3, 3.2, 1.55), 80, pivot=(-3.85, 31.6, 3.62), rotation=(0, 0, -12)),
        cube((-8.25, 31.3, 3.0), (4.0, 3.15, 1.35), 81, pivot=(-6.25, 32.85, 3.68), rotation=(0, 0, -20)),
        cube((-10.15, 33.05, 3.1), (3.7, 2.75, 1.20), 86, pivot=(-8.3, 34.42, 3.7), rotation=(0, 0, -28)),
        cube((-11.6, 35.05, 3.2), (3.35, 2.35, 1.05), 82, pivot=(-9.92, 36.22, 3.72), rotation=(0, 0, -34)),
        cube((-8.4, 32.3, 4.08), (1.05, 3.0, 0.28), 84, pivot=(-7.88, 33.8, 4.22), rotation=(0, 0, -20)),
        cube((-10.05, 34.1, 4.15), (1.0, 2.45, 0.25), 85, pivot=(-9.55, 35.32, 4.28), rotation=(0, 0, -28)),
        cube((-11.35, 36.0, 4.18), (0.9, 1.65, 0.22), 87, pivot=(-10.9, 36.82, 4.29), rotation=(0, 0, -34)),
        cube((-5.15, 29.8, 4.0), (2.55, 0.55, 0.35), 84, pivot=(-3.88, 30.08, 4.18), rotation=(0, 0, -10)),
    ])

    # 5) Split battle coat. The lower half is fully transformed and keeps the
    # silhouette active during biped motion.
    coat = bone("ouros_resonance_coat", "torso", [0, 20.5, 1.2], [
        cube((-6.35, 19.55, -4.0), (12.7, 1.55, 7.75), 80),
        cube((-6.55, 20.8, -4.1), (13.1, 0.38, 7.95), 84),
        cube((-5.8, 11.2, 3.25), (5.25, 9.6, 0.85), 81, pivot=(-3.18, 20.0, 3.68), rotation=(-7, 0, 7)),
        cube((0.55, 12.4, 3.25), (5.25, 8.4, 0.85), 82, pivot=(3.18, 20.0, 3.68), rotation=(-7, 0, -7)),
        cube((-6.0, 12.55, -3.82), (2.05, 7.65, 0.48), 81, pivot=(-4.98, 19.4, -3.58), rotation=(0, 0, -6)),
        cube((3.95, 13.25, -3.82), (2.05, 6.95, 0.48), 82, pivot=(4.98, 19.4, -3.58), rotation=(0, 0, 6)),
        cube((-5.55, 10.95, 4.02), (4.65, 0.52, 0.25), 85, pivot=(-3.22, 11.21, 4.14), rotation=(-7, 0, 7)),
        cube((0.9, 12.15, 4.02), (4.4, 0.50, 0.25), 85, pivot=(3.1, 12.4, 4.14), rotation=(-7, 0, -7)),
        cube((-6.35, 18.35, 2.65), (5.9, 2.15, 1.55), 86),
        cube((0.45, 18.35, 2.65), (5.9, 2.15, 1.55), 82),
    ])

    left_vambrace = bone("ouros_resonance_left_vambrace", "arm_left2", [10.3, 29.4, -0.3], [
        cube((8.75, 27.45, -2.35), (3.45, 4.15, 4.05), 80),
        cube((9.0, 27.7, -2.62), (2.95, 0.42, 4.55), 84),
        cube((11.65, 28.15, -1.25), (0.5, 2.75, 1.85), 82),
        cube((9.55, 31.0, -1.85), (1.8, 1.7, 2.9), 86, pivot=(10.45, 31.42, -0.4), rotation=(0, 0, -9)),
        cube((11.85, 28.75, -0.7), (0.20, 1.35, 0.9), 85),
    ])
    right_vambrace = bone("ouros_resonance_right_vambrace", "arm_right2", [-10.3, 29.4, -0.3], [
        cube((-12.2, 27.45, -2.35), (3.45, 4.15, 4.05), 80),
        cube((-11.95, 27.7, -2.62), (2.95, 0.42, 4.55), 84),
        cube((-12.15, 28.15, -1.25), (0.5, 2.75, 1.85), 82),
        cube((-11.35, 31.0, -1.85), (1.8, 1.7, 2.9), 86, pivot=(-10.45, 31.42, -0.4), rotation=(0, 0, 9)),
        cube((-12.05, 28.75, -0.7), (0.20, 1.35, 0.9), 85),
    ])

    left_greave = bone("ouros_resonance_left_greave", "leg_left4", [3.5, 6.15, -1.5], [
        cube((1.05, -2.45, -2.05), (4.9, 8.35, 1.05), 80),
        cube((1.0, -2.35, -1.55), (1.05, 8.15, 3.35), 81),
        cube((4.95, -2.35, -1.55), (1.0, 8.15, 3.35), 82),
        cube((1.25, 5.0, -2.18), (4.5, 2.1, 1.15), 84, pivot=(3.5, 6.05, -1.55), rotation=(-8, 0, 0)),
        cube((2.05, 0.0, -2.42), (2.9, 2.9, 0.22), 85),
    ])
    right_greave = bone("ouros_resonance_right_greave", "leg_right4", [-3.5, 6.15, -1.5], [
        cube((-5.95, -2.45, -2.05), (4.9, 8.35, 1.05), 80),
        cube((-6.0, -2.35, -1.55), (1.05, 8.15, 3.35), 82),
        cube((-2.05, -2.35, -1.55), (1.0, 8.15, 3.35), 81),
        cube((-5.75, 5.0, -2.18), (4.5, 2.1, 1.15), 84, pivot=(-3.5, 6.05, -1.55), rotation=(-8, 0, 0)),
        cube((-4.95, 0.0, -2.42), (2.9, 2.9, 0.22), 85),
    ])

    # Tail guard keeps the transformation coherent in rear motion without
    # covering the entire biological tail.
    tail_guard = bone("ouros_resonance_tail_guard", "tail2", [0, 19.4, 10.0], [
        cube((-1.35, 17.45, 8.65), (2.7, 3.95, 2.0), 80),
        cube((-1.5, 17.7, 10.15), (3.0, 3.45, 1.25), 84),
        cube((-1.35, 17.85, 11.0), (2.7, 3.05, 1.15), 82),
        cube((-0.45, 18.1, 11.85), (0.9, 2.55, 0.32), 85),
    ])

    return [circlet, shawl, cuirass, banner, coat, left_vambrace, right_vambrace, left_greave, right_greave, tail_guard]


def build_model() -> tuple[dict, int]:
    source = json.loads(MODEL.read_text(encoding="utf-8"))
    geometries = source.get("minecraft:geometry")
    if not isinstance(geometries, list) or len(geometries) != 1:
        raise SystemExit("expected exactly one Lucario geometry")
    geometry = geometries[0]
    bones = geometry.get("bones")
    if not isinstance(bones, list) or len(bones) < OFFICIAL_BONES:
        raise SystemExit("Lucario seed is missing the immutable official bone prefix")
    official = copy.deepcopy(bones[:OFFICIAL_BONES])
    if any(str(entry.get("name", "")).startswith("ouros_") for entry in official):
        raise SystemExit("Ouros cosmetic bone leaked into the 87-bone official prefix")
    if any(not str(entry.get("name", "")).startswith("ouros_") for entry in bones[OFFICIAL_BONES:]):
        raise SystemExit("seed contains unexpected non-Ouros bones after the official prefix")

    candidate = copy.deepcopy(source)
    cgeo = candidate["minecraft:geometry"][0]
    cgeo["description"]["identifier"] = "geometry.ouros_aura_sentinel_lucario"
    extras = resonance_bones()
    cgeo["bones"] = official + extras
    MODEL.write_text(json.dumps(candidate, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    return candidate, sum(len(entry.get("cubes", [])) for entry in extras)


def build_resolver() -> None:
    resolver = {
        "species": "cobblemon:lucario",
        "order": 90,
        "variations": [
            {
                "aspects": ["ouros_aura_sentinel"],
                "poser": "cobblemon:lucario",
                "model": "cobblemon:ouros_aura_sentinel_lucario.geo",
                "texture": "cobblemon:textures/pokemon/0448_lucario/ouros_aura_sentinel.png",
                "layers": [{
                    "name": "ouros_aura_sentinel_resonance_ronin",
                    "texture": "cobblemon:textures/pokemon/0448_lucario/ouros_aura_sentinel_accessories.png",
                    "translucent": True,
                }],
            },
            {
                "aspects": ["ouros_aura_sentinel", "shiny"],
                "poser": "cobblemon:lucario",
                "model": "cobblemon:ouros_aura_sentinel_lucario.geo",
                "texture": "cobblemon:textures/pokemon/0448_lucario/ouros_aura_sentinel_shiny.png",
                "layers": [{
                    "name": "ouros_aura_sentinel_resonance_ronin",
                    "texture": "cobblemon:textures/pokemon/0448_lucario/ouros_aura_sentinel_accessories.png",
                    "translucent": True,
                }],
            },
        ],
    }
    RESOLVER.parent.mkdir(parents=True, exist_ok=True)
    RESOLVER.write_text(json.dumps(resolver, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")


def build_manifest(cube_count: int) -> None:
    manifest = {
        "format": "ouros.cobblemon-professional-skin-review.v1",
        "species": "lucario",
        "nationalDex": 448,
        "concept": "Aura Sentinel — Resonance Ronin",
        "authorityBoundary": "PRESENTATION_ONLY_AUTOPTU_AUTHORITATIVE",
        "artStatus": "OWNER REVIEW REQUIRED",
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
                {
                    "role": "POSER",
                    "path": "assets/cobblemon/bedrock/pokemon/posers/0448_lucario/lucario.json",
                    "sha256": "7cd9642b38fd1c3e2518cc7f30cd1ea221cac9c89e4b413551151418a4e3c07d",
                },
                {
                    "role": "RESOLVER",
                    "path": "assets/cobblemon/bedrock/pokemon/resolvers/0448_lucario/0_lucario_base.json",
                    "sha256": "a1785270f9f21378e6287b30e3e309de4daa348f21e33fcb8a8b03a134508e81",
                },
                {
                    "role": "MODEL_LICENSE",
                    "path": "assets/cobblemon/bedrock/pokemon/models/0448_lucario/license",
                    "sha256": "fb8e971d1895863ec9fc5f3cfc526c64af980bd6c93d0a1615c7969df46a6660",
                },
            ],
        },
        "production": {
            "modelPath": str(MODEL.relative_to(ROOT)).replace("\\", "/"),
            "modelSha256": sha256(MODEL),
            "productionBoneCount": OFFICIAL_BONES + len(resonance_bones()),
            "cosmeticBoneCount": len(resonance_bones()),
            "cosmeticCubeCount": cube_count,
            "attachmentGate": {"anchorGap": 1.5, "pieceGap": 1.0},
            "textures": [
                {
                    "role": "BODY",
                    "path": str(BODY.relative_to(ROOT)).replace("\\", "/"),
                    "sha256": sha256(BODY),
                    "derivation": "OFFICIAL_IDENTICAL",
                },
                {
                    "role": "OVERLAY",
                    "path": str(OVERLAY.relative_to(ROOT)).replace("\\", "/"),
                    "sha256": sha256(OVERLAY),
                    "derivation": "ACCESSORY_OVERLAY",
                },
            ],
            "runtimeAssets": [
                {
                    "role": "RESOLVER",
                    "path": str(RESOLVER.relative_to(ROOT)).replace("\\", "/"),
                    "sha256": sha256(RESOLVER),
                },
                {
                    "role": "SHINY_BODY",
                    "path": str(SHINY.relative_to(ROOT)).replace("\\", "/"),
                    "sha256": sha256(SHINY),
                },
            ],
        },
        "builder": {
            "deterministic": True,
            "scriptPath": "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin.py",
            "command": ["python", "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin.py"],
            "outputs": [
                str(MODEL.relative_to(ROOT)).replace("\\", "/"),
                str(BODY.relative_to(ROOT)).replace("\\", "/"),
                str(OVERLAY.relative_to(ROOT)).replace("\\", "/"),
                str(RESOLVER.relative_to(ROOT)).replace("\\", "/"),
                str(SHINY.relative_to(ROOT)).replace("\\", "/"),
            ],
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
            "artifactName": "lucario-aura-sentinel-resonance-ronin-professional-review",
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
        "qualityIntent": {
            "referenceLessons": [
                "Use layered parent-safe cloth masses across head and torso so a costume reads as one continuous character design, not scattered add-ons.",
                "Carry the authored material envelope through arms, legs and lower-body garment pieces so gameplay-scale motion preserves the transformation.",
                "Use overlapping shells, contour transitions and dedicated equipment material hierarchy while preserving every official biological UV and animation bone.",
            ],
            "signaturePieces": [
                "Asymmetric resonance shawl wrapping both shoulders into the torso silhouette",
                "Single sweeping dorsal resonance banner that cuts a diagonal rear silhouette",
                "Split battle coat that transforms the lower body in motion",
            ],
            "macroFormPlan": "A low open circlet, broad asymmetric shoulder shawl, tapered chest shell, one diagonal dorsal banner, split coat tails, plated forearms and plated shins create a continuous top-to-bottom silhouette without a rectangular portal or detached backpack frame.",
            "paintPlan": "Eight verified free accessory texels provide a high-contrast material hierarchy of midnight cloth, indigo, blue steel, silver, antique gold, aura cyan, amethyst and ivory; biological pixels remain byte-identical to official Lucario.",
            "gameplayReadGoal": "At 160 px the left-heavy shawl, diagonal dorsal banner, cyan/gold edge accents and long split coat must remain legible before any micro-detail is visible.",
            "antiPatternsToReject": [
                "Pokemon base plus scattered accessories",
                "Rectangular cage, shrine frame or portal silhouette",
                "Detached floating fins or equipment islands",
                "Flat recolor used as a substitute for authored geometry",
            ],
            "thirdPartyReusePolicy": "TECHNIQUES_ONLY_UNLESS_LICENSED_DERIVATIVE_DONOR",
        },
        "variantCoverage": {
            "audited": True,
            "variants": [
                {
                    "name": "default",
                    "coverage": "Cobblemon 1.7.3 uses one standard Lucario geometry for this resolver path; the candidate preserves that exact 87-bone model and official normal texture.",
                },
                {
                    "name": "shiny",
                    "coverage": "The same cosmetic geometry and overlay are routed over the exact official shiny Lucario body texture; no shiny biology is repainted.",
                },
                {
                    "name": "sex/forms",
                    "coverage": "No male/female geometry split exists on this official Lucario resolver path, and Mega Lucario is outside this cosmetic slice.",
                },
            ],
        },
        "technicalChecks": [
            "REFERENCE_DOSSIER",
            "OFFICIAL_SOURCE_HASHES",
            "ORIGINAL_BONE_EQUALITY",
            "COSMETIC_ATTACHMENT",
            "BUILDER_REPRODUCIBILITY",
            "BLOCKBENCH_MATCHED_CAMERA",
            "GAMEPLAY_SCALE_EVIDENCE",
            "PLAYABLE_TEST_BUILD",
            "INTEGRATION_CORE_CI",
        ],
    }
    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--bootstrap", action="store_true", help="also write the professional manifest with generated hashes")
    args = parser.parse_args()

    if sha256(BODY) != OFFICIAL_NORMAL_SHA256:
        raise SystemExit("normal body texture is not byte-identical to pinned official Lucario")
    if sha256(SHINY) != OFFICIAL_SHINY_SHA256:
        raise SystemExit("shiny body texture is not byte-identical to pinned official Lucario")

    _, cube_count = build_model()
    write_overlay(OVERLAY)
    build_resolver()
    if args.bootstrap:
        build_manifest(cube_count)

    print(json.dumps({
        "status": "BUILT",
        "concept": "Aura Sentinel — Resonance Ronin",
        "officialBones": OFFICIAL_BONES,
        "cosmeticBones": len(resonance_bones()),
        "cosmeticCubes": cube_count,
        "modelSha256": sha256(MODEL),
        "overlaySha256": sha256(OVERLAY),
        "resolverSha256": sha256(RESOLVER),
        "normalBodySha256": sha256(BODY),
        "shinyBodySha256": sha256(SHINY),
        "bodyTexelRework": "NONE",
    }, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
