#!/usr/bin/env python3
"""Aura Sentinel V7.3 — asymmetric shrine-mantle rebuild.

Presentation only. The exact official Cobblemon Lucario geometry remains intact.
V7.2 still read as base Lucario plus narrow ribbons. V7.3 replaces those bars
with broader overlapping plates that form one continuous asymmetric mantle,
diagonal cuirass/sash, dorsal shrine crest and trailing cloth system. The
construction applies only generic contour/overlap/asymmetry lessons recorded in
the validated same-species dossier. No third-party geometry, UV, texture,
palette layout, motif, costume or silhouette is copied.
"""
from __future__ import annotations
import importlib.util, json
from pathlib import Path

PIPELINE = Path(__file__).with_name("build_aura_sentinel_v3_current.py")
spec = importlib.util.spec_from_file_location("aura_pipeline", PIPELINE)
pipeline = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(pipeline)
base = pipeline.base
c = base.cube


def crown():
    return {"name":"ouros_aura_crown","parent":"head_angle","pivot":[0,37.4,-3.1],"cubes":[
        c([-4.6,36.9,-4.05],[4.0,1.05,0.55],"void",pivot=[-2.6,37.4,-3.78],rotation=[0,0,-13]),
        c([0.6,36.9,-4.05],[4.0,1.05,0.55],"indigo",pivot=[2.6,37.4,-3.78],rotation=[0,0,13]),
        c([-1.25,37.45,-4.20],[2.5,0.72,0.55],"gold",pivot=[0,37.80,-3.92],rotation=[-7,0,0]),
        c([-4.55,38.0,-3.80],[1.15,3.0,0.62],"cobalt",pivot=[-4.0,38.2,-3.5],rotation=[-9,-6,-24]),
        c([-4.85,40.45,-3.50],[0.95,2.15,0.52],"aura",pivot=[-4.35,40.65,-3.24],rotation=[-12,-8,-35]),
    ]}


def mantle():
    return {"name":"ouros_aura_mantle","parent":"torso3","pivot":[-4.5,28.5,0.4],"cubes":[
        c([-8.7,28.3,-3.2],[8.8,2.45,5.7],"void",pivot=[-4.3,29.5,-0.35],rotation=[4,-9,-18]),
        c([-10.5,26.2,-2.7],[8.5,2.15,5.2],"indigo",pivot=[-6.2,27.2,-0.1],rotation=[7,-12,-29]),
        c([-11.7,23.8,-2.0],[7.6,1.85,4.6],"cobalt",pivot=[-7.9,24.7,0.25],rotation=[10,-14,-40]),
        c([-11.6,21.4,-1.15],[6.5,1.55,3.8],"amethyst",pivot=[-8.4,22.1,0.7],rotation=[12,-16,-50]),
        c([-10.5,19.4,-0.3],[5.2,1.25,3.1],"indigo",pivot=[-7.9,20.0,1.25],rotation=[14,-18,-58]),
        c([-7.9,29.3,-3.48],[6.2,0.48,0.36],"aura",pivot=[-4.8,29.5,-3.30],rotation=[4,-9,-18]),
        c([-9.5,27.0,-3.00],[5.5,0.42,0.34],"gold",pivot=[-6.75,27.2,-2.83],rotation=[7,-12,-29]),
    ]}


def chest_sash():
    return {"name":"ouros_aura_chest_sash","parent":"torso3","pivot":[0,27.0,-3.5],"cubes":[
        c([-5.1,29.0,-4.35],[6.4,1.55,0.68],"indigo",pivot=[-1.9,29.75,-4.02],rotation=[0,0,-27]),
        c([-4.0,26.5,-4.38],[6.0,1.45,0.66],"cobalt",pivot=[-1.0,27.2,-4.05],rotation=[0,0,-43]),
        c([-2.1,24.1,-4.40],[5.3,1.30,0.62],"void",pivot=[0.55,24.75,-4.09],rotation=[0,0,-57]),
        c([0.35,28.8,-4.30],[4.4,1.05,0.58],"silver",pivot=[2.55,29.3,-4.01],rotation=[0,0,25]),
        c([0.75,26.7,-4.32],[3.9,0.82,0.54],"gold",pivot=[2.7,27.1,-4.05],rotation=[0,0,43]),
    ]}


def back_crest():
    return {"name":"ouros_aura_back_crest","parent":"torso3","pivot":[-3.8,29.3,3.0],"cubes":[
        c([-7.0,28.2,2.7],[5.8,1.7,2.35],"void",pivot=[-4.1,29.0,3.85],rotation=[-9,0,-19]),
        c([-8.8,30.2,2.9],[5.4,1.45,2.05],"indigo",pivot=[-6.1,30.9,3.90],rotation=[-11,0,-31]),
        c([-9.9,32.3,3.05],[4.6,1.22,1.75],"cobalt",pivot=[-7.6,32.9,3.92],rotation=[-13,0,-43]),
        c([-9.7,34.25,3.2],[3.6,0.96,1.45],"amethyst",pivot=[-7.9,34.7,3.92],rotation=[-15,0,-55]),
        c([-8.55,35.75,3.3],[2.55,0.72,1.10],"aura",pivot=[-7.3,36.1,3.85],rotation=[-17,0,-65]),
    ]}


def armguard(name,parent,left):
    sx=1 if left else -1
    x=1.0 if left else -4.8
    rz=-11 if left else 11
    accent="cobalt" if left else "amethyst"
    return {"name":name,"parent":parent,"pivot":[sx*2.9,17.2,-1.0],"cubes":[
        c([x,14.9,-1.95],[3.9,1.05,2.45],"void",pivot=[sx*2.95,16.4,-0.72],rotation=[-6,0,rz]),
        c([x+0.25*sx,17.1,-2.05],[3.4,0.92,2.15],accent,pivot=[sx*2.95,17.55,-0.98],rotation=[-9,0,rz*0.7]),
        c([x+0.55*sx,19.1,-2.15],[2.7,0.68,1.75],"gold",pivot=[sx*2.95,19.4,-1.28],rotation=[-11,0,rz*0.5]),
    ]}


def waist_sash():
    return {"name":"ouros_aura_waist_sash","parent":"torso","pivot":[-1.6,18.0,2.3],"cubes":[
        c([-5.2,17.8,1.6],[6.8,1.2,2.15],"void",pivot=[-1.8,18.4,2.65],rotation=[-7,0,-15]),
        c([-5.0,15.25,2.15],[6.1,1.05,1.8],"indigo",pivot=[-1.95,15.75,3.05],rotation=[-10,0,-24]),
        c([-4.35,12.9,2.55],[5.2,0.92,1.5],"cobalt",pivot=[-1.75,13.35,3.30],rotation=[-12,0,-32]),
        c([-0.2,17.6,2.0],[4.2,0.78,1.55],"gold",pivot=[1.9,17.98,2.78],rotation=[-8,0,15]),
    ]}


def trailing_ribbon():
    return {"name":"ouros_aura_trailing_ribbon","parent":"torso","pivot":[-2.2,16.0,3.35],"cubes":[
        c([-5.4,14.2,3.0],[5.6,1.30,1.35],"amethyst",pivot=[-2.6,14.85,3.67],rotation=[-12,0,-18]),
        c([-5.7,11.0,3.25],[5.0,1.12,1.18],"indigo",pivot=[-3.2,11.55,3.84],rotation=[-14,0,-29]),
        c([-5.35,8.2,3.45],[4.35,0.96,1.02],"cobalt",pivot=[-3.18,8.68,3.96],rotation=[-16,0,-39]),
        c([-4.7,5.9,3.58],[3.6,0.78,0.88],"aura",pivot=[-2.9,6.3,4.02],rotation=[-18,0,-48]),
    ]}


def greave(name,parent,left):
    sx=1 if left else -1
    x=1.35 if left else -5.05
    rz=-7 if left else 7
    accent="cobalt" if left else "indigo"
    return {"name":name,"parent":parent,"pivot":[sx*3.4,3.0,-1.35],"cubes":[
        c([x,-0.2,-2.15],[3.7,1.05,2.7],"void",pivot=[sx*3.2,1.7,-0.8],rotation=[-7,0,rz]),
        c([x+0.18*sx,2.65,-2.25],[3.35,0.92,2.35],accent,pivot=[sx*3.2,3.1,-1.08],rotation=[-10,0,rz*0.72]),
        c([x+0.48*sx,5.0,-2.32],[2.65,0.68,1.85],"gold",pivot=[sx*3.2,5.32,-1.40],rotation=[-12,0,rz*0.5]),
    ]}


def build_model(source:Path):
    data=json.loads(source.read_text(encoding="utf-8"))
    geo=data["minecraft:geometry"][0]
    original=len(geo["bones"])
    geo["description"]["identifier"]="geometry.ouros_aura_sentinel_lucario"
    geo["bones"].extend([
        crown(), mantle(), chest_sash(), back_crest(),
        armguard("ouros_aura_left_armguard","arm_left2",True),
        armguard("ouros_aura_right_armguard","arm_right2",False),
        waist_sash(), trailing_ribbon(),
        greave("ouros_aura_left_greave","leg_left4",True),
        greave("ouros_aura_right_greave","leg_right4",False),
    ])
    return data, original

base.build_model=build_model
if __name__=="__main__":
    base.main()
