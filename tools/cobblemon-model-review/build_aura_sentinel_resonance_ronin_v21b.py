#!/usr/bin/env python3
"""Resonance Ronin V21b: compact contour mantle + derived biological paint.

This builder is intentionally independent of the rejected V20 builder so the
legacy manifest entrypoint can delegate here without an import cycle.
Presentation only; AutoPTU/Ouros keeps tactical authority.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
import shutil
import subprocess
import sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
V19_PATH = ROOT / "tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v19.py"
spec = importlib.util.spec_from_file_location("resonance_v19", V19_PATH)
if spec is None or spec.loader is None:
    raise SystemExit("cannot load Resonance Ronin V19 builder")
v19 = importlib.util.module_from_spec(spec); spec.loader.exec_module(v19)
v1 = v19.v1; v14 = v19.v14; mcube = v19.mcube; write_overlay = v19.write_overlay

BASELINE_DIR = ROOT / "tools/cobblemon-model-review/baselines/0448_lucario"
NORMAL_BASELINE = BASELINE_DIR / "lucario_official_1.7.3.png"
SHINY_BASELINE = BASELINE_DIR / "lucario_official_shiny_1.7.3.png"
NORMAL_META = ROOT / "docs/cobblemon-skins/0448_lucario/v21-derived-normal.json"
SHINY_META = ROOT / "docs/cobblemon-skins/0448_lucario/v21-derived-shiny.json"
DERIVED_VALIDATOR = ROOT / "tools/cobblemon-model-review/validate_derived_texture.py"
RETAINED = {"ouros_resonance_cowl", "ouros_resonance_high_collar"}


def ensure_baselines() -> None:
    BASELINE_DIR.mkdir(parents=True, exist_ok=True)
    if not NORMAL_BASELINE.exists():
        if v1.sha256(v1.BODY) != v1.OFFICIAL_NORMAL_SHA256: raise SystemExit("cannot bootstrap normal baseline")
        shutil.copy2(v1.BODY, NORMAL_BASELINE)
    if not SHINY_BASELINE.exists():
        if v1.sha256(v1.SHINY) != v1.OFFICIAL_SHINY_SHA256: raise SystemExit("cannot bootstrap shiny baseline")
        shutil.copy2(v1.SHINY, SHINY_BASELINE)
    if v1.sha256(NORMAL_BASELINE) != v1.OFFICIAL_NORMAL_SHA256: raise SystemExit("normal baseline hash drifted")
    if v1.sha256(SHINY_BASELINE) != v1.OFFICIAL_SHINY_SHA256: raise SystemExit("shiny baseline hash drifted")


def paint_pixel(r:int,g:int,b:int,a:int,x:int,y:int,*,shiny:bool)->tuple[int,int,int,int]:
    if a == 0: return r,g,b,a
    mx,mn=max(r,g,b),min(r,g,b); sat=mx-mn; lum=(30*r+59*g+11*b)//100
    cream=r>175 and g>145 and b<205; white=r>205 and g>205 and b>205
    red=r>105 and r>g*1.35 and r>b*1.35
    if cream or white or red: return r,g,b,a
    blue=b>r*1.22 and b>g*1.10 and sat>28
    if blue:
        vertical=max(-18,min(18,14-y//3)); edge=12 if ((x+2*y)%17 in (0,1)) else 0
        if shiny: nr,ng,nb=int(r*.82)+8,int(g*.94)+10+edge//2,int(b*1.04)+8+edge
        else: nr,ng,nb=int(r*.58)+8,int(g*.82)+15+edge//2,int(b*1.08)+15+edge
        nr+=vertical//4; ng+=vertical//2; nb+=vertical
        return *(max(0,min(255,v)) for v in (nr,ng,nb)),a
    if lum<105 and sat<75:
        occ=max(0,(y-18)//4); hi=9 if ((3*x+y)%23 in (0,1,2)) else 0
        nr=int(r*.72)+10+hi//3-occ//3; ng=int(g*.76)+11+hi//2-occ//3; nb=int(b*.92)+22+hi-occ//4
        return *(max(0,min(255,v)) for v in (nr,ng,nb)),a
    return r,g,b,a


def derive(source:Path,target:Path,*,shiny:bool)->None:
    image=Image.open(source).convert("RGBA"); out=Image.new("RGBA",image.size); src=image.load(); dst=out.load()
    for y in range(image.height):
        for x in range(image.width): dst[x,y]=paint_pixel(*src[x,y],x,y,shiny=shiny)
    target.parent.mkdir(parents=True,exist_ok=True); out.save(target,format="PNG",optimize=True,compress_level=9)


def metadata(path:Path,baseline:Path,derived:Path,*,shiny:bool)->None:
    payload={
      "format":"ouros.cobblemon-derived-texture.v1","species":"lucario","variant":"shiny" if shiny else "normal",
      "officialTextureBaseline":str(baseline.relative_to(ROOT)),"officialTextureBaselineSha256":v1.sha256(baseline),
      "derivedTexture":derived.name,"derivedTextureSha256":v1.sha256(derived),"bodyTexelRework":"PAINTED_VALUE_MATERIAL_PASS",
      "paletteIntent":"Deep aura lacquer on existing blue biology; indigo blue-steel depth on existing dark biology; cream spikes, white landmarks and red eyes preserved.",
      "materialIntent":"Non-uniform local value ramps, lower-surface occlusion, sparse facing-plane highlights and subtle hue/value breakup; no third-party palette, markings or motifs.",
      "repaintRegions":["existing blue biological texels","existing dark biological texels"],"alphaSemantics":"UNCHANGED",
      "sourceRelease":"Cobblemon 1.7.3 Fabric / Modrinth kF7CvxTo"}
    path.parent.mkdir(parents=True,exist_ok=True); path.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")


def validate(baseline:Path,derived:Path,meta:Path,expected:str)->None:
    subprocess.run([sys.executable,str(DERIVED_VALIDATOR),"--official",str(baseline),"--derived",str(derived),"--metadata",str(meta),
                    "--expected-official-sha256",expected,"--expected-derived-sha256",v1.sha256(derived)],cwd=ROOT,check=True)


def bones()->list[dict]:
    retained=[b for b in v14.v14_bones() if b["name"] in RETAINED]
    if {b["name"] for b in retained}!=RETAINED: raise SystemExit("retained cosmetic contract drifted")
    shoulder=v1.bone("ouros_resonance_v21_shoulder_wrap","shoulder_right",[-4.0,30.0,-.7],[
      mcube((-7.65,27.85,-3.00),(4.60,3.15,2.15),80,light=82,dark=88,pivot=(-4.95,29.75,-1.75),rotation=(-18,25,31)),
      mcube((-8.70,25.75,-2.15),(3.55,3.15,1.20),81,light=82,dark=88,pivot=(-6.15,28.05,-1.45),rotation=(-12,31,18))])
    mantle=v1.bone("ouros_resonance_v21_contour_mantle","torso3",[-3.0,27.2,1.0],[
      mcube((-8.15,23.10,.75),(5.10,4.20,1.05),80,light=83,dark=88,pivot=(-5.15,26.45,1.30),rotation=(15,-21,25)),
      mcube((-7.05,18.90,1.05),(4.05,5.20,.78),81,light=83,dark=88,pivot=(-4.55,23.20,1.42),rotation=(11,-14,12)),
      mcube((-5.35,14.85,1.05),(2.75,4.85,.52),80,light=82,dark=88,pivot=(-3.70,19.15,1.30),rotation=(5,-6,-4))])
    hips=v1.bone("ouros_resonance_v21_hip_accents","torso",[0,15.2,.6],[
      mcube((-5.25,12.85,-2.10),(2.35,2.45,.46),84,light=85,dark=80,pivot=(-3.75,14.75,-1.86),rotation=(7,12,-24)),
      mcube((2.75,13.60,.70),(1.55,2.10,.38),82,light=85,dark=80,pivot=(3.35,15.00,.90),rotation=(-8,-12,20))])
    return retained+[shoulder,mantle,hips]


def build_model()->int:
    v19.build_model(); data=json.loads(v1.MODEL.read_text(encoding="utf-8")); geo=data["minecraft:geometry"][0]
    official=geo["bones"][:v1.OFFICIAL_BONES]
    if len(official)!=v1.OFFICIAL_BONES: raise SystemExit("official Lucario bone prefix missing")
    extras=bones(); geo["bones"]=official+extras
    v1.MODEL.write_text(json.dumps(data,ensure_ascii=False,separators=(",",":"))+"\n",encoding="utf-8")
    return sum(len(b.get("cubes",[])) for b in extras)


def patch_manifest(cubes:int)->None:
    data=json.loads(v1.MANIFEST.read_text(encoding="utf-8")); data["artStatus"]="ARTISTIC FAIL"
    data["ownerApproval"]={"required":True,"approved":False,"approvedHeadSha":None,"evidenceSetSha256":None,"approvalRecord":None}
    p=data["production"]; p["modelSha256"]=v1.sha256(v1.MODEL); p["productionBoneCount"]=v1.OFFICIAL_BONES+5; p["cosmeticBoneCount"]=5; p["cosmeticCubeCount"]=cubes
    body=next(t for t in p["textures"] if t["role"]=="BODY"); body.update({"sha256":v1.sha256(v1.BODY),"derivation":"DERIVED_FROM_OFFICIAL",
      "officialBaselineSha256":v1.OFFICIAL_NORMAL_SHA256,"derivedMetadataPath":str(NORMAL_META.relative_to(ROOT))})
    next(t for t in p["textures"] if t["role"]=="OVERLAY")["sha256"]=v1.sha256(v1.OVERLAY)
    for asset in p.get("runtimeAssets",[]):
      if asset.get("role")=="RESOLVER": asset["sha256"]=v1.sha256(v1.RESOLVER)
      if asset.get("role")=="SHINY_BODY": asset["sha256"]=v1.sha256(v1.SHINY)
    b=data["builder"]; b["scriptPath"]="tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v21b.py"; b["command"]=["python",b["scriptPath"]]
    for path in [NORMAL_BASELINE,SHINY_BASELINE,NORMAL_META,SHINY_META]:
      raw=str(path.relative_to(ROOT));
      if raw not in b["outputs"]: b["outputs"].append(raw)
    if "DERIVED_TEXTURE_PROVENANCE" not in data["technicalChecks"]: data["technicalChecks"].append("DERIVED_TEXTURE_PROVENANCE")
    q=data["qualityIntent"]
    q["signaturePieces"]=["Compact oblique shoulder wrap","Single diagonal back-to-hip contour mantle","Body-wide aura lacquer repaint integrated into Lucario anatomy"]
    q["macroFormPlan"]="V21 deletes V20's hanging half-cloak cascade and dorsal handoff. Cowl and high collar remain; two shoulder facets start one diagonal three-facet back-to-hip mantle, with two small asymmetric hip counterweights and open center back/chest/tail space."
    q["paintPlan"]="Derive normal and shiny independently from exact 1.7.3 baselines. Existing blue texels receive local aura-lacquer ramps; dark texels receive indigo blue-steel occlusion and sparse facing highlights. Cream spikes, white landmarks, red eyes, UVs, dimensions and alpha semantics remain intact."
    q["gameplayReadGoal"]="At 160 px the transformation should read as one coherent cobalt/indigo material identity plus a diagonal shoulder-to-hip gesture, without a rectangular hanging garment."
    q["iterationNote"]="V20 passed technical visual floors but direct Blockbench QA rejected the half-cloak as stacked dark plates. V21 reduces exterior geometry and transfers more authorship into a validated derived-texture pass."
    data["variantCoverage"]["variants"][0]["coverage"]="Default Lucario preserves the exact official 87-bone geometry and uses a validated normal texture derived from the exact official 1.7.3 baseline."
    data["variantCoverage"]["variants"][1]["coverage"]="Shiny uses the same cosmetic geometry and overlay plus an independently derived texture from the exact official shiny 1.7.3 baseline."
    v1.MANIFEST.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")


def main()->None:
    parser=argparse.ArgumentParser(); parser.add_argument("--bootstrap",action="store_true"); args=parser.parse_args()
    ensure_baselines(); cubes=build_model(); derive(NORMAL_BASELINE,v1.BODY,shiny=False); derive(SHINY_BASELINE,v1.SHINY,shiny=True)
    metadata(NORMAL_META,NORMAL_BASELINE,v1.BODY,shiny=False); metadata(SHINY_META,SHINY_BASELINE,v1.SHINY,shiny=True)
    validate(NORMAL_BASELINE,v1.BODY,NORMAL_META,v1.OFFICIAL_NORMAL_SHA256); validate(SHINY_BASELINE,v1.SHINY,SHINY_META,v1.OFFICIAL_SHINY_SHA256)
    write_overlay(v1.OVERLAY); v1.build_resolver();
    if args.bootstrap: patch_manifest(cubes)
    print(json.dumps({"status":"BUILT","concept":"Aura Sentinel — Resonance Ronin V21","officialBones":v1.OFFICIAL_BONES,
      "cosmeticBones":5,"cosmeticCubes":cubes,"modelSha256":v1.sha256(v1.MODEL),"normalDerivedSha256":v1.sha256(v1.BODY),
      "shinyDerivedSha256":v1.sha256(v1.SHINY),"overlaySha256":v1.sha256(v1.OVERLAY),"bodyTexelRework":"PAINTED_VALUE_MATERIAL_PASS"},indent=2))

if __name__=="__main__": main()
