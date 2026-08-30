#!/usr/bin/env python3
"""Aura Sentinel V7: reference-led macro-form rebuild after owner rejection.

Preserves the exact official Lucario body and replaces V6's fragmented bar/plate
language with fewer, broader, rotated and overlapping systems. The design uses
only generic lessons from same-species references: selective silhouette density,
one dominant asymmetric system, oriented planes, negative space and full-body
continuation. No third-party geometry, UVs, palette layouts, motifs or texture
bytes are copied.
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
    return {"name":"ouros_aura_helm_system","parent":"head_angle","pivot":[0,38.0,-1.0],"cubes":[
        c([-5.45,35.15,0.70],[10.90,4.25,1.55],"void",pivot=[0,37.28,1.48],rotation=[-5,0,0]),
        c([-5.80,35.05,-2.60],[1.25,4.00,3.80],"indigo",pivot=[-5.18,37.05,-0.70],rotation=[0,0,-12]),
        c([4.55,35.05,-2.60],[1.25,4.00,3.80],"cobalt",pivot=[5.18,37.05,-0.70],rotation=[0,0,12]),
        c([-4.80,38.35,-3.55],[4.55,0.72,0.42],"silver",pivot=[-2.53,38.71,-3.34],rotation=[0,0,-16]),
        c([0.25,38.35,-3.55],[4.55,0.72,0.42],"gold",pivot=[2.53,38.71,-3.34],rotation=[0,0,16]),
        c([-6.55,39.05,0.15],[3.35,1.25,1.85],"amethyst",pivot=[-4.88,39.68,1.08],rotation=[0,0,-28])
    ]}


def mantle_shell()->dict:
    return {"name":"ouros_aura_mantle_shell","parent":"torso3","pivot":[0,29.5,0],"cubes":[
        c([-7.20,28.20,-2.95],[14.40,2.05,5.90],"void",pivot=[0,29.23,0],rotation=[4,0,0]),
        c([-10.45,28.45,-2.45],[5.30,2.25,5.20],"indigo",pivot=[-7.80,29.58,0.15],rotation=[0,0,-20]),
        c([-13.10,30.05,-1.95],[4.75,1.75,4.30],"amethyst",pivot=[-10.72,30.93,0.20],rotation=[0,0,-34]),
        c([5.05,28.50,-2.35],[4.25,1.75,4.65],"cobalt",pivot=[7.18,29.38,-0.02],rotation=[0,0,15]),
        c([-4.95,30.00,2.45],[9.90,2.45,1.45],"void",pivot=[0,31.23,3.18],rotation=[-8,0,0])
    ]}


def breastplate()->dict:
    return {"name":"ouros_aura_breastplate","parent":"torso3","pivot":[0,27.0,-3.2],"cubes":[
        c([-4.75,28.35,-4.02],[4.10,1.25,0.46],"indigo",pivot=[-2.70,28.98,-3.79],rotation=[0,0,-24]),
        c([0.65,28.35,-4.02],[4.10,1.25,0.46],"cobalt",pivot=[2.70,28.98,-3.79],rotation=[0,0,24]),
        c([-4.15,25.25,-4.08],[3.00,3.55,0.48],"void",pivot=[-2.65,27.03,-3.84],rotation=[0,0,-15]),
        c([1.15,25.25,-4.08],[3.00,3.55,0.48],"void",pivot=[2.65,27.03,-3.84],rotation=[0,0,15]),
        c([-1.20,26.15,-4.34],[2.40,0.62,0.24],"gold",pivot=[0,26.46,-4.22],rotation=[0,0,45])
    ]}


def shrine_frame()->dict:
    return {"name":"ouros_aura_shrine_frame","parent":"torso3","pivot":[-3.0,31.0,3.2],"cubes":[
        c([-7.10,26.15,2.70],[5.20,2.35,2.00],"void",pivot=[-4.50,27.33,3.70],rotation=[-7,0,-13]),
        c([-9.45,28.00,2.85],[5.65,2.55,1.85],"indigo",pivot=[-6.63,29.28,3.78],rotation=[-5,0,-25]),
        c([-11.65,30.35,3.00],[5.45,2.35,1.70],"amethyst",pivot=[-8.93,31.53,3.85],rotation=[-3,0,-39]),
        c([-13.10,33.05,3.10],[4.65,1.95,1.55],"cobalt",pivot=[-10.78,34.03,3.88],rotation=[0,0,-53]),
        c([-8.15,31.00,4.35],[2.15,2.15,0.28],"aura",pivot=[-7.08,32.08,4.49],rotation=[0,0,45])
    ]}


def armguard(name:str,parent:str,left:bool)->dict:
    if left:
        return {"name":name,"parent":parent,"pivot":[2.65,18.1,-0.2],"cubes":[
            c([1.05,15.30,-1.78],[3.50,4.55,0.66],"void",pivot=[2.80,17.58,-1.45],rotation=[0,0,-8]),
            c([3.55,16.05,-1.25],[0.62,3.45,2.00],"indigo",pivot=[3.86,17.78,-0.25],rotation=[0,0,-14])
        ]}
    return {"name":name,"parent":parent,"pivot":[-2.65,18.1,-0.2],"cubes":[
        c([-4.55,15.55,-1.75],[3.35,4.20,0.62],"void",pivot=[-2.88,17.65,-1.44],rotation=[0,0,7]),
        c([-4.35,16.35,-1.18],[0.58,3.10,1.90],"cobalt",pivot=[-4.06,17.90,-0.23],rotation=[0,0,12])
    ]}


def waistcoat()->dict:
    return {"name":"ouros_aura_waistcoat","parent":"torso","pivot":[0,19.5,1.9],"cubes":[
        c([-4.80,18.75,2.50],[4.25,1.45,1.45],"void",pivot=[-2.68,19.48,3.23],rotation=[-7,0,9]),
        c([0.55,18.75,2.50],[4.25,1.45,1.45],"void",pivot=[2.68,19.48,3.23],rotation=[-7,0,-9]),
        c([-4.40,12.65,2.95],[3.55,6.65,0.62],"indigo",pivot=[-2.63,18.70,3.26],rotation=[-10,0,12]),
        c([0.45,13.20,2.95],[3.40,6.05,0.62],"amethyst",pivot=[2.15,18.70,3.26],rotation=[-10,0,-10])
    ]}


def relic_fin()->dict:
    return {"name":"ouros_aura_relic_fin","parent":"torso3","pivot":[-8.0,31.0,2.8],"cubes":[
        c([-10.20,28.20,1.75],[4.25,4.10,2.15],"void",pivot=[-8.08,30.25,2.83],rotation=[0,0,-18]),
        c([-13.60,30.00,2.00],[5.35,3.10,1.85],"indigo",pivot=[-10.93,31.55,2.93],rotation=[0,0,-33]),
        c([-16.55,32.65,2.15],[5.05,2.35,1.60],"amethyst",pivot=[-14.03,33.83,2.95],rotation=[0,0,-49]),
        c([-18.30,35.55,2.30],[4.20,1.55,1.35],"cobalt",pivot=[-16.20,36.33,2.98],rotation=[0,0,-62])
    ]}


def greave(name:str,parent:str,left:bool)->dict:
    if left:
        return {"name":name,"parent":parent,"pivot":[3.55,5.5,-1.3],"cubes":[
            c([1.55,-1.00,-1.88],[3.85,5.55,0.62],"void",pivot=[3.48,1.78,-1.57],rotation=[-5,0,-4]),
            c([4.35,0.15,-1.28],[0.68,4.00,2.05],"cobalt",pivot=[4.69,2.15,-0.25],rotation=[0,0,-9]),
            c([1.95,4.00,-2.02],[3.05,1.00,0.80],"indigo",pivot=[3.48,4.50,-1.62],rotation=[-10,0,-6])
        ]}
    return {"name":name,"parent":parent,"pivot":[-3.55,5.5,-1.3],"cubes":[
        c([-5.40,-1.00,-1.88],[3.85,5.55,0.62],"void",pivot=[-3.48,1.78,-1.57],rotation=[-5,0,4]),
        c([-5.05,0.15,-1.28],[0.68,4.00,2.05],"amethyst",pivot=[-4.71,2.15,-0.25],rotation=[0,0,9]),
        c([-5.00,4.00,-2.02],[3.05,1.00,0.80],"indigo",pivot=[-3.48,4.50,-1.62],rotation=[-10,0,6])
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
