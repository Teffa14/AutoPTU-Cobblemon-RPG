#!/usr/bin/env python3
"""Aura Sentinel V7.2 — tapered aura-mantle rebuild.

Presentation only. The exact official Cobblemon Lucario geometry remains intact.
This pass intentionally rejects the V7/V7.1 box-armour language. It builds one
primary asymmetric mantle/sash system from thin overlapping rotated planes, then
uses small motion-parented accents to carry the same diagonal rhythm through the
head, arms and legs. No third-party geometry, texture, UV, palette layout,
motif, costume or silhouette is copied; only generic techniques recorded in the
validated same-species dossier are applied.
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
    return {"name":"ouros_aura_crown","parent":"head_angle","pivot":[0,38.2,-1.0],"cubes":[
        c([-4.3,38.1,-3.85],[4.2,0.62,0.42],"gold",pivot=[-2.1,38.4,-3.64],rotation=[0,0,-18]),
        c([0.1,38.1,-3.85],[4.2,0.62,0.42],"silver",pivot=[2.1,38.4,-3.64],rotation=[0,0,18]),
        c([-0.45,38.35,-4.05],[0.9,2.25,0.48],"aura",pivot=[0,38.55,-3.8],rotation=[-8,0,0]),
    ]}


def mantle():
    return {"name":"ouros_aura_mantle","parent":"torso3","pivot":[-4.8,29.2,1.6],"cubes":[
        c([-8.4,28.2,-2.6],[8.0,1.25,5.3],"indigo",pivot=[-4.4,28.8,0.0],rotation=[5,-8,-20]),
        c([-10.6,26.4,-1.8],[8.2,1.15,4.5],"cobalt",pivot=[-6.5,27.0,0.4],rotation=[8,-10,-31]),
        c([-11.8,23.9,-0.9],[7.5,1.05,3.8],"amethyst",pivot=[-8.0,24.5,1.0],rotation=[10,-12,-42]),
        c([-11.9,21.2,-0.1],[6.4,0.95,3.15],"indigo",pivot=[-8.7,21.7,1.45],rotation=[12,-14,-52]),
        c([-10.8,18.8,0.55],[5.2,0.82,2.55],"cobalt",pivot=[-8.2,19.2,1.8],rotation=[14,-16,-60]),
        c([-7.9,28.45,-2.9],[6.7,0.34,0.28],"aura",pivot=[-4.55,28.62,-2.75],rotation=[4,-8,-20]),
        c([-9.85,26.55,-2.05],[6.4,0.30,0.26],"gold",pivot=[-6.65,26.70,-1.92],rotation=[6,-10,-31]),
        c([-11.0,24.05,-1.15],[5.6,0.28,0.24],"silver",pivot=[-8.2,24.20,-1.03],rotation=[8,-12,-42]),
    ]}


def chest_sash():
    return {"name":"ouros_aura_chest_sash","parent":"torso3","pivot":[0,27.2,-3.5],"cubes":[
        c([-5.0,29.0,-4.25],[5.2,0.72,0.46],"indigo",pivot=[-2.4,29.35,-4.02],rotation=[0,0,-28]),
        c([-3.45,25.0,-4.28],[4.4,0.68,0.44],"cobalt",pivot=[-1.25,25.34,-4.06],rotation=[0,0,-55]),
        c([-0.2,29.0,-4.25],[5.2,0.72,0.46],"silver",pivot=[2.4,29.35,-4.02],rotation=[0,0,28]),
        c([-0.95,25.0,-4.28],[4.4,0.68,0.44],"gold",pivot=[1.25,25.34,-4.06],rotation=[0,0,55]),
    ]}


def back_crest():
    return {"name":"ouros_aura_back_crest","parent":"torso3","pivot":[-4.5,30.0,3.0],"cubes":[
        c([-7.2,29.1,2.7],[4.8,0.78,2.0],"void",pivot=[-4.8,29.5,3.7],rotation=[-9,0,-24]),
        c([-9.1,30.8,2.9],[4.4,0.68,1.65],"indigo",pivot=[-6.9,31.1,3.7],rotation=[-12,0,-39]),
        c([-10.2,33.0,3.1],[3.8,0.58,1.35],"amethyst",pivot=[-8.3,33.3,3.78],rotation=[-14,0,-54]),
        c([-8.8,34.55,3.2],[2.9,0.42,1.0],"aura",pivot=[-7.35,34.76,3.7],rotation=[-15,0,-66]),
    ]}


def armguard(name,parent,left):
    sx=1 if left else -1
    x=1.15 if left else -4.65
    r=-12 if left else 12
    accent="cobalt" if left else "amethyst"
    return {"name":name,"parent":parent,"pivot":[sx*2.8,17.4,-1.0],"cubes":[
        c([x,15.2,-1.75],[3.5,0.72,2.15],accent,pivot=[sx*2.85,17.1,-0.68],rotation=[-5,0,r]),
        c([x+0.35*sx,17.75,-1.95],[2.8,0.50,1.75],"gold",pivot=[sx*2.85,17.9,-1.05],rotation=[-8,0,r*0.65]),
    ]}


def waist_sash():
    return {"name":"ouros_aura_waist_sash","parent":"torso","pivot":[0,19.0,1.7],"cubes":[
        c([-5.0,18.7,-2.65],[10.0,0.72,5.7],"void",pivot=[0,19.05,0.2],rotation=[0,0,-5]),
        c([-4.6,17.55,2.2],[6.0,0.76,1.45],"gold",pivot=[-1.6,17.93,2.92],rotation=[-8,0,-18]),
        c([-3.9,14.6,2.55],[5.7,0.82,1.25],"indigo",pivot=[-1.05,15.0,3.18],rotation=[-10,0,-27]),
        c([0.9,17.4,2.25],[4.3,0.70,1.30],"cobalt",pivot=[3.05,17.75,2.90],rotation=[-8,0,14]),
        c([1.45,14.7,2.55],[3.7,0.72,1.12],"silver",pivot=[3.3,15.05,3.10],rotation=[-10,0,22]),
    ]}


def trailing_ribbon():
    return {"name":"ouros_aura_trailing_ribbon","parent":"torso","pivot":[-2.0,18.0,3.1],"cubes":[
        c([-4.7,15.0,3.0],[4.8,0.72,1.05],"amethyst",pivot=[-2.3,15.35,3.52],rotation=[-10,0,-20]),
        c([-5.0,11.9,3.2],[4.1,0.62,0.92],"cobalt",pivot=[-2.95,12.2,3.66],rotation=[-12,0,-31]),
        c([-4.65,10.8,3.28],[3.6,0.62,0.82],"aura",pivot=[-2.85,11.11,3.69],rotation=[-13,0,-39]),
    ]}


def greave(name,parent,left):
    sx=1 if left else -1
    x=1.45 if left else -4.95
    rz=-8 if left else 8
    accent="cobalt" if left else "indigo"
    return {"name":name,"parent":parent,"pivot":[sx*3.4,3.0,-1.4],"cubes":[
        c([x,0.0,-2.0],[3.5,0.78,2.5],accent,pivot=[sx*3.2,2.2,-0.75],rotation=[-7,0,rz]),
        c([x+0.2*sx,3.0,-2.15],[3.0,0.62,2.1],"void",pivot=[sx*3.2,3.3,-1.10],rotation=[-10,0,rz*0.75]),
        c([x+0.55*sx,5.15,-2.22],[2.25,0.48,1.65],"gold",pivot=[sx*3.2,5.38,-1.40],rotation=[-12,0,rz*0.55]),
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
