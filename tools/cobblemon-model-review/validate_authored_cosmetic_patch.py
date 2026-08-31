#!/usr/bin/env python3
from __future__ import annotations
import argparse,json
from collections import Counter
from pathlib import Path
FORMAT="ouros.blockbench-authored-cosmetic-patch.v1"
ALLOWED={"ARTISTIC FAIL","USER REJECTED — REWORK REQUIRED","OWNER REVIEW REQUIRED"}
def fail(m): raise SystemExit(m)
def v3(v,w):
    if not isinstance(v,list) or len(v)!=3 or not all(isinstance(x,(int,float)) for x in v): fail(f"{w} must be numeric vec3")
    return [float(x) for x in v]
def main():
    p=argparse.ArgumentParser(); p.add_argument("patch",type=Path); p.add_argument("--expected-species"); a=p.parse_args(); d=json.loads(a.patch.read_text(encoding="utf-8"))
    if d.get("format")!=FORMAT: fail("unsupported patch format")
    s=str(d.get("species","")).lower()
    if not s: fail("species required")
    if a.expected_species and s!=a.expected_species.lower(): fail("species mismatch")
    if d.get("artStatus") not in ALLOWED: fail("artStatus may not claim approval")
    if d.get("reviewState")!="BLOCKBENCH_REVIEW_PENDING": fail("reviewState must remain BLOCKBENCH_REVIEW_PENDING")
    b=d.get("officialBaseline",{}); sha=b.get("modelSha256")
    if not isinstance(sha,str) or len(sha)!=64: fail("baseline modelSha256 required")
    if s=="lucario" and b.get("officialBoneCount")!=87: fail("Lucario requires exact 87-bone baseline")
    r=d.get("designRules",{})
    if r.get("inheritRejectedCosmeticGeometry") is not False or r.get("dominantRectangularApronFaceAllowed") is not False or r.get("stackedCapSlabsAllowed") is not False: fail("rejected geometry rules violated")
    bones=d.get("bones"); names=set(); sizes=Counter(); total=thin=roots=0
    if not isinstance(bones,list) or not bones: fail("bones required")
    for bi,bone in enumerate(bones):
        name=bone.get("name"); parent=bone.get("parent")
        if not isinstance(name,str) or not name.startswith("ouros_"): fail("all cosmetic bones must use ouros_ namespace")
        if name in names: fail("duplicate cosmetic bone")
        names.add(name)
        if not isinstance(parent,str) or not parent: fail("parent required")
        if not parent.startswith("ouros_"): roots+=1
        v3(bone.get("pivot"),f"bones[{bi}].pivot"); cubes=bone.get("cubes")
        if not isinstance(cubes,list) or not cubes: fail("each cosmetic bone needs cubes")
        for ci,c in enumerate(cubes):
            v3(c.get("origin"),f"cube {ci} origin"); size=v3(c.get("size"),f"cube {ci} size")
            if any(x<=0 for x in size): fail("cube size must be positive")
            if max(size)>14: fail("giant macro slab rejected")
            if min(size)<=0.6: thin+=1
            sizes[tuple(round(x,2) for x in size)]+=1; total+=1
    if [(z,n) for z,n in sizes.items() if n>=3 and max(z)>=4 and min(z)>=1]: fail("repeated large cuboid pattern rejected")
    if roots<3: fail("insufficient direct official roots")
    if total<12 or thin/total<0.45: fail("insufficient cloth-like macro-form distribution")
    m=d.get("sliceMetrics",{})
    if m.get("cosmeticBoneCount")!=len(bones) or m.get("cosmeticCubeCount")!=total: fail("slice metrics mismatch")
    print(json.dumps({"status":"PASS","artApproval":"NOT_GRANTED","species":s,"cosmeticBoneCount":len(bones),"cosmeticCubeCount":total,"thinClothCubeCount":thin,"officialRootCount":roots,"blockbenchReview":"REQUIRED_BEFORE_PRODUCTION"},indent=2))
if __name__=="__main__": main()
