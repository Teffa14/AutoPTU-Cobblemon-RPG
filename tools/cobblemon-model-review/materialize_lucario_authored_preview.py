#!/usr/bin/env python3
from __future__ import annotations
import argparse, copy, hashlib, json
from pathlib import Path
from PIL import Image

OFFICIAL_MODEL_SHA="ccc5f4521fd71fcb4db548a0f0fd0ed41f83426f4a5c04efa473d8a20bef2de9"
OFFICIAL_TEXTURE_SHA="98c46f44f9e3428c8ecfd9f564d8d2e4c26ea60bee9ace6ff225c66f4803596a"
OFFICIAL_BONES=87
PALETTE=[(230,238,250,255),(91,130,207,255),(186,204,235,255),(247,248,250,255),(59,86,143,255),(211,221,240,255),(119,151,211,255),(239,243,250,255)]

def sha256(p:Path)->str:
    return hashlib.sha256(p.read_bytes()).hexdigest()

def solid_uv(u:int,v:int)->dict:
    face={"uv":[u,v],"uv_size":[1,1]}
    return {k:dict(face) for k in ("north","east","south","west","up","down")}

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--patch",type=Path,required=True)
    ap.add_argument("--official-model",type=Path,required=True)
    ap.add_argument("--official-texture",type=Path,required=True)
    ap.add_argument("--output-dir",type=Path,required=True)
    a=ap.parse_args(); a.output_dir.mkdir(parents=True,exist_ok=True)
    if sha256(a.official_model)!=OFFICIAL_MODEL_SHA: raise SystemExit("official model hash mismatch")
    if sha256(a.official_texture)!=OFFICIAL_TEXTURE_SHA: raise SystemExit("official texture hash mismatch")
    patch=json.loads(a.patch.read_text(encoding="utf-8"))
    if patch.get("species")!="lucario" or patch.get("reviewState")!="BLOCKBENCH_REVIEW_PENDING": raise SystemExit("invalid authored patch")
    stem=str(patch.get("previewStem") or "lucario_preview")
    if not stem.startswith("lucario_v") or any(ch not in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-" for ch in stem): raise SystemExit("invalid previewStem")
    src=json.loads(a.official_model.read_text(encoding="utf-8")); geos=src.get("minecraft:geometry")
    if not isinstance(geos,list) or len(geos)!=1: raise SystemExit("expected one geometry")
    geo=geos[0]; bones=geo.get("bones")
    if not isinstance(bones,list) or len(bones)!=OFFICIAL_BONES: raise SystemExit(f"expected {OFFICIAL_BONES} official bones")
    official_names={b.get("name") for b in bones}
    candidate=copy.deepcopy(src); cgeo=candidate["minecraft:geometry"][0]
    cgeo["description"]["identifier"]=f"geometry.ouros_{stem}"
    added=[]
    for bi,b in enumerate(patch["bones"]):
        if b["parent"] not in official_names and b["parent"] not in {x["name"] for x in patch["bones"]}: raise SystemExit(f"unknown parent {b['parent']}")
        nb={"name":b["name"],"parent":b["parent"],"pivot":b["pivot"],"cubes":[]}
        for ci,c in enumerate(b["cubes"]):
            slot=(bi+ci)%len(PALETTE); u=80+slot; v=63
            nc={"origin":c["origin"],"size":c["size"],"uv":solid_uv(u,v)}
            if "pivot" in c: nc["pivot"]=c["pivot"]
            if "rotation" in c: nc["rotation"]=c["rotation"]
            nb["cubes"].append(nc)
        cgeo["bones"].append(nb); added.append(nb["name"])
    out_model=a.output_dir/f"{stem}.geo.json"
    out_model.write_text(json.dumps(candidate,separators=(",",":"))+"\n",encoding="utf-8")
    img=Image.open(a.official_texture).convert("RGBA")
    if img.size!=(128,64): raise SystemExit(f"unexpected texture size {img.size}")
    px=img.load()
    for i,rgba in enumerate(PALETTE):
        x,y=80+i,63
        if px[x,y][3]!=0: raise SystemExit(f"palette slot {x},{y} not transparent in official texture")
        px[x,y]=rgba
    out_tex=a.output_dir/f"{stem}.png"; img.save(out_tex)
    report={"format":"ouros.blockbench-preview-materialization.v1","species":"lucario","concept":patch["concept"],"artStatus":"ARTISTIC FAIL","reviewState":"BLOCKBENCH_REVIEW_PENDING","previewStem":stem,"officialModelSha256":sha256(a.official_model),"officialTextureSha256":sha256(a.official_texture),"candidateModelSha256":sha256(out_model),"candidateTextureSha256":sha256(out_tex),"officialBoneCount":OFFICIAL_BONES,"derivedBoneCount":len(cgeo["bones"]),"cosmeticBoneCount":len(added),"cosmeticGroups":added,"textureMode":"DERIVED_FROM_OFFICIAL_PREVIEW_ONLY","repaintScope":"Eight previously transparent 1px atlas palette slots at x=80..87,y=63; biological texels unchanged.","artApproval":"NOT_GRANTED"}
    (a.output_dir/"materialization-report.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(report,indent=2))
if __name__=="__main__": main()
