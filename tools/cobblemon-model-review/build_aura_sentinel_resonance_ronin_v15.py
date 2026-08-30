#!/usr/bin/env python3
"""Resonance Ronin V15: silhouette-sweep iteration after V14c exact-head floor failure.

V14c changed surface/material enough but exact Blockbench evidence measured only
silhouetteDeltaRatio=0.0289 against the unchanged 0.0400 floor. V15 keeps the
successful narrow mantle architecture and adds a deliberate two-ended contour sweep:
three small overlapping shoulder pennons rise/recede from the official left shoulder,
and two narrow hip streamers continue the existing knot outward/downward. Each piece
is thin, differently sized and compound-rotated; there is visible negative space
between pieces. This expands the external contour without a broad slab, cage, wing,
portal frame, or alternate body rig. Official Lucario anatomy remains untouched.
"""
from __future__ import annotations
import argparse, importlib.util, json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
V14_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v14.py"
spec = importlib.util.spec_from_file_location("resonance_v14", V14_PATH)
if spec is None or spec.loader is None: raise SystemExit("cannot load V14c builder")
v14 = importlib.util.module_from_spec(spec); spec.loader.exec_module(v14)
v1=v14.v1; mcube=v14.mcube; write_overlay=v14.write_overlay


def v15_bones() -> list[dict]:
    bones=v14.v14_bones()
    shoulder_sweep=v1.bone("ouros_resonance_shoulder_sweep","shoulder_left",[7.5,30.4,.7],[
        mcube((7.0,31.0,1.05),(2.0,5.2,.34),84,light=85,dark=80,pivot=(7.7,31.7,1.22),rotation=(-15,-20,-29)),
        mcube((8.15,28.9,1.35),(1.65,4.55,.30),82,light=83,dark=81,pivot=(8.55,30.25,1.50),rotation=(-22,-24,-38)),
        mcube((8.8,27.1,1.60),(1.25,3.75,.27),84,light=85,dark=80,pivot=(9.05,28.5,1.73),rotation=(-26,-28,-47)),
    ])
    hip_streamers=v1.bone("ouros_resonance_hip_streamers","torso",[0,15.0,1.4],[
        mcube((-6.1,8.2,1.35),(1.7,6.2,.32),82,light=83,dark=81,pivot=(-5.15,12.1,1.50),rotation=(-29,18,25)),
        mcube((-7.0,6.6,1.75),(1.15,4.8,.28),84,light=85,dark=80,pivot=(-6.35,9.8,1.88),rotation=(-34,21,34)),
    ])
    return bones+[shoulder_sweep,hip_streamers]


def build_model()->int:
    v14.build_model()
    data=json.loads(v1.MODEL.read_text(encoding="utf-8")); geo=data["minecraft:geometry"][0]
    official=geo["bones"][:v1.OFFICIAL_BONES]
    if len(official)!=v1.OFFICIAL_BONES: raise SystemExit("official Lucario bone prefix missing")
    extras=v15_bones(); geo["bones"]=official+extras
    v1.MODEL.write_text(json.dumps(data,ensure_ascii=False,separators=(",",":"))+"\n",encoding="utf-8")
    return sum(len(b.get("cubes",[])) for b in extras)


def patch_manifest(cubes:int)->None:
    data=json.loads(v1.MANIFEST.read_text(encoding="utf-8"))
    data["artStatus"]="ARTISTIC FAIL"
    data["ownerApproval"]={"required":True,"approved":False,"approvedHeadSha":None,"evidenceSetSha256":None,"approvalRecord":None}
    data["production"]["modelSha256"]=v1.sha256(v1.MODEL)
    data["production"]["productionBoneCount"]=v1.OFFICIAL_BONES+13
    data["production"]["cosmeticBoneCount"]=13
    data["production"]["cosmeticCubeCount"]=cubes
    next(t for t in data["production"]["textures"] if t["role"]=="OVERLAY")["sha256"]=v1.sha256(v1.OVERLAY)
    for a in data["production"].get("runtimeAssets",[]):
        if a.get("role")=="RESOLVER": a["sha256"]=v1.sha256(v1.RESOLVER)
    data["builder"]["scriptPath"]="tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v15.py"
    data["builder"]["command"]=["python","tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v15.py"]
    data["qualityIntent"]["signaturePieces"]=[
      "Continuous cowl/collar/shoulder/back/hip mantle arc with no broad rectangular pauldron",
      "Three unequal shoulder pennons forming a separated rising/receding contour sweep rather than one wing or slab",
      "Hip knot resolving into unequal tails plus two narrow outward streamers with negative space around Lucario's biological tail"
    ]
    data["qualityIntent"]["macroFormPlan"]=("V15 preserves V14c's surface continuity and answers its measured silhouette deficit directly. The external contour is authored at two connected endpoints only: three thin shoulder pennons and two thin hip streamers. Their size, angle and depth taper progressively, and gaps remain visible between pieces. No threshold changes and no biological edits.")
    data["qualityIntent"]["gameplayReadGoal"]=("At 160 px the first read should be a single asymmetric diagonal ceremonial sweep: higher and more open at the left shoulder, then turning around the back and resolving lower/outward at the opposite hip. The contour must read as layered cloth motion rather than armor blocks.")
    data["qualityIntent"]["iterationNote"]=("Exact V14c head 9238a5ad3b0f87891a9e9e31db849291d97e7810 passed source, builder, bone and attachment checks but failed the unchanged silhouette floor at 0.0289 < 0.0400. V15 adds only contour-authored shoulder/hip sweep pieces; owner approval remains absent.")
    v1.MANIFEST.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")


def main():
    p=argparse.ArgumentParser(); p.add_argument("--bootstrap",action="store_true"); a=p.parse_args()
    if v1.sha256(v1.BODY)!=v1.OFFICIAL_NORMAL_SHA256: raise SystemExit("normal body texture drifted")
    if v1.sha256(v1.SHINY)!=v1.OFFICIAL_SHINY_SHA256: raise SystemExit("shiny body texture drifted")
    cubes=build_model(); write_overlay(v1.OVERLAY); v1.build_resolver()
    if a.bootstrap: patch_manifest(cubes)
    print(json.dumps({"status":"BUILT","concept":"Aura Sentinel — Resonance Ronin V15","officialBones":v1.OFFICIAL_BONES,"cosmeticBones":13,"cosmeticCubes":cubes,"modelSha256":v1.sha256(v1.MODEL),"overlaySha256":v1.sha256(v1.OVERLAY),"bodyTexelRework":"NONE","visualChange":"asymmetric shoulder/hip contour sweep; unchanged technical floors"},indent=2))
if __name__=="__main__": main()
