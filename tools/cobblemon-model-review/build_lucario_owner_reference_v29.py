#!/usr/bin/env python3
"""Lucario owner-reference replica V29c.

V29 established the full-body reconstruction. V29b fixed the inherited cyan/gold
palette and exact Blockbench evidence then exposed the remaining visible mismatch:
the apron front was too blue/segmented and cosmetic boot shells replaced Lucario's
reference-like biological feet with square blocks. V29c keeps the exact official
87-bone anatomy, removes lower-leg/boot shells, darkens biological blue, makes the
white apron the dominant lower-body read, and broadens the white headpiece profile.

Presentation only. AutoPTU/Ouros remains authoritative for battle facts.
"""
from __future__ import annotations

import importlib.util
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
V22_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v22.py"
spec = importlib.util.spec_from_file_location("resonance_v22", V22_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V22 provenance pipeline")
v22 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v22)
v1 = v22.v1

NORMAL_META = ROOT / "docs/cobblemon-skins/0448_lucario/v29-reference-derived-normal.json"
SHINY_META = ROOT / "docs/cobblemon-skins/0448_lucario/v29-reference-derived-shiny.json"
v22.NORMAL_META = NORMAL_META
v22.SHINY_META = SHINY_META
REFERENCE_SHA256 = "28da2aa76025e2c2e625eb8df60153656d6ea17289cfb2a56da62f9159e3e419"

COSTUME_PALETTE = {
    80: (20, 23, 31, 255),
    81: (42, 45, 54, 255),
    82: (4, 58, 99, 255),
    83: (8, 91, 139, 255),
    84: (69, 82, 183, 255),
    85: (151, 160, 174, 255),
    86: (204, 209, 218, 255),
    87: (240, 242, 246, 255),
}


def C(origin, size, color, *, pivot=None, rotation=None, inflate=None, mirror=None):
    return v1.cube(origin, size, color, pivot=pivot, rotation=rotation,
                   inflate=inflate, mirror=mirror)


def write_owner_overlay(path: Path) -> None:
    image = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    for x, rgba in COSTUME_PALETTE.items():
        image.putpixel((x, 63), rgba)
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG", optimize=True, compress_level=9)


def cosmetic_bones() -> list[dict]:
    # Source target: broad, stacked white toque/headpiece, not a narrow pyramid.
    hat = v1.bone("ouros_v29_reference_headpiece", "head_angle", [0, 39.2, -0.2], [
        C((-4.65, 38.55, -2.45), (9.30, 1.05, 5.35), 84),
        C((-4.35, 39.10, -2.20), (8.70, 1.10, 5.00), 87),
        C((-4.20, 40.05, -2.05), (8.40, 2.25, 4.80), 86),
        C((-4.35, 42.05, -1.90), (8.70, 2.10, 4.55), 87),
        C((-4.10, 43.90, -1.65), (8.20, 2.20, 4.15), 86),
        C((-3.90, 45.85, -1.35), (7.80, 2.10, 3.70), 87),
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
        C((-3.15, 23.30, 1.60), (6.30, 5.90, 0.70), 87),
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

    # Blue under-skirt remains visible only at the sides/rear, as in the source.
    skirt = v1.bone("ouros_v29_reference_under_skirt", "torso", [0, 18.8, -0.2], [
        C((-7.15, 11.55, -2.95), (2.25, 7.10, 6.20), 83, pivot=(-5.80, 18.00, 0.00), rotation=(0, 0, -5)),
        C((4.90, 11.55, -2.95), (2.25, 7.10, 6.20), 83, pivot=(5.80, 18.00, 0.00), rotation=(0, 0, 5)),
        C((-5.70, 11.50, 2.85), (11.40, 7.15, 0.65), 83, pivot=(0, 18.00, 3.15), rotation=(-3, 0, 0)),
    ])

    # Broad front apron intentionally sits in front of the blue skirt. V29b's
    # front split/blue dominance was the largest mismatch against the supplied image.
    apron = v1.bone("ouros_v29_reference_apron_front", "torso", [0, 18.5, -4.0], [
        C((-5.25, 11.15, -5.10), (10.50, 7.55, 0.55), 87, pivot=(0, 18.10, -4.82), rotation=(2, 0, 0)),
        C((-6.05, 11.75, -5.00), (1.45, 6.55, 0.48), 86, pivot=(-5.10, 17.70, -4.76), rotation=(2, 0, -3)),
        C((4.60, 11.75, -5.00), (1.45, 6.55, 0.48), 86, pivot=(5.10, 17.70, -4.76), rotation=(2, 0, 3)),
        C((-5.30, 11.10, -5.25), (10.60, 0.62, 0.24), 84),
        C((-4.55, 12.20, -5.24), (9.10, 0.42, 0.22), 85),
        C((-3.95, 11.02, -5.27), (2.10, 0.92, 0.20), 82),
    ])
    return [hat, head_ribbons, bow, bodice, sleeve_left, sleeve_right,
            cuff_left, cuff_right, waist, skirt, apron]


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
        band = ((x//8)+(y//10))%3
        targets = ((12,82,126),(20,94,139),(7,68,108)) if shiny else ((3,59,98),(7,74,113),(2,49,84))
        tr,tg,tb = targets[band]; mix = 0.74
        nr=int(r*(1-mix)+tr*mix); ng=int(g*(1-mix)+tg*mix); nb=int(b*(1-mix)+tb*mix)
        if ((2*x+y)%29) in (0,1): nr+=3; ng+=5; nb+=8
        return max(0,min(255,nr)),max(0,min(255,ng)),max(0,min(255,nb)),a
    if lum < 150:
        band=((x//10)+(y//7))%3
        tr,tg,tb=((31,33,42),(43,45,55),(24,27,36))[band]; mix=.64
        nr=int(r*(1-mix)+tr*mix); ng=int(g*(1-mix)+tg*mix); nb=int(b*(1-mix)+tb*mix)
        if ((3*x+y)%37) in (0,1): nr+=5; ng+=5; nb+=7
        return max(0,min(255,nr)),max(0,min(255,ng)),max(0,min(255,nb)),a
    return r,g,b,a


def patch_metadata() -> None:
    for path in (NORMAL_META, SHINY_META):
        if not path.is_file(): continue
        meta=json.loads(path.read_text(encoding="utf-8"))
        meta["bodyTexelRework"]="OWNER_REFERENCE_MATERIAL_MATCH_V29C"
        meta["paletteIntent"]="Match supplied skin with deeper navy/teal biological blue and charcoal dark biology; costume remains explicit cool white, gray, royal blue and black overlay."
        meta["materialIntent"]="Discrete local value blocks/highlights; lower-body biological silhouette is preserved instead of replaced by cosmetic boot boxes."
        meta["repaintRegions"]=["existing blue biological texels","existing dark biological texels"]
        meta["sourceReferenceSha256"]=REFERENCE_SHA256
        path.write_text(json.dumps(meta,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")


def post_patch() -> None:
    data=json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["concept"]="Owner Reference Replica — Blue/White Maid Lucario V29c"
    data["artStatus"]="ARTISTIC FAIL"
    data["ownerApproval"]={"required":True,"approved":False,"approvedHeadSha":None,"evidenceSetSha256":None,"approvalRecord":None}
    p=data["production"]
    p["productionBoneCount"]=v1.OFFICIAL_BONES+len(cosmetic_bones())
    p["cosmeticBoneCount"]=len(cosmetic_bones())
    p["cosmeticCubeCount"]=sum(len(b.get("cubes",[])) for b in cosmetic_bones())
    b=data["builder"]; b["scriptPath"]="tools/cobblemon-model-review/build_lucario_owner_reference_v29.py"; b["command"]=["python",b["scriptPath"]]
    outputs=[]
    for item in b["outputs"]:
        for old in ("v28-derived-normal.json","v27-derived-normal.json","v22-derived-normal.json"): item=item.replace(old,"v29-reference-derived-normal.json")
        for old in ("v28-derived-shiny.json","v27-derived-shiny.json","v22-derived-shiny.json"): item=item.replace(old,"v29-reference-derived-shiny.json")
        outputs.append(item)
    for path in (NORMAL_META,SHINY_META):
        rel=str(path.relative_to(ROOT)).replace("\\","/")
        if rel not in outputs: outputs.append(rel)
    b["outputs"]=list(dict.fromkeys(outputs))
    q=data["qualityIntent"]
    q["signaturePieces"]=["Broad stacked cool-white headpiece with blue lower band and side ribbons","White bodice/sleeves with blue cuffs, dark glove masses and silver/blue chest clasp","Dominant broad white apron layered in front of blue side/back skirt while official dark Lucario legs and feet remain visible"]
    q["macroFormPlan"]="V29c preserves the clean-sheet owner-reference costume but removes four lower-leg/boot cosmetic shells because exact V29b evidence showed square boot blocks unlike the supplied Lucario-shaped dark feet. A separate broad white apron now sits in front of a blue under-skirt, and the headpiece profile is wider and less pyramidal."
    q["paintPlan"]="Normal and shiny derive independently from exact official 1.7.3 baselines. Biological blues are darker navy/teal so the prominent official tail and face do not overpower the white costume. Red eyes, cream landmarks, UVs, dimensions and alpha remain protected."
    q["gameplayReadGoal"]="At 160 px read the supplied target immediately: tall white headpiece, white uniform sleeves/torso, broad white apron, blue skirt at the sides, large dark gloves, and biological dark Lucario legs/feet rather than square boots."
    q["antiPatternsToReject"]=["Blue-dominant split apron from V29b","Square cosmetic boot shells","Cyan/gold Ronin palette","Changing official biological bones or battle-state authority"]
    q["iterationNote"]="Direct comparison of V29b exact Blockbench evidence against owner image SHA-256 "+REFERENCE_SHA256+" showed correct palette and class read, but the lower body remained too blue/segmented, the headpiece tapered too strongly, and cosmetic boots lost the target's Lucario-like feet. V29c corrects those visible mismatches. Only one source three-quarter view exists, so unseen rear/side micro-details remain conservative."
    data["variantCoverage"]["variants"][0]["coverage"]="Default preserves exact official 87-bone Lucario anatomy and adds the V29c owner-reference costume plus independently derived normal paint."
    data["variantCoverage"]["variants"][1]["coverage"]="Shiny uses identical V29c costume geometry and independent derivation from the exact official shiny baseline."
    v1.MANIFEST.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")


def main() -> None:
    v22.cosmetic_bones=cosmetic_bones
    v22.paint_pixel=paint_pixel
    v22.write_overlay=write_owner_overlay
    v22.main()
    patch_metadata(); post_patch()


if __name__=="__main__": main()
