#!/usr/bin/env python3
"""Deterministic Aura Sentinel candidate from exact official Cobblemon Lucario.
Presentation only: AutoPTU/Ouros remains authoritative for battle facts.
External same-species skins inform technique only; no third-party bytes or distinctive costume expression are reused.
"""
from __future__ import annotations
import argparse, hashlib, json, urllib.request, zipfile
from io import BytesIO
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
PROJECT_ID="MdwFAVRL"; VERSION_ID="kF7CvxTo"; VERSION="1.7.3"; MC="1.21.1"; LOADER="fabric"
JAR="Cobblemon-fabric-1.7.3+1.21.1.jar"
JAR256="f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3"
JAR512="7b5376f5f48177db53790237b6fb25378806972b5d3b756151b4d8f2d3c27238d6b587b77da422bc1780bfd358b4702e74369fd82cef2a35301b4b68a2f13c2e"
MODEL="assets/cobblemon/bedrock/pokemon/models/0448_lucario/lucario.geo.json"; MODEL256="ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9"
NORMAL="assets/cobblemon/textures/pokemon/0448_lucario/lucario.png"; NORMAL256="98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a"
SHINY="assets/cobblemon/textures/pokemon/0448_lucario/lucario_shiny.png"; SHINY256="b87aaef14b35139b43446e1a85f7031a9594c5443a6a99c03e36e77cab75e84d"
ANIM="assets/cobblemon/bedrock/pokemon/animations/0448_lucario/lucario.animation.json"; ANIM256="ddf880b0830d7649f8cd8811c1c7e2b7fcdee156c850bbeb398f064995fa8563"
POSER="assets/cobblemon/bedrock/pokemon/posers/0448_lucario/lucario.json"; POSER256="7cd9642b38fd1c3e2518cc7f30cd1ea221cac9c89e4b413551151418a4e3c07d"
BASE_RESOLVER="assets/cobblemon/bedrock/pokemon/resolvers/0448_lucario/0_lucario_base.json"; RESOLVER256="a1785270f9f21378e6287b30e3e309de4daa348f21e33fcb8a8b03a134508e81"
LICENSE="assets/cobblemon/bedrock/pokemon/models/0448_lucario/license"; LICENSE256="fb8e971d1895863ec9fc5f3cfc526c64af980bd6c93d0a1615c7969df46a6660"
OUT_MODEL="fabric-adapter/src/main/resources/assets/cobblemon/bedrock/pokemon/models/0448_lucario/ouros_aura_sentinel_lucario.geo.json"
OUT_NORMAL="fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/0448_lucario/ouros_aura_sentinel.png"
OUT_SHINY="fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/0448_lucario/ouros_aura_sentinel_shiny.png"
OUT_OVERLAY="fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/0448_lucario/ouros_aura_sentinel_accessories.png"
OUT_RESOLVER="fabric-adapter/src/main/resources/assets/cobblemon/bedrock/pokemon/resolvers/0448_lucario/90_ouros_aura_sentinel.json"
MANIFEST="docs/cobblemon-skin-review-manifests/0448_lucario.json"; DOSSIER="docs/cobblemon-skin-reference-dossiers/0448_lucario.json"
REGISTRY="docs/cobblemon-skin-registry.json"; SPECIES_DOC="docs/cobblemon-skins/0448_lucario/lucario-aura-sentinel.md"
BB_VER="5.1.6"; BB_SHA="c6dd92036f3c10495df53911a74e5b00a1d557ea13e506084177ef55a5cd7c0e"
PALETTE={"void":(13,18,31,255),"cloth_shadow":(34,31,58,255),"cloth_mid":(62,62,105,255),"cloth_light":(85,104,149,255),"metal_shadow":(79,63,56,255),"metal_mid":(157,119,61,255),"metal_light":(225,198,121,255),"aura":(76,214,239,238),"ivory":(214,225,223,255)}
PIX={}

def sha(b,algo="sha256"):
    h=hashlib.new(algo); h.update(b); return h.hexdigest()
def fsha(p): return sha((ROOT/p).read_bytes())
def get(url):
    req=urllib.request.Request(url,headers={"User-Agent":"Ouros-Skin-Studio/2"})
    with urllib.request.urlopen(req,timeout=120) as r: return r.read()
def checked(zf,path,expected):
    b=zf.read(path); a=sha(b)
    if a!=expected: raise SystemExit(f"official asset drift {path}: {a}")
    return b

def mark_uv(geo):
    used=set(); w=geo["description"]["texture_width"]; h=geo["description"]["texture_height"]
    def mark(x,y,ww,hh):
        for yy in range(max(0,int(y)),min(h,int(y+hh))):
            for xx in range(max(0,int(x)),min(w,int(x+ww))): used.add((xx,yy))
    for bone in geo["bones"]:
        for q in bone.get("cubes",[]):
            uv=q.get("uv"); dx,dy,dz=q.get("size",[0,0,0])
            if isinstance(uv,list):
                u,v=uv
                for r in ((u+dz,v,dx,dz),(u+dz+dx,v,dx,dz),(u,v+dz,dz,dy),(u+dz,v+dz,dx,dy),(u+dz+dx,v+dz,dz,dy),(u+2*dz+dx,v+dz,dx,dy)): mark(*r)
            elif isinstance(uv,dict):
                for face in uv.values():
                    if isinstance(face,dict):
                        p=face.get("uv",[0,0]); s=face.get("uv_size",[1,1]); mark(p[0],p[1],s[0],s[1])
    return used

def choose_pixels(geo):
    w=geo["description"]["texture_width"]; h=geo["description"]["texture_height"]; used=mark_uv(geo)
    free=[(x,y) for y in range(h-1,-1,-1) for x in range(w) if (x,y) not in used]
    if len(free)<len(PALETTE): raise SystemExit("not enough official UV-free texels")
    return {name:free[i] for i,name in enumerate(PALETTE)}
def uv(family):
    maps={
      "cloth":{"north":"cloth_light","south":"cloth_shadow","east":"void","west":"cloth_mid","up":"cloth_light","down":"void"},
      "metal":{"north":"metal_light","south":"metal_shadow","east":"metal_shadow","west":"metal_mid","up":"ivory","down":"void"},
      "aura":{k:"aura" for k in ("north","south","east","west","up","down")}}
    return {face:{"uv":list(PIX[col]),"uv_size":[1,1]} for face,col in maps[family].items()}
def c(origin,size,family,pivot,rotation): return {"origin":origin,"size":size,"uv":uv(family),"pivot":pivot,"rotation":rotation}

def cosmetics():
    # Authored V2: connected sweeping cloth/metal macro-form, deliberate open chest, quiet limb supports.
    return [
      {"name":"ouros_aura_cowl","parent":"head_angle","pivot":[0,37.0,1.0],"cubes":[
        c([-5.4,34.8,0.8],[4.4,4.9,2.2],"cloth",[-3.2,37.0,1.9],[-8,-8,-14]),
        c([0.7,35.0,0.9],[4.0,4.4,2.0],"cloth",[2.7,37.0,1.9],[-8,8,11]),
        c([-4.9,37.5,-3.2],[3.1,0.65,0.55],"metal",[-3.3,37.8,-2.9],[0,-7,-20]),
        c([1.6,37.5,-3.1],[2.7,0.58,0.5],"metal",[2.9,37.8,-2.85],[0,7,17])]},
      {"name":"ouros_aura_mantle","parent":"torso3","pivot":[-4.4,29.0,1.0],"cubes":[
        c([-9.5,27.4,-2.5],[9.4,3.7,5.8],"cloth",[-4.8,29.2,0.4],[5,-11,-18]),
        c([-10.4,24.2,-1.8],[8.8,3.5,5.0],"cloth",[-6.0,25.9,0.7],[8,-13,-28]),
        c([-10.3,20.9,-0.9],[7.8,3.3,4.2],"cloth",[-6.4,22.5,1.2],[11,-15,-38]),
        c([-9.6,17.9,0.0],[6.6,3.0,3.3],"cloth",[-6.3,19.4,1.6],[14,-17,-47]),
        c([-8.5,15.3,0.8],[5.2,2.6,2.6],"cloth",[-5.9,16.6,2.1],[17,-18,-55]),
        c([-8.4,28.6,-3.0],[6.9,0.55,0.45],"metal",[-5.0,28.9,-2.8],[4,-10,-18])]},
      {"name":"ouros_aura_open_cuirass","parent":"torso3","pivot":[0,27.7,-3.9],"cubes":[
        c([-5.0,27.8,-4.5],[5.5,2.25,0.72],"metal",[-2.25,28.9,-4.1],[0,-2,-28]),
        c([-4.0,25.3,-4.55],[4.8,1.9,0.66],"cloth",[-1.6,26.2,-4.2],[0,-2,-39]),
        c([0.2,27.7,-4.5],[4.8,2.0,0.70],"cloth",[2.6,28.7,-4.15],[0,2,25]),
        c([0.8,25.6,-4.55],[4.0,1.6,0.63],"metal",[2.8,26.4,-4.2],[0,2,36]),
        c([-1.0,26.7,-4.82],[2.0,2.0,0.32],"aura",[0,27.7,-4.65],[0,0,45])]},
      {"name":"ouros_aura_sash","parent":"torso","pivot":[-1.6,19.0,2.6],"cubes":[
        c([-5.8,17.8,1.5],[8.6,1.6,2.6],"metal",[-1.5,18.6,2.8],[-8,0,-12]),
        c([-5.2,15.1,2.2],[7.0,2.2,2.0],"cloth",[-1.7,16.2,3.2],[-12,0,-22]),
        c([-4.4,12.9,2.8],[5.5,1.8,1.55],"cloth",[-1.7,13.8,3.55],[-15,0,-31])]},
      {"name":"ouros_aura_left_vambrace","parent":"arm_left2","pivot":[10.8,29.2,-0.6],"cubes":[
        c([9.0,27.8,-2.2],[4.0,1.35,3.2],"metal",[11.0,28.5,-0.6],[-7,0,-9]),
        c([9.7,29.8,-2.0],[2.9,0.85,2.5],"cloth",[11.15,30.2,-0.75],[-10,0,-6])]},
      {"name":"ouros_aura_left_greave","parent":"leg_left4","pivot":[3.4,4.0,-1.1],"cubes":[
        c([1.2,0.1,-2.15],[4.3,1.4,2.8],"metal",[3.4,0.8,-0.75],[-8,0,-8]),
        c([1.7,3.1,-2.2],[3.4,1.2,2.5],"cloth",[3.4,3.7,-0.95],[-11,0,-5]),
        c([2.1,5.5,-2.15],[2.7,0.72,2.0],"metal",[3.45,5.9,-1.15],[-13,0,-3])]},
      {"name":"ouros_aura_right_greave","parent":"leg_right4","pivot":[-3.4,4.0,-1.1],"cubes":[
        c([-5.5,0.1,-2.15],[4.3,1.4,2.8],"metal",[-3.4,0.8,-0.75],[-8,0,8]),
        c([-5.1,3.1,-2.2],[3.4,1.2,2.5],"cloth",[-3.4,3.7,-0.95],[-11,0,5]),
        c([-4.8,5.5,-2.15],[2.7,0.72,2.0],"metal",[-3.45,5.9,-1.15],[-13,0,3])]},
    ]

def write_overlay(path,size):
    im=Image.new("RGBA",size,(0,0,0,0)); px=im.load()
    for name,color in PALETTE.items(): x,y=PIX[name]; px[x,y]=color
    path.parent.mkdir(parents=True,exist_ok=True); im.save(path,optimize=False,compress_level=9)
def write_resolver(path):
    obj={"species":"cobblemon:lucario","order":90,"variations":[
      {"aspects":["ouros_aura_sentinel"],"poser":"cobblemon:lucario","model":"cobblemon:ouros_aura_sentinel_lucario.geo","texture":"cobblemon:textures/pokemon/0448_lucario/ouros_aura_sentinel.png","layers":[{"name":"ouros_aura_sentinel","texture":"cobblemon:textures/pokemon/0448_lucario/ouros_aura_sentinel_accessories.png","translucent":True}]},
      {"aspects":["ouros_aura_sentinel","shiny"],"poser":"cobblemon:lucario","model":"cobblemon:ouros_aura_sentinel_lucario.geo","texture":"cobblemon:textures/pokemon/0448_lucario/ouros_aura_sentinel_shiny.png","layers":[{"name":"ouros_aura_sentinel","texture":"cobblemon:textures/pokemon/0448_lucario/ouros_aura_sentinel_accessories.png","translucent":True}]}]}
    path.parent.mkdir(parents=True,exist_ok=True); path.write_text(json.dumps(obj,separators=(",",":"))+"\n")
def download():
    meta=json.loads(get(f"https://api.modrinth.com/v2/version/{VERSION_ID}"))
    if meta["project_id"]!=PROJECT_ID or meta["version_number"]!=VERSION or meta["version_type"]!="release" or MC not in meta["game_versions"] or LOADER not in meta["loaders"]: raise SystemExit("Modrinth target drift")
    primary=next(x for x in meta["files"] if x.get("primary"))
    if primary["filename"]!=JAR: raise SystemExit("official filename drift")
    b=get(primary["url"])
    if sha(b)!=JAR256 or sha(b,"sha512")!=JAR512: raise SystemExit("official JAR hash drift")
    return b

def build():
    global PIX
    with zipfile.ZipFile(BytesIO(download())) as zf:
        mb=checked(zf,MODEL,MODEL256); nb=checked(zf,NORMAL,NORMAL256); sb=checked(zf,SHINY,SHINY256)
        checked(zf,ANIM,ANIM256); checked(zf,POSER,POSER256); checked(zf,BASE_RESOLVER,RESOLVER256); checked(zf,LICENSE,LICENSE256)
    data=json.loads(mb); geo=data["minecraft:geometry"][0]
    if len(geo["bones"])!=87: raise SystemExit("official Lucario bone count drift")
    PIX=choose_pixels(geo); geo["description"]["identifier"]="geometry.ouros_aura_sentinel_lucario"; extra=cosmetics(); geo["bones"].extend(extra)
    p=ROOT/OUT_MODEL; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(json.dumps(data,separators=(",",":"))+"\n")
    for out,b in ((OUT_NORMAL,nb),(OUT_SHINY,sb)): q=ROOT/out; q.parent.mkdir(parents=True,exist_ok=True); q.write_bytes(b)
    write_overlay(ROOT/OUT_OVERLAY,(geo["description"]["texture_width"],geo["description"]["texture_height"])); write_resolver(ROOT/OUT_RESOLVER)
    return {"originalBones":87,"cosmeticBones":len(extra),"cosmeticCubes":sum(len(x["cubes"]) for x in extra)}
def bootstrap(info):
    manifest={
      "format":"ouros.cobblemon-professional-skin-review.v1","species":"lucario","nationalDex":448,"concept":"Aura Sentinel","authorityBoundary":"PRESENTATION_ONLY_AUTOPTU_AUTHORITATIVE","artStatus":"ARTISTIC FAIL",
      "ownerApproval":{"required":True,"approved":False,"approvedHeadSha":None,"evidenceSetSha256":None,"approvalRecord":None},"referenceDossier":DOSSIER,
      "officialSource":{"modrinthProjectId":PROJECT_ID,"modrinthVersionId":VERSION_ID,"version":VERSION,"minecraftVersion":MC,"loader":LOADER,"jarFilename":JAR,"jarSha256":JAR256,"jarSha512":JAR512,"releaseChannel":"release","enforceLatestCompatibleStable":True,"modelPath":MODEL,"modelSha256":MODEL256,"officialBoneCount":87,"referenceTexture":{"path":NORMAL,"sha256":NORMAL256},"animationPath":ANIM,"animationSha256":ANIM256,"auxiliaryAssets":[{"role":"POSER","path":POSER,"sha256":POSER256},{"role":"RESOLVER","path":BASE_RESOLVER,"sha256":RESOLVER256},{"role":"MODEL_LICENSE","path":LICENSE,"sha256":LICENSE256}]},
      "production":{"modelPath":OUT_MODEL,"modelSha256":fsha(OUT_MODEL),"productionBoneCount":87+info["cosmeticBones"],"cosmeticBoneCount":info["cosmeticBones"],"cosmeticCubeCount":info["cosmeticCubes"],"attachmentGate":{"anchorGap":1.5,"pieceGap":1.0},"textures":[{"role":"BODY","path":OUT_NORMAL,"sha256":fsha(OUT_NORMAL),"derivation":"OFFICIAL_IDENTICAL"},{"role":"OVERLAY","path":OUT_OVERLAY,"sha256":fsha(OUT_OVERLAY),"derivation":"ACCESSORY_OVERLAY"}],"runtimeAssets":[{"role":"RESOLVER","path":OUT_RESOLVER,"sha256":fsha(OUT_RESOLVER)},{"role":"SHINY_TEXTURE","path":OUT_SHINY,"sha256":fsha(OUT_SHINY)}]},
      "builder":{"deterministic":True,"scriptPath":"tools/cobblemon-model-review/build_aura_sentinel_professional.py","command":["python","tools/cobblemon-model-review/build_aura_sentinel_professional.py"],"outputs":[OUT_MODEL,OUT_NORMAL,OUT_OVERLAY,OUT_RESOLVER,OUT_SHINY]},
      "blockbench":{"version":BB_VER,"appImageSha256":BB_SHA,"matchedCamera":True,"gameplayResolution":160,"heroAnimation":"animation.lucario.ground_idle","heroAnimationTime":0.35,"battleAnimation":"animation.lucario.battle_idle","battleAnimationTime":0.35,"requiredEvidenceNames":["official_reference_three_quarter.png","hero_three_quarter.png","battle_ready_three_quarter.png","hero_front.png","hero_back.png","hero_gameplay_160.png"],"technicalVisualFloor":{"minimumPixelDifferenceRatio":0.03,"minimumSilhouetteDeltaRatio":0.02}},
      "evidence":{"artifactName":"lucario-aura-sentinel-professional-review","reviewContractFile":"review-contract.json","pngHashManifestFile":"png-sha256.txt","requiredFiles":["official_reference_three_quarter.png","hero_three_quarter.png","battle_ready_three_quarter.png","hero_front.png","hero_back.png","hero_gameplay_160.png","contact_sheet.png","review-contract.json","png-sha256.txt"]},
      "qualityIntent":{"referenceLessons":["Ruins Style: use staged parented drape segments to create a continuous flexible sweep rather than bars.","Space Style: distribute overlapping shell treatment across torso and lower body while preserving biological landmarks.","Covert Style: create a dominant head-to-torso garment read with animation-safe parenting and deliberate openings."],"signaturePieces":["Single left-weighted sweeping mantle descending from shoulder to hip","Open split cuirass framing the biological chest spike and aura diamond"],"macroFormPlan":"One connected asymmetric cloth sweep dominates three-quarter and rear views; the open chest V and tapered sash preserve negative space; only one vambrace and paired quiet greaves support the full-body read.","paintPlan":"Accessory overlay uses directional dark-cloth, indigo-facing, warm-metal and ivory highlight texels with shadowed opposite faces; biological normal/shiny textures remain exact official bytes.","gameplayReadGoal":"At 160 px: Lucario first, then one unmistakable sweeping ceremonial mantle and luminous open chest V; no cage, portal frame, or repeated bar read.","antiPatternsToReject":["base Pokemon plus scattered accessories","rectangular cage or portal frame","repeated straight bars","boxy shoulder slabs","flat one-value accessory paint"],"thirdPartyReusePolicy":"TECHNIQUES_ONLY_UNLESS_LICENSED_DERIVATIVE_DONOR"},
      "variantCoverage":{"audited":True,"variants":[{"name":"default","coverage":"Exact official Lucario geometry and normal texture; no male/female geometry split on this resolver path."},{"name":"shiny","coverage":"Same exact official geometry with independently extracted official shiny texture; shared presentation overlay."}]},
      "technicalChecks":["REFERENCE_DOSSIER","OFFICIAL_SOURCE_HASHES","ORIGINAL_BONE_EQUALITY","COSMETIC_ATTACHMENT","BUILDER_REPRODUCIBILITY","BLOCKBENCH_MATCHED_CAMERA","GAMEPLAY_SCALE_EVIDENCE","PLAYABLE_TEST_BUILD","INTEGRATION_CORE_CI"]}
    mp=ROOT/MANIFEST; mp.parent.mkdir(parents=True,exist_ok=True); mp.write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+"\n")
    reg=json.loads((ROOT/REGISTRY).read_text()); e=next(x for x in reg["entries"] if x["slug"]=="0448_lucario"); e.update({"lifecycle":"PROFESSIONAL_CANDIDATE","artStatus":"ARTISTIC FAIL","saleEligible":False,"manifest":MANIFEST,"blocker":"Current professional candidate requires Blockbench QA and explicit owner review; no artistic approval exists."}); (ROOT/REGISTRY).write_text(json.dumps(reg,indent=2,ensure_ascii=False)+"\n")
    doc=ROOT/SPECIES_DOC; text=doc.read_text(); lines=text.splitlines(); lines[2]="Status: ARTISTIC FAIL"; lines[3]="Sale eligibility: NOT ELIGIBLE."; note="\nProfessional V2 candidate: deterministic current-baseline build with 7 Ouros groups and a single connected asymmetric mantle/open-cuirass hierarchy. Historical visual claims below are provenance only and do not describe this candidate.\n"; doc.write_text("\n".join(lines[:5])+note+"\n".join(lines[5:])+"\n")
    print(json.dumps({"status":"BOOTSTRAPPED","info":info,"manifest":MANIFEST},indent=2))
def main():
    ap=argparse.ArgumentParser(); ap.add_argument("--bootstrap",action="store_true"); a=ap.parse_args(); info=build(); bootstrap(info) if a.bootstrap else print(json.dumps({"status":"BUILT","info":info},indent=2))
if __name__=="__main__": main()
