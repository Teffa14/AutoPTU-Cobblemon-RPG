#!/usr/bin/env python3
"""Build Aura Sentinel v2 as a full-surface Lucario transformation.

The exact official Cobblemon Lucario geometry is authoritative. Existing bones,
cubes, pivots, locators, hierarchy, names and UVs remain JSON-equivalent and in
order. Only the geometry identifier and appended `ouros_*` presentation bones
change. Normal and shiny textures are derived from their exact official source
textures without changing dimensions or alpha semantics.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from PIL import Image

FACES=("north","east","south","west","up","down")
PALETTE={
    "void":(22,26,40,255),
    "indigo":(37,55,96,255),
    "cobalt":(46,91,145,255),
    "silver":(174,193,207,255),
    "gold":(223,179,68,255),
    "ivory":(224,221,201,255),
    "aura":(70,218,244,210),
    "amethyst":(104,67,146,255),
}
PIXELS:dict[str,tuple[int,int]]={}


def sha256(path:Path)->str:
    h=hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda:f.read(1024*1024),b""):
            h.update(chunk)
    return h.hexdigest()


def mark_uv_usage(geometry:dict)->set[tuple[int,int]]:
    width=geometry["description"]["texture_width"]
    height=geometry["description"]["texture_height"]
    used:set[tuple[int,int]]=set()
    def mark(x,y,w,h):
        x0,x1=sorted((int(x),int(x+w))); y0,y1=sorted((int(y),int(y+h)))
        for yy in range(max(0,y0),min(height,y1)):
            for xx in range(max(0,x0),min(width,x1)):
                used.add((xx,yy))
    for bone in geometry["bones"]:
        for item in bone.get("cubes",[]):
            dx,dy,dz=item.get("size",[0,0,0]); uv=item.get("uv",[0,0])
            if isinstance(uv,list):
                u,v=uv
                for rect in ((u+dz,v,dx,dz),(u+dz+dx,v,dx,dz),(u,v+dz,dz,dy),(u+dz,v+dz,dx,dy),(u+dz+dx,v+dz,dz,dy),(u+2*dz+dx,v+dz,dx,dy)):
                    mark(*rect)
            elif isinstance(uv,dict):
                for face in uv.values():
                    if isinstance(face,dict):
                        p=face.get("uv",[0,0]); s=face.get("uv_size",[1,1]); mark(p[0],p[1],s[0],s[1])
    return used


def choose_palette_pixels(geometry:dict)->dict[str,tuple[int,int]]:
    width=geometry["description"]["texture_width"]; height=geometry["description"]["texture_height"]
    used=mark_uv_usage(geometry)
    free=[(x,y) for y in range(height-1,-1,-1) for x in range(width) if (x,y) not in used]
    if len(free)<len(PALETTE): raise RuntimeError("official Lucario texture has insufficient UV-free texels")
    return {name:free[i] for i,name in enumerate(PALETTE)}


def solid_uv(material:str)->dict:
    x,y=PIXELS[material]
    return {face:{"uv":[x,y],"uv_size":[1,1]} for face in FACES}


def cube(origin,size,material,**extra)->dict:
    out={"origin":origin,"size":size,"uv":solid_uv(material)}
    out.update({k:v for k,v in extra.items() if v is not None})
    return out


def helm_system()->dict:
    return {"name":"ouros_aura_helm_system","parent":"head_angle","pivot":[0,38.5,-2.0],"cubes":[
        cube([-5.0,35.8,2.3],[10.0,4.8,1.0],"void"),
        cube([-5.4,35.5,-1.7],[1.0,4.9,4.2],"indigo"),cube([4.4,35.5,-1.7],[1.0,4.9,4.2],"indigo"),
        cube([-4.3,37.2,-4.9],[8.6,1.1,0.28],"silver"),
        cube([-4.0,38.25,-5.0],[8.0,0.55,0.34],"gold"),
        cube([-0.48,36.35,-5.08],[0.96,2.0,0.22],"aura"),
        cube([-5.45,34.9,-3.7],[1.15,2.8,2.7],"amethyst",pivot=[-4.88,36.3,-2.35],rotation=[0,0,-9]),
        cube([4.30,34.9,-3.7],[1.15,2.8,2.7],"cobalt",pivot=[4.88,36.3,-2.35],rotation=[0,0,9]),
        cube([-6.7,39.1,-2.4],[3.0,0.55,1.4],"cobalt",pivot=[-5.2,39.38,-1.7],rotation=[0,0,-28]),
        cube([3.7,39.25,-2.2],[2.55,0.48,1.25],"amethyst",pivot=[4.98,39.49,-1.58],rotation=[0,0,23]),
        cube([-6.25,39.7,-2.25],[2.2,0.24,1.0],"aura",pivot=[-5.15,39.82,-1.75],rotation=[0,0,-28]),
        cube([4.0,39.78,-2.08],[1.9,0.22,0.9],"gold",pivot=[4.95,39.89,-1.63],rotation=[0,0,23]),
    ]}


def mantle_shell()->dict:
    return {"name":"ouros_aura_mantle_shell","parent":"torso3","pivot":[0,29.5,0.0],"cubes":[
        cube([-8.0,29.2,-3.0],[16.0,1.6,6.2],"void"),
        cube([-9.2,28.4,-2.5],[4.2,2.6,5.1],"indigo",pivot=[-7.1,29.7,0.05],rotation=[0,0,-11]),
        cube([5.1,28.7,-2.3],[3.5,2.2,4.8],"cobalt",pivot=[6.85,29.8,0.1],rotation=[0,0,8]),
        cube([-9.45,30.75,-2.2],[4.5,0.35,4.5],"silver",pivot=[-7.2,30.92,0.05],rotation=[0,0,-11]),
        cube([5.0,30.72,-2.0],[3.75,0.32,4.25],"gold",pivot=[6.88,30.88,0.12],rotation=[0,0,8]),
        cube([-6.0,30.2,2.55],[12.0,2.2,1.1],"amethyst"),
        cube([-4.8,32.1,2.7],[9.6,0.42,0.8],"aura"),
        cube([-8.8,29.9,-0.7],[2.5,0.5,1.6],"gold",pivot=[-7.55,30.15,0.1],rotation=[0,0,-35]),
        cube([6.1,30.0,-0.55],[2.25,0.45,1.5],"silver",pivot=[7.22,30.23,0.2],rotation=[0,0,31]),
    ]}


def breastplate()->dict:
    return {"name":"ouros_aura_breastplate","parent":"torso3","pivot":[0,27.8,-3.0],"cubes":[
        cube([-4.2,25.3,-4.0],[8.4,5.0,0.48],"indigo"),
        cube([-4.55,25.1,-3.45],[0.75,5.2,6.0],"void"),cube([3.8,25.1,-3.45],[0.75,5.2,6.0],"void"),
        cube([-3.7,29.7,-4.22],[7.4,0.6,0.34],"silver"),
        cube([-3.7,24.95,-4.20],[7.4,0.55,0.34],"gold"),
        cube([-3.1,26.3,-4.28],[1.0,2.5,0.26],"aura"),cube([2.1,26.3,-4.28],[1.0,2.5,0.26],"aura"),
        cube([-1.75,26.55,-4.42],[3.5,2.9,0.30],"void",pivot=[0,28.0,-4.27],rotation=[0,0,45]),
        cube([-1.15,27.15,-4.66],[2.3,1.7,0.22],"aura",pivot=[0,28.0,-4.55],rotation=[0,0,45]),
    ]}


def shrine_frame()->dict:
    return {"name":"ouros_aura_shrine_frame","parent":"torso3","pivot":[0,31.0,3.0],"cubes":[
        cube([-6.5,24.0,2.8],[0.9,11.0,0.9],"void",pivot=[-6.05,29.5,3.25],rotation=[-4,0,-7]),
        cube([5.6,24.0,2.8],[0.9,11.0,0.9],"void",pivot=[6.05,29.5,3.25],rotation=[-4,0,7]),
        cube([-6.1,34.4,2.65],[12.2,0.85,1.2],"indigo"),
        cube([-5.55,35.25,2.75],[11.1,0.32,1.0],"gold"),
        cube([-7.9,33.4,2.9],[3.1,0.55,1.4],"cobalt",pivot=[-6.35,33.68,3.6],rotation=[0,0,-32]),
        cube([4.8,33.55,2.9],[2.7,0.50,1.35],"amethyst",pivot=[6.15,33.8,3.58],rotation=[0,0,28]),
        cube([-7.45,34.0,3.0],[2.25,0.25,1.0],"aura",pivot=[-6.32,34.13,3.5],rotation=[0,0,-32]),
        cube([5.05,34.08,3.0],[2.0,0.24,0.95],"aura",pivot=[6.05,34.2,3.48],rotation=[0,0,28]),
        cube([-1.1,35.35,3.05],[2.2,2.2,0.32],"void",pivot=[0,36.45,3.21],rotation=[0,0,45]),
        cube([-0.68,35.77,3.32],[1.36,1.36,0.20],"aura",pivot=[0,36.45,3.42],rotation=[0,0,45]),
        cube([-6.2,23.6,3.0],[12.4,0.62,0.82],"silver"),
    ]}


def armguard(name,parent,left:bool)->dict:
    s=1 if left else -1
    x=9.0 if left else -12.2
    rot=-8 if left else 8
    return {"name":name,"parent":parent,"pivot":[10.4*s,29.4,-0.3],"cubes":[
        cube([x,27.6,-2.5],[3.2,4.0,4.2],"void"),
        cube([x+(0.25 if left else 0.25),27.9,-2.72],[2.7,0.42,4.6],"silver"),
        cube([x+(2.65 if left else 0.0),28.3,-1.15],[0.55,2.5,1.65],"gold"),
        cube([x+(2.85 if left else 0.15),28.8,-0.82],[0.22,1.45,0.95],"aura"),
        cube([x+(1.0 if left else 0.65),31.1,-1.8],[1.55,1.75,2.8],"cobalt" if left else "amethyst",pivot=[10.5*s,31.55,-0.4],rotation=[0,0,rot]),
    ]}


def waistcoat()->dict:
    return {"name":"ouros_aura_waistcoat","parent":"torso","pivot":[0,20.6,1.0],"cubes":[
        cube([-5.6,20.2,-3.8],[11.2,1.0,7.4],"void"),
        cube([-5.8,21.1,-3.9],[11.6,0.32,7.6],"gold"),
        cube([-5.1,13.2,3.5],[4.5,7.7,0.72],"indigo",pivot=[-2.85,20.15,3.86],rotation=[-7,0,8]),
        cube([0.55,14.2,3.55],[4.25,6.7,0.72],"amethyst",pivot=[2.68,20.2,3.91],rotation=[-7,0,-7]),
        cube([-4.75,13.15,4.18],[3.9,0.42,0.24],"silver",pivot=[-2.8,13.36,4.30],rotation=[-7,0,8]),
        cube([0.82,14.18,4.23],[3.7,0.38,0.24],"gold",pivot=[2.67,14.37,4.35],rotation=[-7,0,-7]),
        cube([-5.45,16.4,-3.95],[2.0,4.4,0.36],"cobalt",pivot=[-4.45,18.6,-3.77],rotation=[0,0,-5]),
        cube([3.45,17.1,-3.95],[2.0,3.7,0.36],"indigo",pivot=[4.45,18.95,-3.77],rotation=[0,0,5]),
    ]}


def relic_fin()->dict:
    return {"name":"ouros_aura_relic_fin","parent":"torso3","pivot":[-6.0,29.0,3.8],"cubes":[
        cube([-10.2,27.0,3.55],[4.8,0.7,1.1],"amethyst",pivot=[-7.8,27.35,4.1],rotation=[0,0,-42]),
        cube([-10.0,28.05,3.65],[4.2,0.5,0.95],"cobalt",pivot=[-7.9,28.30,4.12],rotation=[0,0,-42]),
        cube([-9.5,29.0,3.72],[3.2,0.28,0.8],"aura",pivot=[-7.9,29.14,4.12],rotation=[0,0,-42]),
        cube([-8.3,25.5,3.62],[0.55,4.8,0.95],"gold",pivot=[-8.03,27.9,4.10],rotation=[0,0,-8]),
    ]}


def build_model(source:Path)->tuple[dict,int]:
    data=json.loads(source.read_text(encoding="utf-8")); geo=data["minecraft:geometry"][0]
    original=len(geo["bones"]); geo["description"]["identifier"]="geometry.ouros_aura_sentinel_lucario"
    geo["bones"].extend([
        helm_system(),mantle_shell(),breastplate(),shrine_frame(),
        armguard("ouros_aura_left_armguard","arm_left2",True),
        armguard("ouros_aura_right_armguard","arm_right2",False),
        waistcoat(),relic_fin(),
    ])
    return data,original


def remap_texture(source:Path,target:Path,shiny:bool)->dict:
    image=Image.open(source).convert("RGBA"); out=Image.new("RGBA",image.size)
    changed=0; occupied=0
    for y in range(image.height):
        for x in range(image.width):
            r,g,b,a=image.getpixel((x,y))
            if a==0:
                out.putpixel((x,y),(r,g,b,a)); continue
            occupied+=1; lum=(r*299+g*587+b*114)//1000
            # Preserve highly red eye/mouth accents so Lucario identity remains readable.
            if r>110 and r>g*1.35 and r>b*1.25:
                nr,ng,nb=(214,71,79) if not shiny else (230,99,127)
            elif lum<58:
                nr,ng,nb=(21,25,39) if not shiny else (28,30,40)
            elif b>r*1.12 and b>g*1.02:
                if shiny:
                    nr,ng,nb=(74+lum//5,91+lum//6,126+lum//5)
                else:
                    nr,ng,nb=(28+lum//7,57+lum//5,102+lum//4)
            elif r>130 and g>110 and abs(r-g)<80:
                if shiny:
                    nr,ng,nb=(190+lum//8,194+lum//9,205+lum//10)
                else:
                    nr,ng,nb=(184+lum//7,176+lum//8,146+lum//10)
            else:
                if shiny:
                    nr,ng,nb=(62+lum//6,67+lum//7,88+lum//6)
                else:
                    nr,ng,nb=(34+lum//8,48+lum//7,74+lum//6)
            nr=min(255,nr); ng=min(255,ng); nb=min(255,nb)
            new=(nr,ng,nb,a); out.putpixel((x,y),new)
            if new!=(r,g,b,a): changed+=1
    target.parent.mkdir(parents=True,exist_ok=True); out.save(target,optimize=True)
    return {"changedPixels":changed,"occupiedPixels":occupied,"dimensions":[image.width,image.height]}


def texture_metadata(official:Path,derived:Path,shiny:bool,stats:dict)->dict:
    return {
        "officialTextureBaselineSha256":sha256(official),
        "derivedTexture":derived.name,
        "derivedTextureSha256":sha256(derived),
        "bodyTexelRework":f"Full-surface {'shiny ' if shiny else ''}Lucario material pass changes {stats['changedPixels']} occupied pixels while preserving the exact 128x64 canvas, UV coordinates and transparency semantics.",
        "paletteIntent":"Normal uses midnight indigo/cobalt, obsidian, ivory, gold and aura-cyan. Shiny deliberately shifts to graphite, cool silver, amethyst and brighter aura accents rather than silently reusing normal pixels.",
        "materialIntent":"Biological color regions are rematerialized as a coherent ceremonial aura-knight suit language; large added geometry uses armor, cloth, metal and aura-energy roles while the original anatomy stays intact underneath.",
        "allowAlphaSemanticsChange":False,
    }


def main()->None:
    p=argparse.ArgumentParser()
    p.add_argument("--official",type=Path,required=True)
    p.add_argument("--official-normal",type=Path,required=True)
    p.add_argument("--official-shiny",type=Path,required=True)
    p.add_argument("--model-out",type=Path,required=True)
    p.add_argument("--normal-out",type=Path,required=True)
    p.add_argument("--shiny-out",type=Path,required=True)
    p.add_argument("--overlay-out",type=Path,required=True)
    p.add_argument("--normal-metadata-out",type=Path,required=True)
    p.add_argument("--shiny-metadata-out",type=Path,required=True)
    p.add_argument("--build-metadata-out",type=Path,required=True)
    args=p.parse_args()

    source=json.loads(args.official.read_text(encoding="utf-8"))["minecraft:geometry"][0]
    global PIXELS; PIXELS=choose_palette_pixels(source)
    built,original=build_model(args.official)
    args.model_out.parent.mkdir(parents=True,exist_ok=True)
    args.model_out.write_text(json.dumps(built,separators=(",",":"))+"\n",encoding="utf-8")

    overlay=Image.new("RGBA",(source["description"]["texture_width"],source["description"]["texture_height"]),(0,0,0,0))
    for name,color in PALETTE.items(): overlay.putpixel(PIXELS[name],color)
    args.overlay_out.parent.mkdir(parents=True,exist_ok=True); overlay.save(args.overlay_out,optimize=True)

    nstats=remap_texture(args.official_normal,args.normal_out,False)
    sstats=remap_texture(args.official_shiny,args.shiny_out,True)
    nmeta=texture_metadata(args.official_normal,args.normal_out,False,nstats)
    smeta=texture_metadata(args.official_shiny,args.shiny_out,True,sstats)
    args.normal_metadata_out.parent.mkdir(parents=True,exist_ok=True)
    args.normal_metadata_out.write_text(json.dumps(nmeta,indent=2)+"\n",encoding="utf-8")
    args.shiny_metadata_out.write_text(json.dumps(smeta,indent=2)+"\n",encoding="utf-8")

    extras=built["minecraft:geometry"][0]["bones"][original:]
    bmeta={
        "format":"ouros.aura-sentinel-v2-build.v1",
        "originalBoneCount":original,"derivedBoneCount":len(built["minecraft:geometry"][0]["bones"]),
        "cosmeticBones":[b["name"] for b in extras],
        "cosmeticCubeCount":sum(len(b.get("cubes",[])) for b in extras),
        "palettePixels":{k:list(v) for k,v in PIXELS.items()},
        "normalTextureSha256":sha256(args.normal_out),"shinyTextureSha256":sha256(args.shiny_out),
        "officialNormalSha256":sha256(args.official_normal),"officialShinySha256":sha256(args.official_shiny),
    }
    args.build_metadata_out.parent.mkdir(parents=True,exist_ok=True)
    args.build_metadata_out.write_text(json.dumps(bmeta,indent=2)+"\n",encoding="utf-8")

if __name__=="__main__": main()
