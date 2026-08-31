#!/usr/bin/env python3
"""Lucario owner-reference replica V29d.

V29/V29b established the full-body owner-reference reconstruction and fixed the
legacy palette. V29c restored Lucario-shaped biological feet and made the apron
white, but exact Blockbench evidence showed that apron as a thin detached placard.
V29d turns it into a stepped, volumetric waist-connected apron, adds the visible
blue/lavender headpiece band, and deepens biological teal/navy to better match the
single supplied three-quarter render while preserving the exact official 87 bones.

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
    hat = v1.bone("ouros_v29_reference_headpiece", "head_angle", [0, 39.2, -0.2], [
        C((-4.65, 38.55, -2.45), (9.30, 1.05, 5.35), 83),
        C((-4.35, 39.10, -2.20), (8.70, 1.10, 5.00), 87),
        C((-4.32, 40.00, -2.18), (8.64, 0.52, 4.96), 84),
        C((-4.20, 40.42, -2.05), (8.40, 1.90, 4.80), 86),
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
    skirt = v1.bone("ouros_v29_reference_under_skirt", "torso", [0, 18.8, -0.2], [
        C((-7.15, 11.55, -2.95), (2.25, 7.10, 6.20), 83, pivot=(-5.80, 18.00, 0.00), rotation=(0, 0, -5)),
        C((4.90, 11.55, -2.95), (2.25, 7.10, 6.20), 83, pivot=(5.80, 18.00, 0.00), rotation=(0, 0, 5)),
        C((-5.70, 11.50, 2.85), (11.40, 7.15, 0.65), 83, pivot=(0, 18.00, 3.15), rotation=(-3, 0, 0)),
    ])

    # Three stepped volumes form the apron body. Their increasing width toward
    # the hem produces the same broad flared read as the supplied skin without a
    # detached paper-thin sign-board silhouette.
    apron = v1.bone("ouros_v29_reference_apron_front", "torso", [0, 18.4, -3.9], [
        C((-5.35, 16.45, -4.72), (10.70, 2.25, 1.18), 87, pivot=(0, 18.25, -4.05), rotation=(2, 0, 0)),
        C((-5.65, 13.70, -4.82), (11.30, 2.90, 1.23), 87, pivot=(0, 16.25, -4.10), rotation=(1.5, 0, 0)),
        C((-5.90, 11.15, -4.72), (11.80, 2.75, 1.12), 86, pivot=(0, 13.85, -4.10), rotation=(1, 0, 0)),
        C((-6.20, 11.65, -4.20), (0.72, 6.45, 1.20), 86, pivot=(-5.65, 17.25, -3.85), rotation=(2, 0, -3)),
        C((5.48, 11.65, -4.20), (0.72, 6.45, 1.20), 86, pivot=(5.65, 17.25, -3.85), rotation=(2, 0, 3)),
        C((-5.45, 11.02, -5.28), (10.90, 0.58, 0.25), 84),
        C((-4.45, 12.04, -5.30), (2.15, 0.42, 0.22), 82),
        C((2.30, 12.04, -5.30), (2.15, 0.42, 0.22), 82),
    ])
    return [hat, head_ribbons, bow, bodice, sleeve_left, sleeve_right,
            cuff_left, cuff_right, waist, skirt, apron]


def paint_pixel(r: int, g: int, b: int, a: int, x: int, y: int, *, shiny: bool):
    if a == 0: return r,g,b,a
    mx,mn=max(r,g,b),min(r,g,b); sat=mx-mn; lum=(30*r+59*g+11*b)//100
    cream=r>170 and g>135 and b<205; white=r>210 and g>210 and b>210; red=r>105 and r>g*1.35 and r>b*1.35
    if cream or white or red: return r,g,b,a
    blue=b>r*1.15 and b>g*1.04 and sat>20
    if blue:
        band=((x//8)+(y//10))%3
        targets=((10,75,117),(16,87,129),(5,62,101)) if shiny else ((2,50,86),(5,63,99),(1,42,73))
        tr,tg,tb=targets[band]; mix=.78
        nr=int(r*(1-mix)+tr*mix); ng=int(g*(1-mix)+tg*mix); nb=int(b*(1-mix)+tb*mix)
        if ((2*x+y)%29) in (0,1): nr+=3; ng+=5; nb+=7
        return max(0,min(255,nr)),max(0,min(255,ng)),max(0,min(255,nb)),a
    if lum<150:
        band=((x//10)+(y//7))%3; tr,tg,tb=((31,33,42),(43,45,55),(24,27,36))[band]; mix=.64
        nr=int(r*(1-mix)+tr*mix); ng=int(g*(1-mix)+tg*mix); nb=int(b*(1-mix)+tb*mix)
        if ((3*x+y)%37) in (0,1): nr+=5; ng+=5; nb+=7
        return max(0,min(255,nr)),max(0,min(255,ng)),max(0,min(255,nb)),a
    return r,g,b,a


def patch_metadata() -> None:
    for path in (NORMAL_META,SHINY_META):
        if not path.is_file(): continue
        meta=json.loads(path.read_text(encoding="utf-8"))
        meta["bodyTexelRework"]="OWNER_REFERENCE_MATERIAL_MATCH_V29D"
        meta["paletteIntent"]="Deep navy/teal biological blue and charcoal dark biology match the supplied render while cool white/gray/royal-blue/black costume remains explicit in the overlay."
        meta["materialIntent"]="Local value variation only; official lower-leg and foot silhouette remains biological."
        meta["repaintRegions"]=["existing blue biological texels","existing dark biological texels"]
        meta["sourceReferenceSha256"]=REFERENCE_SHA256
        path.write_text(json.dumps(meta,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")


def post_patch() -> None:
    data=json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["concept"]="Owner Reference Replica — Blue/White Maid Lucario V29d"
    data["artStatus"]="ARTISTIC FAIL"
    data["ownerApproval"]={"required":True,"approved":False,"approvedHeadSha":None,"evidenceSetSha256":None,"approvalRecord":None}
    p=data["production"]; p["productionBoneCount"]=v1.OFFICIAL_BONES+len(cosmetic_bones()); p["cosmeticBoneCount"]=len(cosmetic_bones()); p["cosmeticCubeCount"]=sum(len(b.get("cubes",[])) for b in cosmetic_bones())
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
    q["signaturePieces"]=["Broad stacked cool-white headpiece with blue lower and lavender mid band plus side ribbons","White bodice/sleeves with blue cuffs, dark glove masses and silver/blue chest clasp","Waist-connected three-tier white apron flaring over blue side/back skirt while official dark Lucario legs and feet remain visible"]
    q["macroFormPlan"]="V29d keeps 11 animation-rooted costume systems over the immutable official 87-bone prefix. The apron is now three overlapping volumetric tiers connected to the waist, not V29c's thin placard. Headpiece receives the visible blue/lavender band. No lower-leg or boot cosmetic shells return."
    q["paintPlan"]="Normal and shiny derive independently from exact 1.7.3 baselines. Biological blue is deepened further to navy/teal; dark biology stays charcoal. Red eyes, cream landmarks, UVs, dimensions and alpha remain protected."
    q["gameplayReadGoal"]="At 160 px read the supplied target immediately: tall banded white headpiece, white uniform torso/sleeves, broad dimensional white apron, blue skirt sides, large dark gloves and Lucario-shaped dark feet."
    q["antiPatternsToReject"]=["Detached placard apron","Square cosmetic boots","Blue-dominant apron","Cyan/gold Ronin palette","Any change to official biological bones or battle-state authority"]
    q["iterationNote"]="V29c exact evidence restored biological feet and white apron color but the front panel read as a detached flat placard. V29d replaces it with three stepped waist-connected volumes and adds the source-visible headpiece band. Owner image SHA-256: "+REFERENCE_SHA256+". Only one source three-quarter view exists, so unseen rear/side micro-details remain conservative."
    data["variantCoverage"]["variants"][0]["coverage"]="Default preserves exact official 87-bone Lucario anatomy and adds V29d owner-reference costume plus independently derived normal paint."
    data["variantCoverage"]["variants"][1]["coverage"]="Shiny uses identical V29d costume geometry and independent derivation from exact official shiny baseline."
    v1.MANIFEST.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")


def main() -> None:
    v22.cosmetic_bones=cosmetic_bones; v22.paint_pixel=paint_pixel; v22.write_overlay=write_owner_overlay
    v22.main(); patch_metadata(); post_patch()


if __name__=="__main__": main()
