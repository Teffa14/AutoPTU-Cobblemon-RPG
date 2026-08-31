#!/usr/bin/env python3
"""Resonance Ronin V22: integrated mantle crest + restrained derived paint.

V21 proved that the derived-texture path is reproducible, but matched-camera QA
showed silhouetteDeltaRatio 0.0221 and direct review showed bright blocky thighs
plus a legacy cowl/plate read. V22 removes all legacy cosmetics and rebuilds the
presentation from the official 87-bone prefix using thin rotated planes and
compact contact-rooted shells. It keeps the successful derived-texture contract
but shifts blue biology to restrained deep cobalt rather than electric blue.

Presentation only. AutoPTU/Ouros remains authoritative for tactical battle facts.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
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
v1 = v19.v1; mcube = v19.mcube; write_overlay = v19.write_overlay

BASELINE_DIR = ROOT / "tools/cobblemon-model-review/baselines/0448_lucario"
NORMAL_BASELINE = BASELINE_DIR / "lucario_official_1.7.3.png"
SHINY_BASELINE = BASELINE_DIR / "lucario_official_shiny_1.7.3.png"
NORMAL_META = ROOT / "docs/cobblemon-skins/0448_lucario/v22-derived-normal.json"
SHINY_META = ROOT / "docs/cobblemon-skins/0448_lucario/v22-derived-shiny.json"
VALIDATOR = ROOT / "tools/cobblemon-model-review/validate_derived_texture.py"


def require_baselines() -> None:
    expected=[(NORMAL_BASELINE,v1.OFFICIAL_NORMAL_SHA256),(SHINY_BASELINE,v1.OFFICIAL_SHINY_SHA256)]
    for path,sha in expected:
        if not path.is_file(): raise SystemExit(f"missing pinned official baseline {path}")
        if v1.sha256(path)!=sha: raise SystemExit(f"official baseline hash drifted: {path}")


def paint_pixel(r:int,g:int,b:int,a:int,x:int,y:int,*,shiny:bool)->tuple[int,int,int,int]:
    if a==0: return r,g,b,a
    mx,mn=max(r,g,b),min(r,g,b); sat=mx-mn; lum=(30*r+59*g+11*b)//100
    cream=r>170 and g>135 and b<205
    white=r>205 and g>205 and b>205
    red=r>105 and r>g*1.35 and r>b*1.35
    if cream or white or red: return r,g,b,a
    blue=b>r*1.20 and b>g*1.08 and sat>25
    if blue:
        # Deep cobalt with a controlled top/facing lift. Avoid the saturated
        # electric-blue masses seen in V21 at the thighs and forearms.
        ramp=max(-12,min(12,10-y//4))
        edge=7 if ((x+3*y)%29 in (0,1)) else 0
        if shiny:
            nr=int(r*.72)+7; ng=int(g*.82)+9+edge//2; nb=int(b*.90)+10+edge
        else:
            nr=int(r*.42)+8; ng=int(g*.60)+12+edge//2; nb=int(b*.82)+18+edge
        nr+=ramp//5; ng+=ramp//3; nb+=ramp//2
        return *(max(0,min(255,v)) for v in (nr,ng,nb)),a
    if lum<112 and sat<82:
        occ=max(0,(y-20)//5); hi=6 if ((2*x+y)%31 in (0,1)) else 0
        nr=int(r*.68)+9+hi//3-occ//3
        ng=int(g*.70)+10+hi//2-occ//3
        nb=int(b*.86)+18+hi-occ//4
        return *(max(0,min(255,v)) for v in (nr,ng,nb)),a
    return r,g,b,a


def derive(source:Path,target:Path,*,shiny:bool)->None:
    image=Image.open(source).convert("RGBA"); out=Image.new("RGBA",image.size); src=image.load(); dst=out.load()
    for y in range(image.height):
        for x in range(image.width): dst[x,y]=paint_pixel(*src[x,y],x,y,shiny=shiny)
    target.parent.mkdir(parents=True,exist_ok=True); out.save(target,format="PNG",optimize=True,compress_level=9)


def write_meta(path:Path,baseline:Path,derived:Path,*,shiny:bool)->None:
    payload={
      "format":"ouros.cobblemon-derived-texture.v1","species":"lucario","variant":"shiny" if shiny else "normal",
      "officialTextureBaseline":str(baseline.relative_to(ROOT)),"officialTextureBaselineSha256":v1.sha256(baseline),
      "derivedTexture":derived.name,"derivedTextureSha256":v1.sha256(derived),"bodyTexelRework":"PAINTED_VALUE_MATERIAL_PASS",
      "paletteIntent":"Restrained deep-cobalt aura lacquer on existing blue biology and indigo steel depth on existing dark biology; cream spikes, white landmarks and red eyes preserved.",
      "materialIntent":"Local value ramps, lower-surface painted occlusion and sparse facing highlights; no uniform hue rotation, flood fill, third-party palette, markings or costume motifs.",
      "repaintRegions":["existing blue biological texels","existing dark biological texels"],"alphaSemantics":"UNCHANGED",
      "sourceRelease":"Cobblemon 1.7.3 Fabric / Modrinth kF7CvxTo"}
    path.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")


def validate(baseline:Path,derived:Path,meta:Path,expected:str)->None:
    subprocess.run([sys.executable,str(VALIDATOR),"--official",str(baseline),"--derived",str(derived),"--metadata",str(meta),
                    "--expected-official-sha256",expected,"--expected-derived-sha256",v1.sha256(derived)],cwd=ROOT,check=True)


def plane(origin,size,uv,*,pivot,rotation,light=83,dark=88):
    return mcube(origin,size,uv,light=light,dark=dark,pivot=pivot,rotation=rotation)


def cosmetic_bones()->list[dict]:
    # Open-face temple crown. Thin side planes follow the head instead of a boxy
    # hood; the official muzzle, eyes, ears and aura sensors stay exposed.
    crown=v1.bone("ouros_v22_open_aura_crown","head_angle",[0,36.0,-1.7],[
      plane((-4.15,35.2,-3.75),(.32,3.9,3.0),82,pivot=(-3.75,36.4,-2.1),rotation=(3,-16,-12),light=84,dark=89),
      plane((3.83,35.2,-3.75),(.32,3.9,3.0),82,pivot=(3.75,36.4,-2.1),rotation=(3,16,12),light=84,dark=89),
    ])

    # Asymmetric mantle root. These are shallow shells, heavily rotated and
    # overlapped, making one sloped shoulder mass rather than pauldron cubes.
    shoulder=v1.bone("ouros_v22_mantle_root","shoulder_right",[-3.2,30.0,-.5],[
      plane((-8.3,27.9,-2.2),(5.6,.42,4.1),80,pivot=(-4.8,29.8,-.2),rotation=(18,-14,31)),
      plane((-7.5,26.1,-.2),(4.9,.34,4.9),81,pivot=(-4.7,28.5,.9),rotation=(-9,-26,20),light=82,dark=88),
    ])

    # Signature mantle crest: three broad, near-zero-thickness planes fan from a
    # single back contact root. Their axes diverge and lengths taper; together
    # they make a broken crescent with negative space rather than a portal frame.
    crest=v1.bone("ouros_v22_dorsal_mantle_crest","torso3",[-2.2,27.0,1.0],[
      plane((-7.7,23.5,1.45),(5.4,.18,7.0),80,pivot=(-4.2,27.1,1.7),rotation=(22,-29,34),light=83,dark=89),
      plane((-5.6,19.0,1.65),(4.1,.16,7.4),81,pivot=(-3.6,23.2,1.8),rotation=(14,-18,18),light=82,dark=89),
      plane((-3.8,14.5,1.55),(2.8,.14,6.0),80,pivot=(-2.8,19.0,1.7),rotation=(7,-8,3),light=82,dark=88),
    ])

    # Chest line turns the mantle across the body but keeps the chest spike open.
    breast=v1.bone("ouros_v22_breast_sash","torso3",[0,28.0,-2.3],[
      plane((-4.0,26.4,-3.25),(6.8,.22,1.55),84,pivot=(0,28.3,-3.0),rotation=(5,0,-27),light=86,dark=80),
    ])

    # Split waistcoat: two independent thin tails with different length and angle.
    # They frame, rather than cover, the biological legs and tail.
    waist=v1.bone("ouros_v22_split_waistcoat","torso",[0,17.0,.3],[
      plane((-4.7,10.4,-.15),(2.5,.18,7.2),80,pivot=(-2.9,17.0,.2),rotation=(7,8,-13),light=82,dark=89),
      plane((2.0,12.0,.4),(2.0,.16,5.8),81,pivot=(2.7,17.0,.6),rotation=(-6,-10,14),light=83,dark=89),
    ])

    # Motion-safe forearm accents use one shallow shell each, subordinate to the
    # mantle and tied to official animated bones.
    arms=v1.bone("ouros_v22_arm_guards","arms",[0,29.4,-.3],[
      plane((-12.0,28.0,-2.1),(3.2,.28,3.4),84,pivot=(-10.0,29.5,-.5),rotation=(12,8,-8),light=85,dark=80),
      plane((8.8,28.0,-2.1),(3.2,.28,3.4),84,pivot=(10.0,29.5,-.5),rotation=(12,-8,8),light=85,dark=80),
    ])
    return [crown,shoulder,crest,breast,waist,arms]


def build_model()->int:
    v19.build_model(); data=json.loads(v1.MODEL.read_text(encoding="utf-8")); geo=data["minecraft:geometry"][0]
    official=geo["bones"][:v1.OFFICIAL_BONES]
    if len(official)!=v1.OFFICIAL_BONES: raise SystemExit("official Lucario prefix missing")
    extras=cosmetic_bones(); geo["bones"]=official+extras
    v1.MODEL.write_text(json.dumps(data,ensure_ascii=False,separators=(",",":"))+"\n",encoding="utf-8")
    return sum(len(b.get("cubes",[])) for b in extras)


def patch_manifest(cubes:int)->None:
    data=json.loads(v1.MANIFEST.read_text(encoding="utf-8")); data["artStatus"]="ARTISTIC FAIL"
    data["ownerApproval"]={"required":True,"approved":False,"approvedHeadSha":None,"evidenceSetSha256":None,"approvalRecord":None}
    p=data["production"]; p["modelSha256"]=v1.sha256(v1.MODEL); p["productionBoneCount"]=v1.OFFICIAL_BONES+6; p["cosmeticBoneCount"]=6; p["cosmeticCubeCount"]=cubes
    body=next(t for t in p["textures"] if t["role"]=="BODY")
    body.update({"sha256":v1.sha256(v1.BODY),"derivation":"DERIVED_FROM_OFFICIAL","officialBaselineSha256":v1.OFFICIAL_NORMAL_SHA256,"derivedMetadataPath":str(NORMAL_META.relative_to(ROOT))})
    next(t for t in p["textures"] if t["role"]=="OVERLAY")["sha256"]=v1.sha256(v1.OVERLAY)
    for asset in p.get("runtimeAssets",[]):
      if asset.get("role")=="RESOLVER": asset["sha256"]=v1.sha256(v1.RESOLVER)
      if asset.get("role")=="SHINY_BODY": asset["sha256"]=v1.sha256(v1.SHINY)
    b=data["builder"]; b["scriptPath"]="tools/cobblemon-model-review/build_aura_sentinel_resonance_ronin_v22.py"; b["command"]=["python",b["scriptPath"]]
    for path in [NORMAL_META,SHINY_META]:
      raw=str(path.relative_to(ROOT));
      if raw not in b["outputs"]: b["outputs"].append(raw)
    # Remove superseded V21 metadata from reproducibility outputs.
    b["outputs"]=[x for x in b["outputs"] if "v21-derived-" not in x]
    if "DERIVED_TEXTURE_PROVENANCE" not in data["technicalChecks"]: data["technicalChecks"].append("DERIVED_TEXTURE_PROVENANCE")
    q=data["qualityIntent"]
    q["signaturePieces"]=["Open-face thin temple crown","Asymmetric shoulder-to-back mantle crest with broken crescent negative space","Long split waistcoat and restrained full-body cobalt material pass"]
    q["macroFormPlan"]="V22 deletes every V21 legacy cowl/collar/hip component. Six systems remain: open-face temple crown, two-shell mantle root, three-plane diagonal dorsal crest, one breast sash around the chest spike, two unequal split waistcoat tails and shallow animated arm guards. Thin rotated planes create contour and overlap without thick slabs or a rectangular frame."
    q["paintPlan"]="Normal and shiny remain independently derived from exact 1.7.3 baselines. Blue biological texels are deliberately darkened to deep cobalt with restrained facing highlights; dark biology receives indigo occlusion. Cream spikes, white landmarks, red eyes, UV layout, dimensions and alpha semantics remain intact."
    q["gameplayReadGoal"]="At 160 px read one ceremonial aura-sentinel silhouette: open face, diagonal mantle crest and split lower tails. Avoid electric-blue shorts, helmet boxes, portal frames and plate stacks."
    q["iterationNote"]="V21 exact-head Blockbench failed silhouetteDeltaRatio 0.0221 < 0.0400 and direct QA found electric-blue thigh masses plus legacy cowl plates. V22 removes those systems rather than adding onto them, darkens the repaint, and recovers silhouette using thin rotated mantle planes with negative space."
    data["variantCoverage"]["variants"][0]["coverage"]="Default preserves the exact official 87-bone Lucario geometry and uses a validated V22 normal texture derived from the exact official 1.7.3 baseline."
    data["variantCoverage"]["variants"][1]["coverage"]="Shiny uses the same V22 cosmetic geometry and overlay plus an independently derived V22 texture from the exact official shiny 1.7.3 baseline."
    v1.MANIFEST.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")


def main()->None:
    parser=argparse.ArgumentParser(); parser.add_argument("--bootstrap",action="store_true"); args=parser.parse_args()
    require_baselines(); cubes=build_model(); derive(NORMAL_BASELINE,v1.BODY,shiny=False); derive(SHINY_BASELINE,v1.SHINY,shiny=True)
    write_meta(NORMAL_META,NORMAL_BASELINE,v1.BODY,shiny=False); write_meta(SHINY_META,SHINY_BASELINE,v1.SHINY,shiny=True)
    validate(NORMAL_BASELINE,v1.BODY,NORMAL_META,v1.OFFICIAL_NORMAL_SHA256); validate(SHINY_BASELINE,v1.SHINY,SHINY_META,v1.OFFICIAL_SHINY_SHA256)
    write_overlay(v1.OVERLAY); v1.build_resolver()
    if args.bootstrap: patch_manifest(cubes)
    print(json.dumps({"status":"BUILT","concept":"Aura Sentinel — Resonance Ronin V22","officialBones":v1.OFFICIAL_BONES,"cosmeticBones":6,"cosmeticCubes":cubes,"modelSha256":v1.sha256(v1.MODEL),"normalDerivedSha256":v1.sha256(v1.BODY),"shinyDerivedSha256":v1.sha256(v1.SHINY),"overlaySha256":v1.sha256(v1.OVERLAY)},indent=2))

if __name__=="__main__": main()
