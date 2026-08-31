#!/usr/bin/env python3
"""Lucario V30: owner-reference fidelity pass.

V29 proved that the supplied target can be represented as a full-body costume, but
exact Blockbench QA showed a major palette failure: the headpiece, bodice and apron
rendered cyan/teal instead of the reference's cool white/light-grey materials. V30
keeps the exact official 87-bone Lucario prefix and rebuilds only Ouros cosmetic
geometry, while replacing the inherited overlay writer with an explicit palette
that matches the supplied render more closely.

Presentation only. AutoPTU/Ouros remains authoritative for tactical battle facts.
"""
from __future__ import annotations

import importlib.util
import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V29_PATH = ROOT / "tools/cobblemon-model-review/build_lucario_owner_reference_v29.py"
spec = importlib.util.spec_from_file_location("owner_reference_v29", V29_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Lucario owner-reference V29 builder")
v29 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v29)
v1 = v29.v1

NORMAL_META = ROOT / "docs/cobblemon-skins/0448_lucario/v30-reference-derived-normal.json"
SHINY_META = ROOT / "docs/cobblemon-skins/0448_lucario/v30-reference-derived-shiny.json"
v29.NORMAL_META = NORMAL_META
v29.SHINY_META = SHINY_META
v29.v22.NORMAL_META = NORMAL_META
v29.v22.SHINY_META = SHINY_META

# Explicit accessory palette sampled/approximated from the owner-supplied render.
# 80-81 dark glove/leg material; 82-84 blue families; 85-90 white/grey families.
PALETTE = {
    80: (18, 20, 27, 255),
    81: (34, 37, 47, 255),
    82: (2, 67, 108, 255),
    83: (7, 105, 157, 255),
    84: (66, 82, 188, 255),
    85: (135, 149, 205, 255),
    86: (177, 184, 201, 255),
    87: (218, 222, 231, 255),
    88: (239, 241, 245, 255),
    89: (198, 203, 215, 255),
    90: (132, 141, 153, 255),
    91: (29, 35, 49, 255),
    92: (16, 86, 136, 255),
}


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


def C(origin, size, color, *, pivot=None, rotation=None, inflate=None, mirror=None):
    return v1.cube(origin, size, color, pivot=pivot, rotation=rotation, inflate=inflate, mirror=mirror)


def cosmetic_bones() -> list[dict]:
    # Tall white toque/headpiece. The blue band stays low around the head while the
    # white/grey mass expands upward, matching the reference instead of V29 cyan.
    hat = v1.bone("ouros_v30_reference_headpiece", "head_angle", [0, 39.0, -0.1], [
        C((-4.85, 38.10, -2.55), (9.70, 1.30, 5.80), 84),
        C((-4.55, 39.20, -2.35), (9.10, 1.00, 5.40), 88),
        C((-4.15, 40.05, -2.10), (8.30, 2.45, 4.95), 87),
        C((-4.45, 42.20, -1.90), (8.90, 2.20, 4.65), 89, pivot=(0, 43.20, 0.35), rotation=(0, 0, 3)),
        C((-4.05, 44.10, -1.65), (8.10, 2.30, 4.20), 87, pivot=(0, 45.10, 0.45), rotation=(0, 0, -3)),
        C((-3.55, 46.05, -1.30), (7.10, 1.70, 3.65), 88),
        C((-2.90, 47.45, -1.05), (5.80, 1.05, 3.10), 86),
    ])
    # The supplied render has a large visible blue ribbon/stack on the left side;
    # keep the opposite side subordinate so the silhouette stays asymmetric.
    ribbons = v1.bone("ouros_v30_reference_head_ribbons", "head_angle", [0, 40.0, 1.0], [
        C((-5.05, 37.95, 0.25), (1.55, 4.00, 2.35), 83, pivot=(-4.20, 39.70, 1.35), rotation=(0, -7, 18)),
        C((-5.35, 40.55, 0.75), (1.45, 3.55, 2.05), 82, pivot=(-4.55, 42.00, 1.65), rotation=(0, -10, 27)),
        C((-4.95, 43.15, 1.05), (1.30, 2.55, 1.70), 83, pivot=(-4.25, 44.20, 1.85), rotation=(0, -12, 34)),
        C((3.55, 38.80, 0.55), (1.00, 2.85, 1.70), 83, pivot=(4.05, 40.05, 1.45), rotation=(0, 8, -13)),
    ])
    # Reference throat/collar band immediately below the face.
    collar = v1.bone("ouros_v30_reference_collar", "neck", [0, 32.1, -0.1], [
        C((-3.15, 31.10, -2.40), (6.30, 1.15, 4.60), 82),
        C((-2.80, 31.65, -2.55), (5.60, 0.65, 4.90), 83),
    ])
    bow = v1.bone("ouros_v30_reference_bow", "torso3", [0, 30.1, -3.0], [
        C((-3.65, 29.00, -4.10), (3.20, 1.55, 0.68), 84, pivot=(-1.75, 29.75, -3.75), rotation=(0, 0, -17)),
        C((0.45, 29.00, -4.10), (3.20, 1.55, 0.68), 84, pivot=(1.75, 29.75, -3.75), rotation=(0, 0, 17)),
        C((-1.20, 28.30, -4.28), (2.40, 2.40, 0.86), 90, pivot=(0, 29.50, -3.85), rotation=(0, 0, 45)),
        C((-0.55, 27.20, -4.12), (1.10, 1.55, 0.55), 86),
    ])
    # White fitted bodice with lavender/blue seam accents, matching the target's
    # central white mass rather than V29's cyan torso.
    bodice = v1.bone("ouros_v30_reference_bodice", "torso3", [0, 26.4, -0.5], [
        C((-4.05, 23.00, -3.70), (3.25, 6.25, 0.72), 88, pivot=(-2.10, 26.10, -3.35), rotation=(0, 0, -3)),
        C((0.80, 23.00, -3.70), (3.25, 6.25, 0.72), 88, pivot=(2.10, 26.10, -3.35), rotation=(0, 0, 3)),
        C((-4.20, 23.00, -2.95), (0.72, 6.20, 5.55), 87),
        C((3.48, 23.00, -2.95), (0.72, 6.20, 5.55), 87),
        C((-3.20, 23.25, 1.55), (6.40, 5.75, 0.72), 89),
        C((-2.60, 23.45, -4.08), (0.62, 2.30, 0.30), 84),
        C((1.98, 23.45, -4.08), (0.62, 2.30, 0.30), 84),
        C((-0.35, 23.20, -4.14), (0.70, 3.10, 0.32), 85),
    ])
    sleeve_left = v1.bone("ouros_v30_reference_sleeve_left", "arm_left", [4.5, 29.7, -0.4], [
        C((2.05, 28.20, -1.78), (6.25, 2.95, 2.85), 88, inflate=0.04),
        C((7.80, 28.15, -2.10), (4.15, 3.00, 3.45), 87, inflate=0.03),
    ])
    sleeve_right = v1.bone("ouros_v30_reference_sleeve_right", "arm_right", [-4.5, 29.7, -0.4], [
        C((-8.30, 28.20, -1.78), (6.25, 2.95, 2.85), 88, inflate=0.04),
        C((-11.95, 28.15, -2.10), (4.15, 3.00, 3.45), 87, inflate=0.03),
    ])
    glove_left = v1.bone("ouros_v30_reference_glove_left", "arm_left2", [12.0, 29.4, -0.4], [
        C((11.15, 28.00, -2.30), (2.25, 3.20, 3.85), 84),
        C((12.25, 28.15, -2.60), (4.35, 2.95, 4.45), 80),
        C((15.95, 28.20, -2.80), (3.20, 2.80, 4.80), 81),
    ])
    glove_right = v1.bone("ouros_v30_reference_glove_right", "arm_right2", [-12.0, 29.4, -0.4], [
        C((-13.40, 28.00, -2.30), (2.25, 3.20, 3.85), 84),
        C((-16.60, 28.15, -2.60), (4.35, 2.95, 4.45), 80),
        C((-19.15, 28.20, -2.80), (3.20, 2.80, 4.80), 81),
        C((-19.45, 27.55, -0.80), (0.65, 2.20, 0.65), 90),
    ])
    waist = v1.bone("ouros_v30_reference_waist", "torso", [0, 20.0, -0.2], [
        C((-6.55, 19.05, -4.05), (13.10, 1.55, 8.00), 88),
        C((-6.10, 18.35, -3.75), (12.20, 1.05, 7.45), 87),
    ])
    # Broad white apron front, with blue side/back garment visible around it.
    skirt = v1.bone("ouros_v30_reference_apron_skirt", "torso", [0, 18.5, -0.2], [
        C((-6.95, 11.35, -3.00), (2.10, 7.10, 6.10), 83, pivot=(-5.75, 17.90, 0.00), rotation=(0, 0, -5)),
        C((4.85, 11.35, -3.00), (2.10, 7.10, 6.10), 83, pivot=(5.75, 17.90, 0.00), rotation=(0, 0, 5)),
        C((-5.65, 11.30, 2.80), (11.30, 7.00, 0.70), 82, pivot=(0, 17.85, 3.15), rotation=(-3, 0, 0)),
        C((-5.35, 11.15, -4.25), (10.70, 7.20, 0.66), 87, pivot=(0, 17.85, -3.90), rotation=(2, 0, 0)),
        C((-4.65, 10.55, -4.60), (9.30, 7.35, 0.58), 88, pivot=(0, 17.40, -4.25), rotation=(4, 0, 0)),
        C((-5.15, 10.65, -4.80), (10.30, 0.78, 0.34), 84),
        C((-3.95, 12.00, -4.76), (7.90, 0.55, 0.30), 85),
    ])
    leg_left = v1.bone("ouros_v30_reference_leg_left", "leg_left4", [3.5, 4.5, -0.1], [
        C((1.25, -2.95, -1.85), (4.50, 9.45, 3.70), 81, inflate=0.03),
    ])
    leg_right = v1.bone("ouros_v30_reference_leg_right", "leg_right4", [-3.5, 4.5, -0.1], [
        C((-5.75, -2.95, -1.85), (4.50, 9.45, 3.70), 81, inflate=0.03),
    ])
    boot_left = v1.bone("ouros_v30_reference_boot_left", "foot_left", [3.5, -1.7, -1.2], [
        C((0.70, -4.05, -4.35), (5.60, 3.65, 5.90), 80, inflate=0.03),
        C((1.00, -3.85, -4.60), (5.00, 0.70, 1.10), 81),
    ])
    boot_right = v1.bone("ouros_v30_reference_boot_right", "foot_right", [-3.5, -1.7, -1.2], [
        C((-6.30, -4.05, -4.35), (5.60, 3.65, 5.90), 80, inflate=0.03),
        C((-6.00, -3.85, -4.60), (5.00, 0.70, 1.10), 81),
    ])
    return [hat, ribbons, collar, bow, bodice, sleeve_left, sleeve_right, glove_left, glove_right, waist, skirt, leg_left, leg_right, boot_left, boot_right]


def post_patch() -> None:
    data = json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["concept"] = "Owner Reference Replica — Blue/White Maid Lucario V30"
    data["artStatus"] = "ARTISTIC FAIL"
    data["ownerApproval"] = {"required": True, "approved": False, "approvedHeadSha": None, "evidenceSetSha256": None, "approvalRecord": None}
    p = data["production"]
    p["productionBoneCount"] = v1.OFFICIAL_BONES + len(cosmetic_bones())
    p["cosmeticBoneCount"] = len(cosmetic_bones())
    p["cosmeticCubeCount"] = sum(len(b.get("cubes", [])) for b in cosmetic_bones())
    b = data["builder"]
    b["scriptPath"] = "tools/cobblemon-model-review/build_lucario_owner_reference_v30.py"
    b["command"] = ["python", b["scriptPath"]]
    b["outputs"] = [x.replace("v29-reference-derived-normal.json", "v30-reference-derived-normal.json").replace("v29-reference-derived-shiny.json", "v30-reference-derived-shiny.json") for x in b["outputs"]]
    q = data["qualityIntent"]
    q["signaturePieces"] = [
        "Tall cool-white stepped headpiece with low royal-blue band and asymmetric blue side ribbon",
        "White fitted bodice and full white sleeves with blue cuffs, oversized charcoal gloves and silver/blue chest clasp",
        "Wide layered white apron front over blue side/back skirt, with dark stocking legs and block boots",
    ]
    q["macroFormPlan"] = "V30 keeps the owner-reference full-body architecture but fixes V29's dominant visual error: white/grey costume surfaces no longer inherit the old Resonance Ronin cyan palette. The headpiece is taller/wider and primarily white, the bodice/sleeves are white, the apron dominates the skirt front, and blue is restricted to the visible band, ribbons, trim and side/back garment masses."
    q["paintPlan"] = "Body normal/shiny remain independently derived from the exact official 1.7.3 baselines. Accessory overlay is now emitted by a dedicated V30 writer with explicit charcoal, cobalt/royal-blue, lavender, cool-grey and white swatches matching the supplied render; it no longer relies on inherited Resonance Ronin palette globals."
    q["gameplayReadGoal"] = "At 160 px immediately read the supplied white-and-blue maid/housekeeper Lucario: white toque, black/red face, white sleeves and torso, broad white apron, blue side panels, dark gloves/legs/boots."
    q["iterationNote"] = "Direct V29 exact-head Blockbench QA passed technical floors (pixelDifferenceRatio 0.852941; silhouetteDeltaRatio 0.12467) but failed fidelity because inherited overlay palette resolution rendered the intended white headpiece/bodice/apron as cyan/teal. V30 replaces that palette path and retunes visible geometry against the supplied owner reference."
    data["variantCoverage"]["variants"][0]["coverage"] = "Default preserves the exact official 87-bone Lucario geometry and adds the V30 owner-reference costume plus a validated independently-derived normal texture."
    data["variantCoverage"]["variants"][1]["coverage"] = "Shiny preserves the same V30 costume geometry and derives body paint independently from the exact official 1.7.3 shiny baseline."
    v1.MANIFEST.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    # Replace V29 geometry and the inherited overlay writer before invoking the
    # already-validated official-source / derived-texture pipeline.
    v29.cosmetic_bones = cosmetic_bones
    v29.v22.write_overlay = write_overlay
    v29.main()
    post_patch()


if __name__ == "__main__":
    main()
