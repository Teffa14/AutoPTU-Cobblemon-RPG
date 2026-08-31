#!/usr/bin/env python3
"""Lucario V29: owner-supplied blue/white maid reference replica.

The owner supplied a concrete rendered skin target and explicitly requested a
faithful reconstruction rather than another incremental Resonance Ronin variant.
This builder therefore discards every historical Ouros cosmetic bone and keeps
only the exact official 87-bone Lucario prefix before adding a full-body costume
that follows the visible target: tall stepped white headpiece, blue side ribbons,
blue bow with silver clasp, white fitted bodice/sleeves, blue cuffs, dark gloves,
wide white apron/skirt with blue side panels and trim, and dark lower-leg/boot
coverage.

Presentation only. AutoPTU/Ouros remains authoritative for combatants, legality,
HP/status, positions, RNG, damage and tactical outcomes.
"""
from __future__ import annotations

import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V22_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v22.py"
spec = importlib.util.spec_from_file_location("resonance_v22", V22_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V22 texture/provenance pipeline")
v22 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v22)
v1 = v22.v1

NORMAL_META = ROOT / "docs/cobblemon-skins/0448_lucario/v29-reference-derived-normal.json"
SHINY_META = ROOT / "docs/cobblemon-skins/0448_lucario/v29-reference-derived-shiny.json"
v22.NORMAL_META = NORMAL_META
v22.SHINY_META = SHINY_META

v1.PALETTE.update({
    80: (21, 23, 31, 255),
    81: (39, 42, 53, 255),
    82: (4, 60, 101, 255),
    83: (10, 91, 142, 255),
    84: (65, 78, 181, 255),
    85: (142, 153, 191, 255),
    86: (194, 200, 215, 255),
    87: (236, 239, 244, 255),
})

def C(origin, size, color, *, pivot=None, rotation=None, inflate=None, mirror=None):
    return v1.cube(origin, size, color, pivot=pivot, rotation=rotation,
                   inflate=inflate, mirror=mirror)

def cosmetic_bones() -> list[dict]:
    hat = v1.bone("ouros_v29_reference_headpiece", "head_angle", [0, 39.2, -0.2], [
        C((-4.65, 38.55, -2.45), (9.30, 1.05, 5.35), 84),
        C((-4.35, 39.10, -2.20), (8.70, 1.15, 5.00), 87),
        C((-4.00, 40.15, -1.95), (8.00, 2.55, 4.65), 86),
        C((-3.65, 42.35, -1.70), (7.30, 2.35, 4.25), 87),
        C((-3.10, 44.40, -1.35), (6.20, 2.45, 3.70), 86),
        C((-2.65, 46.55, -1.05), (5.30, 1.55, 3.20), 87),
    ])
    head_ribbons = v1.bone("ouros_v29_reference_head_ribbons", "head_angle", [0, 39.0, 1.0], [
        C((-4.45, 38.10, 0.40), (1.25, 4.10, 2.10), 83, pivot=(-3.80, 39.80, 1.45), rotation=(0, -8, 14)),
        C((-4.15, 41.05, 0.85), (1.10, 3.00, 1.75), 82, pivot=(-3.55, 42.10, 1.70), rotation=(0, -10, 22)),
        C((3.20, 38.45, 0.55), (1.20, 3.50, 1.95), 83, pivot=(3.80, 39.85, 1.50), rotation=(0, 8, -12)),
        C((3.15, 41.05, 0.95), (1.00, 2.55, 1.55), 82, pivot=(3.65, 42.05, 1.70), rotation=(0, 8, -18)),
    ])
    bow = v1.bone("ouros_v29_reference_bow", "torso3", [0, 30.5, -2.8], [
        C((-3.75, 29.35, -4.00), (3.35, 1.65, 0.72), 84, pivot=(-1.85, 30.15, -3.65), rotation=(0, 0, -18)),
        C((0.40, 29.35, -4.00), (3.35, 1.65, 0.72), 84, pivot=(1.85, 30.15, -3.65), rotation=(0, 0, 18)),
        C((-1.15, 28.55, -4.20), (2.30, 2.30, 0.85), 86, pivot=(0, 29.70, -3.78), rotation=(0, 0, 45)),
        C((-0.55, 27.40, -4.05), (1.10, 1.65, 0.55), 85),
    ])
    bodice = v1.bone("ouros_v29_reference_bodice", "torso3", [0, 26.8, -0.5], [
        C((-4.15, 23.10, -3.70), (3.35, 6.45, 0.70), 87, pivot=(-2.20, 26.30, -3.35), rotation=(0, 0, -3)),
        C((0.80, 23.10, -3.70), (3.35, 6.45, 0.70), 87, pivot=(2.20, 26.30, -3.35), rotation=(0, 0, 3)),
        C((-4.25, 23.05, -2.95), (0.70, 6.35, 5.55), 86),
        C((3.55, 23.05, -2.95), (0.70, 6.35, 5.55), 86),
        C((-3.15, 23.30, 1.60), (6.30, 5.90, 0.70), 85),
        C((-2.45, 23.30, -4.05), (0.65, 2.40, 0.30), 84),
        C((1.80, 23.30, -4.05), (0.65, 2.40, 0.30), 84),
    ])
    sleeve_left = v1.bone("ouros_v29_reference_sleeve_left", "arm_left", [4.5, 29.7, -0.4], [
        C((2.10, 28.25, -1.75), (6.45, 2.90, 2.80), 87, inflate=0.04),
        C((7.95, 28.20, -2.15), (4.20, 2.95, 3.50), 86, inflate=0.03),
    ])
    sleeve_right = v1.bone("ouros_v29_reference_sleeve_right", "arm_right", [-4.5, 29.7, -0.4], [
        C((-8.55, 28.25, -1.75), (6.45, 2.90, 2.80), 87, inflate=0.04),
        C((-12.15, 28.20, -2.15), (4.20, 2.95, 3.50), 86, inflate=0.03),
    ])
    cuff_left = v1.bone("ouros_v29_reference_cuff_left", "arm_left2", [11.7, 29.6, -0.4], [
        C((11.30, 28.05, -2.30), (2.05, 3.15, 3.80), 84),
        C((12.30, 28.25, -2.55), (4.25, 2.85, 4.30), 80),
        C((15.85, 28.35, -2.70), (2.85, 2.65, 4.60), 81),
    ])
    cuff_right = v1.bone("ouros_v29_reference_cuff_right", "arm_right2", [-11.7, 29.6, -0.4], [
        C((-13.35, 28.05, -2.30), (2.05, 3.15, 3.80), 84),
        C((-16.55, 28.25, -2.55), (4.25, 2.85, 4.30), 80),
        C((-18.70, 28.35, -2.70), (2.85, 2.65, 4.60), 81),
    ])
    waist = v1.bone("ouros_v29_reference_waist", "torso", [0, 20.2, -0.2], [
        C((-6.65, 19.25, -4.10), (13.30, 1.55, 8.10), 87),
        C((-6.20, 18.55, -3.80), (12.40, 1.10, 7.55), 86),
    ])
    skirt = v1.bone("ouros_v29_reference_apron_skirt", "torso", [0, 18.8, -0.2], [
        C((-6.00, 11.00, -3.85), (12.00, 7.80, 0.70), 86, pivot=(0, 18.20, -3.50), rotation=(3, 0, 0)),
        C((-5.25, 12.00, -4.30), (10.50, 6.70, 0.72), 87, pivot=(0, 18.15, -3.90), rotation=(2, 0, 0)),
        C((-7.15, 11.55, -2.95), (2.10, 7.10, 6.20), 83, pivot=(-5.80, 18.00, 0.00), rotation=(0, 0, -5)),
        C((5.05, 11.55, -2.95), (2.10, 7.10, 6.20), 83, pivot=(5.80, 18.00, 0.00), rotation=(0, 0, 5)),
        C((-5.10, 11.15, -4.68), (10.20, 0.75, 0.35), 84),
        C((-4.35, 12.25, -4.64), (8.70, 0.55, 0.30), 85),
        C((-5.25, 10.95, -4.70), (2.20, 1.45, 0.36), 82),
        C((3.05, 10.95, -4.70), (2.20, 1.45, 0.36), 82),
        C((-5.70, 11.50, 2.85), (11.40, 7.15, 0.65), 83, pivot=(0, 18.00, 3.15), rotation=(-3, 0, 0)),
    ])
    leg_left = v1.bone("ouros_v29_reference_leg_left", "leg_left4", [3.5, 4.5, -0.1], [
        C((1.25, -2.95, -1.85), (4.50, 9.45, 3.70), 81, inflate=0.03),
    ])
    leg_right = v1.bone("ouros_v29_reference_leg_right", "leg_right4", [-3.5, 4.5, -0.1], [
        C((-5.75, -2.95, -1.85), (4.50, 9.45, 3.70), 81, inflate=0.03),
    ])
    boot_left = v1.bone("ouros_v29_reference_boot_left", "foot_left", [3.5, -1.7, -1.2], [
        C((0.70, -4.05, -4.35), (5.60, 3.65, 5.90), 80, inflate=0.03),
        C((1.00, -3.85, -4.60), (5.00, 0.70, 1.10), 81),
    ])
    boot_right = v1.bone("ouros_v29_reference_boot_right", "foot_right", [-3.5, -1.7, -1.2], [
        C((-6.30, -4.05, -4.35), (5.60, 3.65, 5.90), 80, inflate=0.03),
        C((-6.00, -3.85, -4.60), (5.00, 0.70, 1.10), 81),
    ])
    return [hat, head_ribbons, bow, bodice, sleeve_left, sleeve_right, cuff_left, cuff_right, waist, skirt, leg_left, leg_right, boot_left, boot_right]

def paint_pixel(r: int, g: int, b: int, a: int, x: int, y: int, *, shiny: bool):
    if a == 0:
        return r, g, b, a
    mx, mn = max(r, g, b), min(r, g, b)
    sat = mx - mn
    lum = (30*r + 59*g + 11*b) // 100
    cream = r > 170 and g > 135 and b < 205
    white = r > 210 and g > 210 and b > 210
    red = r > 105 and r > g*1.35 and r > b*1.35
    if cream or white or red:
        return r, g, b, a
    blue = b > r*1.15 and b > g*1.04 and sat > 20
    if blue:
        band = ((x // 8) + (y // 10)) % 3
        targets = ((23, 105, 151), (33, 119, 166), (12, 84, 130)) if shiny else ((8, 81, 126), (15, 99, 148), (4, 64, 106))
        tr, tg, tb = targets[band]
        mix = 0.68
        nr = int(r*(1-mix) + tr*mix); ng = int(g*(1-mix) + tg*mix); nb = int(b*(1-mix) + tb*mix)
        if ((2*x+y) % 29) in (0, 1): nr += 4; ng += 7; nb += 10
        return max(0,min(255,nr)), max(0,min(255,ng)), max(0,min(255,nb)), a
    if lum < 150:
        band = ((x // 10) + (y // 7)) % 3
        tr, tg, tb = ((31, 33, 42), (43, 45, 55), (24, 27, 36))[band]
        mix = 0.64
        nr = int(r*(1-mix) + tr*mix); ng = int(g*(1-mix) + tg*mix); nb = int(b*(1-mix) + tb*mix)
        if ((3*x+y) % 37) in (0, 1): nr += 5; ng += 5; nb += 7
        return max(0,min(255,nr)), max(0,min(255,ng)), max(0,min(255,nb)), a
    return r, g, b, a

def patch_metadata() -> None:
    for path in (NORMAL_META, SHINY_META):
        if not path.is_file():
            continue
        meta = json.loads(path.read_text(encoding="utf-8"))
        meta["bodyTexelRework"] = "OWNER_REFERENCE_MATERIAL_MATCH"
        meta["paletteIntent"] = "Match the owner-supplied existing skin: teal/cobalt Lucario biology and charcoal dark biology; costume whites/blues/black remain in the accessory overlay."
        meta["materialIntent"] = "Discrete block-value ramps and local highlights/occlusion; not a flat palette swap. The visible costume geometry carries the dominant transformation."
        meta["repaintRegions"] = ["existing blue biological texels", "existing dark biological texels"]
        meta["sourceReferenceSha256"] = "28da2aa76025e2c2e625eb8df60153656d6ea17289cfb2a56da62f9159e3e419"
        path.write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

def post_patch() -> None:
    data = json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["concept"] = "Owner Reference Replica — Blue/White Maid Lucario V29"
    data["artStatus"] = "ARTISTIC FAIL"
    data["ownerApproval"] = {"required": True, "approved": False, "approvedHeadSha": None, "evidenceSetSha256": None, "approvalRecord": None}
    p = data["production"]
    p["productionBoneCount"] = v1.OFFICIAL_BONES + len(cosmetic_bones())
    p["cosmeticBoneCount"] = len(cosmetic_bones())
    p["cosmeticCubeCount"] = sum(len(b.get("cubes", [])) for b in cosmetic_bones())
    b = data["builder"]
    b["scriptPath"] = "tools/cobblemon-model-review/build_lucario_owner_reference_v29.py"
    b["command"] = ["python", b["scriptPath"]]
    outputs = []
    for item in b["outputs"]:
        for old in ("v28-derived-normal.json", "v27-derived-normal.json", "v22-derived-normal.json"):
            item = item.replace(old, "v29-reference-derived-normal.json")
        for old in ("v28-derived-shiny.json", "v27-derived-shiny.json", "v22-derived-shiny.json"):
            item = item.replace(old, "v29-reference-derived-shiny.json")
        outputs.append(item)
    for path in (NORMAL_META, SHINY_META):
        rel = str(path.relative_to(ROOT)).replace("\\", "/")
        if rel not in outputs: outputs.append(rel)
    b["outputs"] = list(dict.fromkeys(outputs))
    q = data["qualityIntent"]
    q["signaturePieces"] = ["Tall stepped white headpiece with royal-blue band and side ribbon blocks", "White fitted bodice and full animated sleeves with blue cuffs, dark gloves and silver/blue bow clasp", "Wide layered white apron skirt with blue side/back panels and dark stocking/boot lower body"]
    q["macroFormPlan"] = "V29 is a clean-sheet owner-reference replica, not another Resonance Ronin increment. All V28 cosmetic geometry is discarded. Fourteen attachment-rooted costume systems cover the head, neck, torso, both arm chains, waist, skirt, both lower legs and both feet while preserving the exact official Lucario anatomy underneath."
    q["paintPlan"] = "Normal and shiny are independently derived from the exact 1.7.3 baselines. Biological blue is shifted toward the reference teal/cobalt blocks and dark biology toward charcoal with discrete value variation. Red eyes, cream spikes, UV layout, dimensions and alpha semantics remain intact. Costume uses a dedicated cool-white/blue/charcoal overlay palette."
    q["gameplayReadGoal"] = "At 160 px the model must instantly read as the supplied white-and-blue maid/housekeeper skin: tall headpiece, white sleeves/bodice, broad apron skirt, blue side panels and dark legs/boots."
    q["antiPatternsToReject"] = ["Returning to Resonance Ronin shoulder arcs, mantles or tiny accessory traces", "Flattening the owner target into a recolor without the headpiece, sleeves and apron silhouette", "Changing official biological bones or using Cobblemon battle-state authority"]
    q["iterationNote"] = "Owner supplied a concrete existing-skin render on 2026-08-30 and explicitly requested a faithful replica. Source image SHA-256: 28da2aa76025e2c2e625eb8df60153656d6ea17289cfb2a56da62f9159e3e419. Only one three-quarter render was supplied, so unseen rear/side micro-details are reconstructed conservatively while visible costume masses/colors are targeted directly. Artistic approval remains owner-only."
    data["variantCoverage"]["variants"][0]["coverage"] = "Default preserves the exact official 87-bone Lucario geometry and adds the V29 owner-reference costume plus a validated independently-derived normal texture."
    data["variantCoverage"]["variants"][1]["coverage"] = "Shiny preserves the same V29 costume geometry and derives body paint independently from the exact official 1.7.3 shiny baseline."
    v1.MANIFEST.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

def main() -> None:
    v22.cosmetic_bones = cosmetic_bones
    v22.paint_pixel = paint_pixel
    v22.main()
    patch_metadata()
    post_patch()

if __name__ == "__main__":
    main()
