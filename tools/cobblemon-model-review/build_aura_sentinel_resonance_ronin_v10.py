#!/usr/bin/env python3
"""Resonance Ronin V10: back-weighted mantle and open-face cowl after V9 art QA fail.

V9 passed the technical visual floor but its side sweep read as a stack of plates.
V10 preserves Lucario's exact 87-bone biological prefix and moves the authored
mass behind the anatomy: one shoulder-rooted back mantle, two long overlapping
coat tails with negative space, and a compact open-face cowl. Presentation only.
AutoPTU/Ouros remains authoritative for all tactical battle facts.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V9_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v9.py"
spec = importlib.util.spec_from_file_location("resonance_v9", V9_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V9 builder")
v9 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v9)
v8 = v9.v8
v1 = v9.v1
mcube = v9.mcube
write_overlay = v9.write_overlay


def v10_bones() -> list[dict]:
    # Compact head system. The face, muzzle, eyes, ears and aura sensors remain
    # visually open; the cowl is carried by a rear crown and two angled cheek fins.
    cowl = v1.bone("ouros_resonance_cowl", "head_angle", [0, 37.2, -0.8], [
        mcube((-4.0, 38.0, -1.0), (8.0, 1.25, 3.2), 84, light=85, dark=80,
              pivot=(0, 38.6, .5), rotation=(-5, 0, 0)),
        mcube((-4.35, 35.2, -4.82), (1.15, 3.3, .28), 82, light=83, dark=81,
              pivot=(-3.75, 36.9, -4.68), rotation=(0, -4, 13)),
        mcube((3.2, 35.2, -4.82), (1.15, 3.3, .28), 82, light=83, dark=81,
              pivot=(3.75, 36.9, -4.68), rotation=(0, 4, -13)),
        mcube((-2.9, 39.15, -4.66), (5.8, .34, .24), 86, light=87, dark=81,
              pivot=(0,39.3,-4.54), rotation=(0,0,0)),
    ])

    # One back-weighted mantle system. A broad shoulder yoke touches torso3,
    # then two overlapping back planes form a continuous upper mass. The lower
    # silhouette splits into two long tails separated by deliberate negative space.
    mantle = v1.bone("ouros_resonance_mantle", "torso3", [0, 29.4, 1.2], [
        mcube((-6.6, 28.7, -0.4), (13.2, 3.5, 4.2), 82, light=83, dark=81,
              pivot=(0,30.2,1.5), rotation=(5,0,0)),
        mcube((-6.8, 23.6, 2.35), (13.6, 6.4, .54), 81, light=83, dark=88,
              pivot=(0,28.9,2.62), rotation=(-12,0,0)),
        mcube((-7.4, 18.5, 2.62), (14.8, 6.6, .48), 82, light=83, dark=81,
              pivot=(0,23.9,2.86), rotation=(-15,0,0)),
        # left tail: broad at hip, taper simulated by overlapping rotated stages
        mcube((-7.7, 11.5, 2.88), (6.7, 8.8, .42), 81, light=83, dark=88,
              pivot=(-4.2,18.8,3.09), rotation=(-18,5,-7)),
        mcube((-7.25, 6.4, 3.12), (5.4, 6.7, .36), 82, light=83, dark=81,
              pivot=(-4.45,11.8,3.30), rotation=(-21,8,-10)),
        # right tail deliberately differs in angle/length; centre gap remains open
        mcube((1.15, 12.8, 2.9), (5.9, 7.6, .42), 82, light=83, dark=81,
              pivot=(4.15,19.0,3.11), rotation=(-17,-5,7)),
        mcube((1.85, 8.5, 3.12), (4.7, 5.8, .36), 81, light=83, dark=88,
              pivot=(4.25,13.0,3.30), rotation=(-20,-8,10)),
        # edge seams terminate the tails without creating an external cage
        mcube((-6.65, 6.1, 3.35), (3.9, .30, .16), 86, light=87, dark=81,
              pivot=(-4.45,6.3,3.43), rotation=(-21,8,-10)),
        mcube((2.35, 8.2, 3.35), (3.5, .30, .16), 86, light=87, dark=81,
              pivot=(4.25,8.4,3.43), rotation=(-20,-8,10)),
    ])

    # One diagonal lapel and narrow obi. No chest plate, skirt or front box.
    sash = v1.bone("ouros_resonance_sash", "torso3", [0, 27.0, -3.4], [
        mcube((-5.0, 27.1, -4.08), (8.5, .78, .24), 84, light=85, dark=80,
              pivot=(-.8,27.5,-3.96), rotation=(0,0,-31)),
        mcube((-3.4, 22.85, -3.60), (6.8, .58, .52), 84, light=85, dark=80),
    ])

    left_vambrace = v1.bone("ouros_resonance_left_vambrace", "arm_left2", [10.3,29.4,-.3], [
        mcube((9.0,27.8,-2.16),(2.6,2.55,.26),80,light=82,dark=88,
              pivot=(10.3,29.05,-2.03),rotation=(0,-4,-6)),
    ])
    right_vambrace = v1.bone("ouros_resonance_right_vambrace", "arm_right2", [-10.3,29.4,-.3], [
        mcube((-11.5,27.9,-2.14),(2.3,2.35,.24),80,light=82,dark=88,
              pivot=(-10.3,29.05,-2.02),rotation=(0,3,5)),
    ])
    left_greave = v1.bone("ouros_resonance_left_greave", "leg_left4", [3.5,6.15,-1.5], [
        mcube((1.95,-1.0,-2.0),(2.8,5.6,.28),80,light=82,dark=88,
              pivot=(3.35,1.8,-1.86),rotation=(-8,0,-5)),
    ])
    right_greave = v1.bone("ouros_resonance_right_greave", "leg_right4", [-3.5,6.15,-1.5], [
        mcube((-4.75,-1.0,-2.0),(2.8,5.6,.28),80,light=82,dark=88,
              pivot=(-3.35,1.8,-1.86),rotation=(-8,0,5)),
    ])
    tail_clasp = v1.bone("ouros_resonance_tail_clasp", "tail2", [0,19.4,10.0], [
        mcube((-1.05,18.25,9.35),(2.1,1.05,.28),84,light=85,dark=80),
    ])
    return [cowl,mantle,sash,left_vambrace,right_vambrace,left_greave,right_greave,tail_clasp]


def build_model() -> int:
    v9.build_model()
    data=json.loads(v1.MODEL.read_text(encoding="utf-8"))
    geo=data["minecraft:geometry"][0]
    official=geo["bones"][:v1.OFFICIAL_BONES]
    if len(official)!=v1.OFFICIAL_BONES:
        raise SystemExit("official Lucario bone prefix missing")
    extras=v10_bones()
    geo["bones"]=official+extras
    v1.MODEL.write_text(json.dumps(data,ensure_ascii=False,separators=(",",":"))+"\n",encoding="utf-8")
    return sum(len(b.get("cubes",[])) for b in extras)


def patch_manifest(cubes:int)->None:
    data=json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["artStatus"]="ARTISTIC FAIL"
    data["ownerApproval"]={"required":True,"approved":False,"approvedHeadSha":None,"evidenceSetSha256":None,"approvalRecord":None}
    data["production"]["modelSha256"]=v1.sha256(v1.MODEL)
    data["production"]["productionBoneCount"]=v1.OFFICIAL_BONES+8
    data["production"]["cosmeticBoneCount"]=8
    data["production"]["cosmeticCubeCount"]=cubes
    overlay=next(t for t in data["production"]["textures"] if t["role"]=="OVERLAY")
    overlay["sha256"]=v1.sha256(v1.OVERLAY)
    for asset in data["production"].get("runtimeAssets",[]):
        if asset.get("role")=="RESOLVER": asset["sha256"]=v1.sha256(v1.RESOLVER)
    data["builder"]["scriptPath"]="tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v10.py"
    data["builder"]["command"]=["python","tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v10.py"]
    data["qualityIntent"]["signaturePieces"]=[
        "Back-weighted shoulder mantle with one broad upper mass and two long asymmetrical tails separated by negative space",
        "Compact open-face resonance cowl preserving eyes, muzzle, ears and aura sensors",
        "Minimal diagonal lapel plus articulation-safe arm, shin and tail accents"
    ]
    data["qualityIntent"]["macroFormPlan"]=(
        "V10 rejects V9's lateral tile-chain read. The signature mass now sits behind the anatomy: a broad shoulder yoke flows into two overlapping back planes, then splits into two long asymmetrical tails with a clear centre gap. "
        "The head gains a compact open-face cowl; the front keeps only a diagonal lapel and obi. No skirt, shorts, cage, backpack, repeated bar or armor-shell system is introduced."
    )
    data["qualityIntent"]["gameplayReadGoal"]=(
        "At 160 px Lucario must read as a ceremonial ronin with one continuous rear mantle and open-face cowl, not as Lucario with a stack of side plates. "
        "The technical floors remain 0.0800 pixel difference and 0.0400 silhouette delta and may not be relaxed."
    )
    data["qualityIntent"]["iterationNote"]="V10 follows a direct internal rejection of V9's plate-chain silhouette; owner approval remains absent."
    v1.MANIFEST.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")


def main()->None:
    parser=argparse.ArgumentParser(); parser.add_argument("--bootstrap",action="store_true"); args=parser.parse_args()
    if v1.sha256(v1.BODY)!=v1.OFFICIAL_NORMAL_SHA256: raise SystemExit("normal body texture drifted from official Lucario")
    if v1.sha256(v1.SHINY)!=v1.OFFICIAL_SHINY_SHA256: raise SystemExit("shiny body texture drifted from official Lucario")
    cubes=build_model(); write_overlay(v1.OVERLAY); v1.build_resolver()
    if args.bootstrap: patch_manifest(cubes)
    print(json.dumps({"status":"BUILT","concept":"Aura Sentinel — Resonance Ronin V10","officialBones":v1.OFFICIAL_BONES,"cosmeticBones":8,"cosmeticCubes":cubes,"modelSha256":v1.sha256(v1.MODEL),"overlaySha256":v1.sha256(v1.OVERLAY),"resolverSha256":v1.sha256(v1.RESOLVER),"normalBodySha256":v1.sha256(v1.BODY),"shinyBodySha256":v1.sha256(v1.SHINY),"bodyTexelRework":"NONE","visualChange":"back-weighted continuous mantle with split tails and open-face cowl"},indent=2,ensure_ascii=False))

if __name__=="__main__": main()
