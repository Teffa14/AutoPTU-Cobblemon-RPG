#!/usr/bin/env python3
"""Build the professional Aura Sentinel candidate from the exact official Lucario JAR.

Presentation only. AutoPTU/Ouros remains authoritative for battle facts.
The builder downloads one pinned official Cobblemon release, preserves every
official Lucario bone byte-structurally, and appends only original `ouros_*`
cosmetic groups. Third-party references inform generic construction technique
only; no external model, UV, texture, costume, palette layout, or motif is copied.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import urllib.request
import zipfile
from io import BytesIO
from pathlib import Path
from typing import Any

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
VERSION_ID = "kF7CvxTo"
PROJECT_ID = "MdwFAVRL"
VERSION = "1.7.3"
MC_VERSION = "1.21.1"
LOADER = "fabric"
JAR_FILENAME = "Cobblemon-fabric-1.7.3+1.21.1.jar"
JAR_SHA256 = "f7c25955176badc444ad6211fc556514fedbdba776227f105fe899f8819d74e3"
JAR_SHA512 = "7b5376f5f48177db53790237b6fb25378806972b5d3b756151b4d8f2d3c27238d6b587b77da422bc1780bfd358b4702e74369fd82cef2a35301b4b68a2f13c2e"
MODEL_PATH = "assets/cobblemon/bedrock/pokemon/models/0448_lucario/lucario.geo.json"
MODEL_SHA256 = "ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9"
ANIMATION_PATH = "assets/cobblemon/bedrock/pokemon/animations/0448_lucario/lucario.animation.json"
ANIMATION_SHA256 = "ddf880b0830d7649f8cd8811c1c7e2b7fcdee156c850bbeb398f064995fa8563"
NORMAL_PATH = "assets/cobblemon/textures/pokemon/0448_lucario/lucario.png"
NORMAL_SHA256 = "98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a"
SHINY_PATH = "assets/cobblemon/textures/pokemon/0448_lucario/lucario_shiny.png"
SHINY_SHA256 = "b87aaef14b35139b43446e1a85f7031a9594c5443a6a99c03e36e77cab75e84d"
AUX_HASHES = {
    "POSER": "7cd9642b38fd1c3e2518cc7f30cd1ea221cac9c89e4b413551151418a4e3c07d",
    "RESOLVER": "a1785270f9f21378e6287b30e3e309de4daa348f21e33fcb8a8b03a134508e81",
    "MODEL_LICENSE": "fb8e971d1895863ec9fc5f3cfc526c64af980bd6c93d0a1615c7969df46a6660",
}

MODEL_OUT = "fabric-adapter/src/main/resources/assets/cobblemon/bedrock/pokemon/models/0448_lucario/ouros_aura_sentinel_lucario.geo.json"
NORMAL_OUT = "fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/0448_lucario/ouros_aura_sentinel.png"
SHINY_OUT = "fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/0448_lucario/ouros_aura_sentinel_shiny.png"
OVERLAY_OUT = "fabric-adapter/src/main/resources/assets/cobblemon/textures/pokemon/0448_lucario/ouros_aura_sentinel_accessories.png"
RESOLVER_OUT = "fabric-adapter/src/main/resources/assets/cobblemon/bedrock/pokemon/resolvers/0448_lucario/90_ouros_aura_sentinel.json"
MANIFEST_OUT = "docs/cobblemon-skin-review-manifests/0448_lucario.json"
REGISTRY = "docs/cobblemon-skin-registry.json"
SPECIES_DOC = "docs/cobblemon-skins/0448_lucario/lucario-aura-sentinel.md"
DOSSIER = "docs/cobblemon-skin-reference-dossiers/0448_lucario.json"

PALETTE = {
    "shadow": (15, 18, 30, 255),
    "ink": (27, 36, 62, 255),
    "violet": (69, 55, 102, 255),
    "cobalt": (44, 88, 128, 255),
    "gold_dark": (142, 96, 38, 255),
    "gold_light": (216, 184, 104, 255),
    "aura": (79, 210, 235, 235),
    "ivory": (211, 222, 219, 255),
}
PIXELS: dict[str, tuple[int, int]] = {}


def digest(data: bytes, algo: str = "sha256") -> str:
    h = hashlib.new(algo); h.update(data); return h.hexdigest()


def file_sha(path: str) -> str:
    return digest((ROOT / path).read_bytes())


def fetch(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": "Ouros-Skin-Studio/1"})
    with urllib.request.urlopen(req, timeout=120) as response:
        return response.read()


def download_jar() -> tuple[bytes, dict[str, Any]]:
    meta = json.loads(fetch(f"https://api.modrinth.com/v2/version/{VERSION_ID}").decode())
    if meta["project_id"] != PROJECT_ID or meta["version_number"] != VERSION:
        raise SystemExit("pinned Modrinth metadata drifted")
    if MC_VERSION not in meta["game_versions"] or LOADER not in meta["loaders"] or meta["version_type"] != "release":
        raise SystemExit("pinned Cobblemon release is no longer the declared compatible stable target")
    primary = next((f for f in meta["files"] if f.get("primary")), None)
    if not primary or primary["filename"] != JAR_FILENAME:
        raise SystemExit("official primary filename drifted")
    jar = fetch(primary["url"])
    if digest(jar) != JAR_SHA256 or digest(jar, "sha512") != JAR_SHA512:
        raise SystemExit("official JAR hash mismatch")
    return jar, meta


def checked(zf: zipfile.ZipFile, path: str, expected: str) -> bytes:
    data = zf.read(path)
    actual = digest(data)
    if actual != expected:
        raise SystemExit(f"official asset drift: {path} expected={expected} actual={actual}")
    return data


def find_path_by_hash(zf: zipfile.ZipFile, expected: str) -> str:
    matches = []
    for name in zf.namelist():
        if name.endswith("/"):
            continue
        try:
            data = zf.read(name)
        except Exception:
            continue
        if digest(data) == expected:
            matches.append(name)
    if len(matches) != 1:
        raise SystemExit(f"expected exactly one official asset for hash {expected}, found {matches}")
    return matches[0]


def mark_uv_usage(geo: dict) -> set[tuple[int, int]]:
    width = geo["description"]["texture_width"]; height = geo["description"]["texture_height"]
    used: set[tuple[int, int]] = set()
    def mark(x: float, y: float, w: float, h: float) -> None:
        x0, x1 = sorted((int(x), int(x + w))); y0, y1 = sorted((int(y), int(y + h)))
        for yy in range(max(0, y0), min(height, y1)):
            for xx in range(max(0, x0), min(width, x1)):
                used.add((xx, yy))
    for bone in geo["bones"]:
        for item in bone.get("cubes", []):
            dx, dy, dz = item.get("size", [0, 0, 0]); uv = item.get("uv", [0, 0])
            if isinstance(uv, list):
                u, v = uv
                rects = ((u+dz,v,dx,dz),(u+dz+dx,v,dx,dz),(u,v+dz,dz,dy),(u+dz,v+dz,dx,dy),(u+dz+dx,v+dz,dz,dy),(u+2*dz+dx,v+dz,dx,dy))
                for rect in rects: mark(*rect)
            elif isinstance(uv, dict):
                for face in uv.values():
                    if isinstance(face, dict):
                        p = face.get("uv", [0,0]); s = face.get("uv_size", [1,1]); mark(p[0],p[1],s[0],s[1])
    return used


def choose_pixels(geo: dict) -> dict[str, tuple[int, int]]:
    width = geo["description"]["texture_width"]; height = geo["description"]["texture_height"]
    used = mark_uv_usage(geo)
    free = [(x,y) for y in range(height-1,-1,-1) for x in range(width) if (x,y) not in used]
    if len(free) < len(PALETTE): raise SystemExit("insufficient official UV-free texels")
    return {name: free[i] for i, name in enumerate(PALETTE)}


def face_uv(family: str) -> dict:
    schemes = {
        "cloth": {"north":"violet","south":"ink","east":"shadow","west":"ink","up":"cobalt","down":"shadow"},
        "lacquer": {"north":"cobalt","south":"ink","east":"shadow","west":"violet","up":"ivory","down":"shadow"},
        "gold": {"north":"gold_light","south":"gold_dark","east":"gold_dark","west":"gold_light","up":"ivory","down":"gold_dark"},
        "aura": {face:"aura" for face in ("north","south","east","west","up","down")},
    }
    return {face: {"uv": list(PIXELS[color]), "uv_size": [1,1]} for face, color in schemes[family].items()}


def cube(origin, size, family, *, pivot=None, rotation=None, inflate=None) -> dict:
    out = {"origin": origin, "size": size, "uv": face_uv(family)}
    if pivot is not None: out["pivot"] = pivot
    if rotation is not None: out["rotation"] = rotation
    if inflate is not None: out["inflate"] = inflate
    return out


def cosmetics() -> list[dict]:
    c = cube
    return [
        {"name":"ouros_aura_crown","parent":"head_angle","pivot":[0,37.4,-2.3],"cubes":[
            c([-4.9,36.9,-4.3],[4.2,1.15,0.75],"lacquer",pivot=[-2.8,37.5,-3.9],rotation=[0,-5,-17]),
            c([0.7,36.9,-4.3],[4.2,1.15,0.75],"lacquer",pivot=[2.8,37.5,-3.9],rotation=[0,5,17]),
            c([-4.7,38.0,-2.3],[1.5,3.8,2.3],"cloth",pivot=[-4.0,38.3,-1.2],rotation=[-8,-8,-24]),
            c([3.4,38.0,-2.1],[1.25,2.8,2.0],"gold",pivot=[4.0,38.4,-1.1],rotation=[-7,7,18]),
        ]},
        {"name":"ouros_aura_processional_mantle","parent":"torso3","pivot":[-4.0,29.5,0.5],"cubes":[
            c([-9.2,28.2,-3.0],[9.4,3.2,6.2],"cloth",pivot=[-4.5,29.8,0.1],rotation=[4,-10,-18]),
            c([-10.7,25.3,-2.4],[8.8,2.8,5.7],"cloth",pivot=[-6.3,26.7,0.4],rotation=[7,-13,-30]),
            c([-11.0,22.4,-1.6],[7.7,2.45,4.9],"lacquer",pivot=[-7.2,23.6,0.8],rotation=[10,-16,-41]),
            c([-10.2,19.7,-0.7],[6.3,2.0,4.0],"cloth",pivot=[-7.0,20.7,1.3],rotation=[12,-18,-51]),
            c([-8.8,17.5,0.1],[5.0,1.6,3.2],"lacquer",pivot=[-6.3,18.3,1.7],rotation=[14,-20,-59]),
            c([-8.2,29.0,-3.45],[6.8,0.55,0.34],"gold",pivot=[-4.8,29.3,-3.28],rotation=[4,-10,-18]),
        ]},
        {"name":"ouros_aura_split_cuirass","parent":"torso3","pivot":[0,27.7,-3.7],"cubes":[
            c([-5.0,28.2,-4.45],[5.9,2.0,0.72],"lacquer",pivot=[-2.1,29.2,-4.1],rotation=[0,-2,-27]),
            c([-4.0,25.4,-4.5],[5.6,1.85,0.72],"lacquer",pivot=[-1.2,26.3,-4.14],rotation=[0,-2,-43]),
            c([0.1,28.1,-4.4],[4.9,1.8,0.68],"cloth",pivot=[2.55,29.0,-4.06],rotation=[0,2,24]),
            c([0.7,25.8,-4.45],[4.3,1.55,0.65],"cloth",pivot=[2.85,26.6,-4.12],rotation=[0,2,39]),
            c([-1.15,27.0,-4.78],[2.3,2.3,0.35],"aura",pivot=[0,28.15,-4.6],rotation=[0,0,45]),
        ]},
        {"name":"ouros_aura_rear_drape","parent":"torso3","pivot":[-2.8,27.0,2.8],"cubes":[
            c([-6.8,25.5,2.4],[7.3,2.2,2.4],"cloth",pivot=[-3.2,26.6,3.6],rotation=[-9,-4,-14]),
            c([-6.9,21.7,2.8],[6.5,3.2,2.0],"cloth",pivot=[-3.7,23.3,3.7],rotation=[-12,-4,-21]),
            c([-6.4,17.8,3.1],[5.6,3.3,1.7],"cloth",pivot=[-3.6,19.5,3.8],rotation=[-15,-4,-29]),
            c([-5.6,14.5,3.35],[4.6,2.8,1.45],"lacquer",pivot=[-3.3,15.9,4.0],rotation=[-18,-4,-37]),
        ]},
        {"name":"ouros_aura_left_vambrace","parent":"arm_left2","pivot":[10.3,29.5,-0.3],"cubes":[
            c([8.7,28.1,-2.1],[4.1,1.25,3.2],"lacquer",pivot=[10.7,29.0,-0.5],rotation=[-5,0,-8]),
            c([9.2,30.0,-2.0],[3.4,1.05,2.8],"cloth",pivot=[10.9,30.5,-0.6],rotation=[-8,0,-6]),
            c([10.0,31.5,-1.85],[2.2,0.65,2.2],"gold",pivot=[11.1,31.8,-0.75],rotation=[-10,0,-4]),
        ]},
        {"name":"ouros_aura_right_vambrace","parent":"arm_right2","pivot":[-10.3,29.5,-0.3],"cubes":[
            c([-12.8,28.1,-2.1],[4.1,1.25,3.2],"lacquer",pivot=[-10.7,29.0,-0.5],rotation=[-5,0,8]),
            c([-12.6,30.0,-2.0],[3.4,1.05,2.8],"cloth",pivot=[-10.9,30.5,-0.6],rotation=[-8,0,6]),
            c([-12.2,31.5,-1.85],[2.2,0.65,2.2],"gold",pivot=[-11.1,31.8,-0.75],rotation=[-10,0,4]),
        ]},
        {"name":"ouros_aura_waist_sash","parent":"torso","pivot":[-1.5,19.5,2.6],"cubes":[
            c([-5.9,18.6,1.5],[8.9,1.65,2.6],"lacquer",pivot=[-1.45,19.4,2.8],rotation=[-7,0,-12]),
            c([-5.4,16.1,2.2],[7.4,1.55,2.1],"cloth",pivot=[-1.7,16.9,3.2],rotation=[-10,0,-20]),
            c([-4.7,14.0,2.75],[6.0,1.25,1.7],"gold",pivot=[-1.7,14.6,3.6],rotation=[-12,0,-28]),
        ]},
        {"name":"ouros_aura_left_greave","parent":"leg_left4","pivot":[3.4,4.0,-1.2],"cubes":[
            c([1.2,0.0,-2.05],[4.4,1.45,2.8],"lacquer",pivot=[3.4,1.0,-0.65],rotation=[-7,0,-7]),
            c([1.5,3.0,-2.15],[3.8,1.25,2.5],"cloth",pivot=[3.4,3.6,-0.9],rotation=[-10,0,-5]),
            c([2.1,5.6,-2.15],[2.7,0.75,2.0],"gold",pivot=[3.45,5.95,-1.15],rotation=[-12,0,-3]),
        ]},
        {"name":"ouros_aura_right_greave","parent":"leg_right4","pivot":[-3.4,4.0,-1.2],"cubes":[
            c([-5.6,0.0,-2.05],[4.4,1.45,2.8],"lacquer",pivot=[-3.4,1.0,-0.65],rotation=[-7,0,7]),
            c([-5.3,3.0,-2.15],[3.8,1.25,2.5],"cloth",pivot=[-3.4,3.6,-0.9],rotation=[-10,0,5]),
            c([-4.8,5.6,-2.15],[2.7,0.75,2.0],"gold",pivot=[-3.45,5.95,-1.15],rotation=[-12,0,3]),
        ]},
    ]


def write_png_overlay(path: Path, size: tuple[int,int]) -> None:
    image = Image.new("RGBA", size, (0,0,0,0))
    px = image.load()
    for name, color in PALETTE.items():
        x,y = PIXELS[name]; px[x,y] = color
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, optimize=False, compress_level=9)


def write_resolver(path: Path) -> None:
    payload = {
        "species":"cobblemon:lucario","order":90,"variations":[
            {"aspects":["ouros_aura_sentinel"],"poser":"cobblemon:lucario","model":"cobblemon:ouros_aura_sentinel_lucario.geo","texture":"cobblemon:textures/pokemon/0448_lucario/ouros_aura_sentinel.png","layers":[{"name":"ouros_aura_sentinel","texture":"cobblemon:textures/pokemon/0448_lucario/ouros_aura_sentinel_accessories.png","translucent":True}]},
            {"aspects":["ouros_aura_sentinel","shiny"],"poser":"cobblemon:lucario","model":"cobblemon:ouros_aura_sentinel_lucario.geo","texture":"cobblemon:textures/pokemon/0448_lucario/ouros_aura_sentinel_shiny.png","layers":[{"name":"ouros_aura_sentinel","texture":"cobblemon:textures/pokemon/0448_lucario/ouros_aura_sentinel_accessories.png","translucent":True}]},
        ]}
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, separators=(",", ":")) + "\n", encoding="utf-8")


def build() -> dict:
    global PIXELS
    jar, _ = download_jar()
    with zipfile.ZipFile(BytesIO(jar)) as zf:
        model_bytes = checked(zf, MODEL_PATH, MODEL_SHA256)
        normal = checked(zf, NORMAL_PATH, NORMAL_SHA256)
        shiny = checked(zf, SHINY_PATH, SHINY_SHA256)
        checked(zf, ANIMATION_PATH, ANIMATION_SHA256)
        aux_paths = {role: find_path_by_hash(zf, h) for role,h in AUX_HASHES.items()}
    data = json.loads(model_bytes.decode("utf-8"))
    geo = data["minecraft:geometry"][0]
    original = len(geo["bones"])
    if original != 87: raise SystemExit(f"official Lucario bone count drifted: {original}")
    PIXELS = choose_pixels(geo)
    geo["description"]["identifier"] = "geometry.ouros_aura_sentinel_lucario"
    additions = cosmetics()
    geo["bones"].extend(additions)
    model_path = ROOT / MODEL_OUT; model_path.parent.mkdir(parents=True, exist_ok=True)
    model_path.write_text(json.dumps(data, separators=(",", ":")) + "\n", encoding="utf-8")
    for raw, content in ((NORMAL_OUT,normal),(SHINY_OUT,shiny)):
        p=ROOT/raw; p.parent.mkdir(parents=True,exist_ok=True); p.write_bytes(content)
    write_png_overlay(ROOT/OVERLAY_OUT, (geo["description"]["texture_width"], geo["description"]["texture_height"]))
    write_resolver(ROOT/RESOLVER_OUT)
    return {"originalBones":original,"cosmeticBones":len(additions),"cosmeticCubes":sum(len(b["cubes"]) for b in additions),"auxPaths":aux_paths,"palettePixels":PIXELS}


def bootstrap(info: dict) -> None:
    manifest = {
      "format":"ouros.cobblemon-professional-skin-review.v1","species":"lucario","nationalDex":448,"concept":"Aura Sentinel",
      "authorityBoundary":"PRESENTATION_ONLY_AUTOPTU_AUTHORITATIVE","artStatus":"ARTISTIC FAIL",
      "ownerApproval":{"required":True,"approved":False,"approvedHeadSha":None,"evidenceSetSha256":None,"approvalRecord":None},
      "referenceDossier":DOSSIER,
      "officialSource":{"modrinthProjectId":PROJECT_ID,"modrinthVersionId":VERSION_ID,"version":VERSION,"minecraftVersion":MC_VERSION,"loader":LOADER,"jarFilename":JAR_FILENAME,"jarSha256":JAR_SHA256,"jarSha512":JAR_SHA512,"releaseChannel":"release","enforceLatestCompatibleStable":True,"modelPath":MODEL_PATH,"modelSha256":MODEL_SHA256,"officialBoneCount":87,"referenceTexture":{"path":NORMAL_PATH,"sha256":NORMAL_SHA256},"animationPath":ANIMATION_PATH,"animationSha256":ANIMATION_SHA256,"auxiliaryAssets":[{"role":role,"path":info["auxPaths"][role],"sha256":AUX_HASHES[role]} for role in ("POSER","RESOLVER","MODEL_LICENSE")]},
      "production":{"modelPath":MODEL_OUT,"modelSha256":file_sha(MODEL_OUT),"productionBoneCount":info["originalBones"]+info["cosmeticBones"],"cosmeticBoneCount":info["cosmeticBones"],"cosmeticCubeCount":info["cosmeticCubes"],"attachmentGate":{"anchorGap":1.5,"pieceGap":1.0},"textures":[{"role":"BODY","path":NORMAL_OUT,"sha256":file_sha(NORMAL_OUT),"derivation":"OFFICIAL_IDENTICAL"},{"role":"OVERLAY","path":OVERLAY_OUT,"sha256":file_sha(OVERLAY_OUT),"derivation":"ACCESSORY_OVERLAY"}],"runtimeAssets":[{"role":"RESOLVER","path":RESOLVER_OUT,"sha256":file_sha(RESOLVER_OUT)},{"role":"SHINY_TEXTURE","path":SHINY_OUT,"sha256":file_sha(SHINY_OUT)}]},
      "builder":{"deterministic":True,"scriptPath":"tools/cobblemon-model-review/build_aura_sentinel_professional.py","command":["python","tools/cobblemon-model-review/build_aura_sentinel_professional.py"],"outputs":[MODEL_OUT,NORMAL_OUT,OVERLAY_OUT,RESOLVER_OUT,SHINY_OUT]},
      "blockbench":{"version":"5.1.6","appImageSha256":"c6dd92036f3c10495df53911a74e5b00a1d557ea13e506084177ef55a5cd7c0e","matchedCamera":True,"gameplayResolution":160,"heroAnimation":"animation.lucario.ground_idle","heroAnimationTime":0.35,"battleAnimation":"animation.lucario.battle_idle","battleAnimationTime":0.35,"requiredEvidenceNames":["official_reference_three_quarter.png","hero_three_quarter.png","battle_ready_three_quarter.png","hero_front.png","hero_back.png","hero_gameplay_160.png"] ,"technicalVisualFloor":{"minimumPixelDifferenceRatio":0.03,"minimumSilhouetteDeltaRatio":0.02}},
      "evidence":{"artifactName":"lucario-aura-sentinel-professional-review","reviewContractFile":"review-contract.json","pngHashManifestFile":"png-sha256.txt","requiredFiles":["official_reference_three_quarter.png","hero_three_quarter.png","battle_ready_three_quarter.png","hero_front.png","hero_back.png","hero_gameplay_160.png","contact_sheet.png","review-contract.json","png-sha256.txt"]},
      "qualityIntent":{"referenceLessons":["Use the Ruins Style lesson of multi-stage parented drape construction to make one continuous mantle rather than isolated rigid bars.","Use the Space Style lesson of overlapping shells with distinct depth so the chest, arms, and legs participate without hiding Lucario anatomy.","Use the Covert Style lesson of a dominant head-to-torso garment read and motion-parented limb overlays while preserving open face and negative space."],"signaturePieces":["Asymmetric processional mantle cascading from left shoulder into the rear drape","Open split cuirass framing Lucario's biological chest spike"],"macroFormPlan":"Build one left-weighted mantle from broad overlapping rotated plates, continue it into a rear cloth cascade, keep a deliberate open chest V, and use small subordinate limb shells instead of a surrounding frame.","paintPlan":"Use directional face swatches with deep shadow, ink, violet/cobalt facing planes, restrained gold edges and aura accents so accessory materials show value hierarchy without altering biological UVs.","gameplayReadGoal":"At 160 px the eye should read Lucario first, then one sweeping asymmetric ceremonial mantle and open luminous chest V; limb guards must support rather than fragment the silhouette.","antiPatternsToReject":["Pokemon base plus scattered accessories","rectangular cage or portal-frame silhouette","repeated bars or oversized orthogonal slabs","flat single-color equipment with no value hierarchy"],"thirdPartyReusePolicy":"TECHNIQUES_ONLY_UNLESS_LICENSED_DERIVATIVE_DONOR"},
      "variantCoverage":{"audited":True,"variants":[{"name":"default","coverage":"Exact official Lucario default model and normal texture; no male/female geometry split exists on this resolver path."},{"name":"shiny","coverage":"Same exact official geometry with independently extracted official shiny texture; cosmetic overlay is shared presentation-only material."}]},
      "technicalChecks":["REFERENCE_DOSSIER","OFFICIAL_SOURCE_HASHES","ORIGINAL_BONE_EQUALITY","COSMETIC_ATTACHMENT","BUILDER_REPRODUCIBILITY","BLOCKBENCH_MATCHED_CAMERA","GAMEPLAY_SCALE_EVIDENCE","PLAYABLE_TEST_BUILD","INTEGRATION_CORE_CI"]
    }
    mp=ROOT/MANIFEST_OUT; mp.parent.mkdir(parents=True,exist_ok=True); mp.write_text(json.dumps(manifest,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    registry=json.loads((ROOT/REGISTRY).read_text(encoding="utf-8"))
    entry=next(e for e in registry["entries"] if e["slug"]=="0448_lucario")
    entry.update({"lifecycle":"PROFESSIONAL_CANDIDATE","artStatus":"ARTISTIC FAIL","saleEligible":False,"manifest":MANIFEST_OUT,"blocker":"Professional candidate generated from the exact current official baseline. Technical and Blockbench review required; owner has not approved the current art."})
    (ROOT/REGISTRY).write_text(json.dumps(registry,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    doc=ROOT/SPECIES_DOC
    old=doc.read_text(encoding="utf-8")
    lines=old.splitlines()
    lines[2]="Status: ARTISTIC FAIL"
    lines[3]="Sale eligibility: NOT ELIGIBLE."
    notice="\nProfessional candidate note: the current production bytes are regenerated by `build_aura_sentinel_professional.py` under the Skin Studio manifest. Historical V4/V6/V7 geometry descriptions below are provenance history only and do not describe the current candidate.\n"
    doc.write_text("\n".join(lines[:5])+notice+"\n".join(lines[5:])+"\n",encoding="utf-8")
    print(json.dumps({"status":"BOOTSTRAPPED","manifest":MANIFEST_OUT,"info":info},indent=2))


def main() -> None:
    parser=argparse.ArgumentParser(); parser.add_argument("--bootstrap",action="store_true"); args=parser.parse_args()
    info=build()
    if args.bootstrap: bootstrap(info)
    else: print(json.dumps({"status":"BUILT","info":info},indent=2))

if __name__ == "__main__": main()
