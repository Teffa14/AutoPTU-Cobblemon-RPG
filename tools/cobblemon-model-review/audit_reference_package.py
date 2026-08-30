#!/usr/bin/env python3
"""Inspect an external skin package and emit metadata-only structural evidence."""
from __future__ import annotations
import argparse, hashlib, io, json, math, re, zipfile
from collections import Counter
from pathlib import Path
from PIL import Image

MODEL_SUFFIXES=(".geo.json",".bbmodel",".model.json")
TEXTURE_SUFFIXES=(".png",)
ANIMATION_SUFFIXES=(".animation.json",)
AUX_TOKENS=("resolver","poser","species","feature")

def sha256(data:bytes)->str: return hashlib.sha256(data).hexdigest()
def classify(path:str)->str:
    low=path.lower()
    if low.endswith(MODEL_SUFFIXES): return "MODEL"
    if low.endswith(TEXTURE_SUFFIXES): return "TEXTURE"
    if low.endswith(ANIMATION_SUFFIXES): return "ANIMATION"
    if low.endswith(".json") and any(t in low for t in AUX_TOKENS): return "AUX_JSON"
    if any(t in low for t in ("license","copying","notice")): return "LICENSE"
    return "OTHER"

def load_json(data:bytes):
    try: return json.loads(data.decode("utf-8"))
    except Exception: return None

def json_identifiers(parsed)->dict:
    if parsed is None: return {}
    text=json.dumps(parsed,separators=(",",":"))
    return {"identifiers":sorted(set(re.findall(r"(?:geometry|animation)\.[A-Za-z0-9_.:-]+",text)))[:50]}

def geometry_metrics(parsed)->dict:
    if not isinstance(parsed,dict): return {}
    gs=parsed.get("minecraft:geometry")
    if not isinstance(gs,list) or not gs: return {}
    bones=gs[0].get("bones",[])
    cubes=[]; rotated=0; min_xyz=[math.inf]*3; max_xyz=[-math.inf]*3; hist=Counter(); parented=0; locators=0
    for b in bones:
        if b.get("parent"): parented+=1
        if isinstance(b.get("locators"),dict): locators+=len(b["locators"])
        for cube in b.get("cubes",[]):
            cubes.append(cube)
            rot=cube.get("rotation",[0,0,0])
            if any(abs(float(v))>1e-6 for v in rot[:3]): rotated+=1
            o=cube.get("origin",[0,0,0]); s=cube.get("size",[0,0,0])
            try:
                for i in range(3): min_xyz[i]=min(min_xyz[i],float(o[i])); max_xyz[i]=max(max_xyz[i],float(o[i])+float(s[i]))
                hist[tuple(round(float(v),1) for v in sorted(s,reverse=True))]+=1
            except Exception: pass
    bounds=None
    if cubes and all(math.isfinite(v) for v in min_xyz+max_xyz):
        bounds={"min":[round(v,3) for v in min_xyz],"max":[round(v,3) for v in max_xyz],"span":[round(max_xyz[i]-min_xyz[i],3) for i in range(3)]}
    return {"boneCount":len(bones),"parentedBoneCount":parented,"locatorCount":locators,"cubeCount":len(cubes),"rotatedCubeCount":rotated,"rotatedCubeRatio":round(rotated/len(cubes),4) if cubes else 0.0,"bounds":bounds,"topCubeSizeFamilies":[{"size":list(k),"count":v} for k,v in hist.most_common(8)]}

def texture_metrics(data:bytes)->dict:
    try: image=Image.open(io.BytesIO(data)).convert("RGBA")
    except Exception: return {}
    px=list(image.getdata()); rgb=[p[:3] for p in px if p[3]>0]
    vals=[max(c) for c in rgb] if rgb else [0]
    return {"width":image.width,"height":image.height,"opaquePixelCount":sum(p[3]==255 for p in px),"transparentPixelCount":sum(p[3]==0 for p in px),"partialAlphaPixelCount":sum(0<p[3]<255 for p in px),"uniqueVisibleRgbCount":len(set(rgb)),"visibleValueRange":[min(vals),max(vals)],"meanVisibleValue":round(sum(vals)/len(vals),2) if vals else 0.0}

def main():
    ap=argparse.ArgumentParser(); ap.add_argument("package",type=Path); ap.add_argument("--species",required=True); ap.add_argument("--project",required=True); ap.add_argument("--version",required=True); ap.add_argument("--source-url",required=True); ap.add_argument("--output",type=Path,required=True); a=ap.parse_args()
    raw=a.package.read_bytes(); species=a.species.casefold(); records=[]
    with zipfile.ZipFile(a.package) as zf:
        for info in zf.infolist():
            if info.is_dir(): continue
            path=info.filename; kind=classify(path)
            if species not in path.casefold() and kind!="LICENSE": continue
            data=zf.read(info); rec={"path":path,"kind":kind,"size":len(data),"sha256":sha256(data)}
            if kind in {"MODEL","ANIMATION","AUX_JSON"}:
                parsed=load_json(data); rec.update(json_identifiers(parsed))
                if kind=="MODEL": rec["geometryMetrics"]=geometry_metrics(parsed)
            elif kind=="TEXTURE": rec["textureMetrics"]=texture_metrics(data)
            records.append(rec)
    report={"format":"ouros.external-reference-package-audit.v2","project":a.project,"sourceVersion":a.version,"sourceUrl":a.source_url,"species":a.species.lower(),"packageFilename":a.package.name,"packageSha256":sha256(raw),"matchingFiles":sorted(records,key=lambda r:(r["kind"],r["path"])),"modelCount":sum(r["kind"]=="MODEL" for r in records),"textureCount":sum(r["kind"]=="TEXTURE" for r in records),"animationCount":sum(r["kind"]=="ANIMATION" for r in records)}
    a.output.parent.mkdir(parents=True,exist_ok=True); a.output.write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8"); print(json.dumps(report,indent=2))
if __name__=="__main__": main()
