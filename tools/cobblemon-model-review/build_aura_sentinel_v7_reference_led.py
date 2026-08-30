#!/usr/bin/env python3
"""Aura Sentinel V7.1: reference-led continuous macro-form rebuild.

Preserves the exact official Lucario body. This pass removes V7's detached-looking
bars and skirt-like plate field, using fewer overlapping rotated forms with a
single shoulder-to-back signature sweep. Only generic technique lessons from the
same-species reference dossier are used. No third-party geometry, UVs, palette
layouts, motifs or texture bytes are copied.
"""
from __future__ import annotations
import importlib.util, json
from pathlib import Path

PIPELINE_PATH=Path(__file__).with_name("build_aura_sentinel_v3_current.py")
spec=importlib.util.spec_from_file_location("aura_pipeline",PIPELINE_PATH)
pipeline=importlib.util.module_from_spec(spec); assert spec.loader is not None; spec.loader.exec_module(pipeline)
base=pipeline.base
c=base.cube


def helm_system()->dict:
    return {"name":"ouros_aura_helm_system","parent":"head_angle","pivot":[0,37.0,-1.0],"cubes":[
        c([-5.20,35.35,0.55],[10.40,3.20,1.40],"void",pivot=[0,36.95,1.25],rotation=[-5,0,0]),
        c([-5.65,35.05,-2.45],[1.55,3.85,3.35],"indigo",pivot=[-4.88,36.98,-0.78],rotation=[0,0,-10]),
        c([4.10,35.05,-2.45],[1.55,3.85,3.35],"cobalt",pivot=[4.88,36.98,-0.78],rotation=[0,0,10]),
        c([-3.15,38.00,-3.42],[6.30,0.72,0.42],"gold",pivot=[0,38.36,-3.21],rotation=[0,0,0])
    ]}


def mantle_shell()->dict:
    return {"name":"ouros_aura_mantle_shell","parent":"torso3","pivot":[0,29.2,0],"cubes":[
        c([-7.05,28.05,-2.85],[14.10,2.05,5.65],"void",pivot=[0,29.08,-0.02],rotation=[4,0,0]),
        c([-9.15,27.85,-2.25],[6.25,3.15,4.75],"indigo",pivot=[-6.03,29.43,0.13],rotation=[0,0,-19]),
        c([-11.05,29.15,-1.95],[5.35,2.80,4.20],"amethyst",pivot=[-8.38,30.55,0.15],rotation=[0,0,-32]),
        c([4.95,28.35,-2.25],[4.20,1.80,4.45],"cobalt",pivot=[7.05,29.25,-0.03],rotation=[0,0,14]),
        c([-4.80,30.00,2.25],[9.60,2.35,1.40],"void",pivot=[0,31.18,2.95],rotation=[-8,0,0])
    ]}


def breastplate()->dict:
    return {"name":"ouros_aura_breastplate","parent":"torso3","pivot":[0,27.0,-3.2],"cubes":[
        c([-4.65,28.20,-4.02],[4.00,1.15,0.46],"indigo",pivot=[-2.65,28.78,-3.79],rotation=[0,0,-22]),
        c([0.65,28.20,-4.02],[4.00,1.15,0.46],"cobalt",pivot=[2.65,28.78,-3.79],rotation=[0,0,22]),
        c([-4.00,25.10,-4.05],[2.85,3.50,0.48],"void",pivot=[-2.58,26.85,-3.81],rotation=[0,0,-14]),
        c([1.15,25.10,-4.05],[2.85,3.50,0.48],"void",pivot=[2.58,26.85,-3.81],rotation=[0,0,14]),
        c([-1.05,26.20,-4.30],[2.10,0.58,0.24],"gold",pivot=[0,26.49,-4.18],rotation=[0,0,45])
    ]}


def shrine_frame()->dict:
    return {"name":"ouros_aura_shrine_frame","parent":"torso3","pivot":[-5.5,29.5,3.0],"cubes":[
        c([-8.00,26.60,2.45],[5.50,3.45,2.05],"void",pivot=[-5.25,28.33,3.48],rotation=[-6,0,-15]),
        c([-10.55,28.25,2.55],[5.65,3.25,1.90],"indigo",pivot=[-7.73,29.88,3.50],rotation=[-4,0,-28]),
        c([-12.65,30.65,2.65],[5.25,2.85,1.72],"amethyst",pivot=[-10.03,32.08,3.51],rotation=[-2,0,-41]),
        c([-13.95,33.45,2.75],[4.35,2.20,1.55],"cobalt",pivot=[-11.78,34.55,3.53],rotation=[0,0,-54])
    ]}


def armguard(name:str,parent:str,left:bool)->dict:
    if left:
        return {"name":name,"parent":parent,"pivot":[2.65,18.1,-0.2],"cubes":[
            c([1.05,15.35,-1.72],[3.45,4.45,0.62],"void",pivot=[2.78,17.58,-1.41],rotation=[0,0,-7]),
            c([3.45,16.10,-1.18],[0.60,3.30,1.90],"indigo",pivot=[3.75,17.75,-0.23],rotation=[0,0,-12])
        ]}
    return {"name":name,"parent":parent,"pivot":[-2.65,18.1,-0.2],"cubes":[
        c([-4.50,15.55,-1.70],[3.30,4.15,0.60],"void",pivot=[-2.85,17.63,-1.40],rotation=[0,0,7]),
        c([-4.25,16.35,-1.15],[0.58,3.05,1.85],"cobalt",pivot=[-3.96,17.88,-0.23],rotation=[0,0,11])
    ]}


def waistcoat()->dict:
    return {"name":"ouros_aura_waistcoat","parent":"torso","pivot":[0,19.3,2.0],"cubes":[
        c([-4.65,18.75,2.45],[4.05,1.35,1.35],"void",pivot=[-2.63,19.43,3.13],rotation=[-7,0,8]),
        c([0.60,18.75,2.45],[4.05,1.35,1.35],"void",pivot=[2.63,19.43,3.13],rotation=[-7,0,-8]),
        c([-3.25,14.20,2.85],[6.50,5.05,0.56],"indigo",pivot=[0,18.55,3.13],rotation=[-9,0,0])
    ]}


def relic_fin()->dict:
    return {"name":"ouros_aura_relic_fin","parent":"torso3","pivot":[-9.0,30.0,2.7],"cubes":[
        c([-11.20,27.75,2.15],[5.65,4.45,2.00],"void",pivot=[-8.38,29.98,3.15],rotation=[-3,0,-27]),
        c([-14.45,30.05,2.25],[5.85,3.65,1.78],"indigo",pivot=[-11.53,31.88,3.14],rotation=[-2,0,-43]),
        c([-16.60,33.15,2.35],[5.10,2.75,1.55],"amethyst",pivot=[-14.05,34.53,3.13],rotation=[0,0,-57])
    ]}


def greave(name:str,parent:str,left:bool)->dict:
    if left:
        return {"name":name,"parent":parent,"pivot":[3.55,5.5,-1.3],"cubes":[
            c([1.55,-1.00,-1.86],[3.80,5.50,0.60],"void",pivot=[3.45,1.75,-1.56],rotation=[-5,0,-4]),
            c([4.30,0.15,-1.25],[0.65,3.95,1.95],"cobalt",pivot=[4.63,2.13,-0.28],rotation=[0,0,-8]),
            c([1.95,3.95,-2.00],[3.00,0.95,0.76],"indigo",pivot=[3.45,4.43,-1.62],rotation=[-9,0,-5])
        ]}
    return {"name":name,"parent":parent,"pivot":[-3.55,5.5,-1.3],"cubes":[
        c([-5.35,-1.00,-1.86],[3.80,5.50,0.60],"void",pivot=[-3.45,1.75,-1.56],rotation=[-5,0,4]),
        c([-5.00,0.15,-1.25],[0.65,3.95,1.95],"amethyst",pivot=[-4.68,2.13,-0.28],rotation=[0,0,8]),
        c([-4.95,3.95,-2.00],[3.00,0.95,0.76],"indigo",pivot=[-3.45,4.43,-1.62],rotation=[-9,0,5])
    ]}


def build_model(source:Path)->tuple[dict,int]:
    data=json.loads(source.read_text(encoding="utf-8")); geo=data["minecraft:geometry"][0]; original=len(geo["bones"])
    geo["description"]["identifier"]="geometry.ouros_aura_sentinel_lucario"
    geo["bones"].extend([
        helm_system(),mantle_shell(),breastplate(),shrine_frame(),
        armguard("ouros_aura_left_armguard","arm_left2",True),
        armguard("ouros_aura_right_armguard","arm_right2",False),
        waistcoat(),relic_fin(),
        greave("ouros_aura_left_greave","leg_left4",True),
        greave("ouros_aura_right_greave","leg_right4",False)
    ])
    return data,original

base.helm_system=helm_system; base.mantle_shell=mantle_shell; base.breastplate=breastplate; base.shrine_frame=shrine_frame
base.armguard=armguard; base.waistcoat=waistcoat; base.relic_fin=relic_fin; base.build_model=build_model
if __name__=="__main__": base.main()
