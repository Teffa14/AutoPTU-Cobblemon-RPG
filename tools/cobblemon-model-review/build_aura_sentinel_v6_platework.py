#!/usr/bin/env python3
"""Aura Sentinel V6 platework iteration after V5 internal artistic fail.

The exact official Lucario body remains untouched. V6 removes V5's horizontal
visor bar, fragmented dorsal hardware and skirt-heavy lower read. Added geometry
is deliberately flatter, more diagonal and more hierarchical: split brow crown,
one dominant asymmetric shoulder/relic blade, a compact diamond dorsal sigil,
open chest V, rear coat tails and narrow greaves.
"""
from __future__ import annotations

import importlib.util
import json
from pathlib import Path

PIPELINE_PATH = Path(__file__).with_name("build_aura_sentinel_v3_current.py")
spec = importlib.util.spec_from_file_location("aura_pipeline", PIPELINE_PATH)
pipeline = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(pipeline)
base = pipeline.base
c = base.cube


def helm_system() -> dict:
    return {"name":"ouros_aura_helm_system","parent":"head_angle","pivot":[0,38.0,-1.2],"cubes":[
        c([-5.35,35.30,-3.55],[1.15,3.85,3.30],"indigo",pivot=[-4.8,37.1,-1.9],rotation=[0,0,-12]),
        c([4.20,35.30,-3.55],[1.15,3.85,3.30],"cobalt",pivot=[4.8,37.1,-1.9],rotation=[0,0,12]),
        c([-4.50,38.20,-4.25],[3.65,0.72,0.42],"silver",pivot=[-2.68,38.56,-4.04],rotation=[0,0,-11]),
        c([0.85,38.20,-4.25],[3.65,0.72,0.42],"gold",pivot=[2.68,38.56,-4.04],rotation=[0,0,11]),
        c([-7.55,39.10,-0.55],[3.70,0.90,1.45],"amethyst",pivot=[-5.7,39.55,0.18],rotation=[0,0,-32]),
        c([3.95,39.10,-0.55],[2.85,0.72,1.35],"void",pivot=[5.38,39.46,0.13],rotation=[0,0,24]),
    ]}


def mantle_shell() -> dict:
    return {"name":"ouros_aura_mantle_shell","parent":"torso3","pivot":[0,29.0,0],"cubes":[
        c([-4.80,29.10,-2.55],[9.60,1.15,5.10],"void",pivot=[0,29.68,0],rotation=[4,0,0]),
        c([-10.70,29.00,-2.30],[6.40,1.65,4.70],"indigo",pivot=[-7.5,29.83,0.05],rotation=[0,0,-22]),
        c([-13.80,31.00,-1.65],[5.20,1.25,3.45],"amethyst",pivot=[-11.2,31.63,0.08],rotation=[0,0,-36]),
        c([-16.45,33.05,-1.15],[4.25,0.92,2.55],"cobalt",pivot=[-14.33,33.51,0.12],rotation=[0,0,-48]),
        c([4.10,29.15,-2.05],[4.70,1.35,4.10],"cobalt",pivot=[6.45,29.83,0],rotation=[0,0,15]),
        c([6.75,30.40,-1.35],[3.15,0.80,2.75],"gold",pivot=[8.33,30.8,0.03],rotation=[0,0,27]),
    ]}


def breastplate() -> dict:
    return {"name":"ouros_aura_breastplate","parent":"torso3","pivot":[0,27.0,-3.3],"cubes":[
        c([-4.85,28.20,-4.05],[4.30,1.15,0.48],"indigo",pivot=[-2.7,28.78,-3.81],rotation=[0,0,-22]),
        c([0.55,28.20,-4.05],[4.30,1.15,0.48],"cobalt",pivot=[2.7,28.78,-3.81],rotation=[0,0,22]),
        c([-4.00,25.45,-4.08],[3.05,3.15,0.44],"void",pivot=[-2.48,27.03,-3.86],rotation=[0,0,-13]),
        c([0.95,25.45,-4.08],[3.05,3.15,0.44],"void",pivot=[2.48,27.03,-3.86],rotation=[0,0,13]),
        c([-2.55,24.80,-4.28],[2.20,0.72,0.28],"gold",pivot=[-1.45,25.16,-4.14],rotation=[0,0,-24]),
        c([-0.52,26.70,-4.45],[1.04,1.04,0.20],"aura",pivot=[0,27.22,-4.35],rotation=[0,0,45]),
    ]}


def shrine_frame() -> dict:
    return {"name":"ouros_aura_shrine_frame","parent":"torso3","pivot":[0,31.0,3.2],"cubes":[
        c([-3.70,26.35,2.95],[7.40,1.55,1.25],"void",pivot=[0,27.13,3.58],rotation=[-7,0,0]),
        c([-2.25,29.00,3.05],[4.50,4.50,0.70],"indigo",pivot=[0,31.25,3.40],rotation=[0,0,45]),
        c([-1.35,29.90,3.55],[2.70,2.70,0.26],"aura",pivot=[0,31.25,3.68],rotation=[0,0,45]),
        c([-7.25,30.55,3.05],[5.35,0.90,1.05],"amethyst",pivot=[-4.58,31.0,3.58],rotation=[0,0,-34]),
        c([1.90,30.55,3.05],[4.35,0.80,1.00],"cobalt",pivot=[4.08,30.95,3.55],rotation=[0,0,31]),
        c([-5.10,33.45,3.30],[2.95,0.45,0.65],"gold",pivot=[-3.63,33.68,3.63],rotation=[0,0,-52]),
    ]}


def armguard(name:str,parent:str,left:bool) -> dict:
    x=0.85 if left else -4.55
    pivot=[2.65 if left else -2.65,18.1,-0.2]
    sign=-1 if left else 1
    return {"name":name,"parent":parent,"pivot":pivot,"cubes":[
        c([x,15.35,-1.85],[3.70,4.95,0.72],"void",pivot=pivot,rotation=[0,0,sign*7]),
        c([x+(2.80 if left else 0.12),16.15,-1.30],[0.65,3.95,2.15],"indigo",pivot=pivot,rotation=[0,0,sign*11]),
        c([x+0.65,18.25,-2.03],[2.45,0.28,0.18],"aura",pivot=pivot,rotation=[0,0,sign*7]),
    ]}


def waistcoat() -> dict:
    return {"name":"ouros_aura_waistcoat","parent":"torso","pivot":[0,19.5,1.8],"cubes":[
        c([-4.95,19.20,-3.10],[9.90,1.10,6.20],"void",pivot=[0,19.75,0],rotation=[0,0,0]),
        c([-4.50,12.50,2.90],[3.65,7.05,0.68],"indigo",pivot=[-2.68,19.0,3.24],rotation=[-8,0,13]),
        c([0.45,13.20,2.95],[3.50,6.35,0.68],"amethyst",pivot=[2.20,19.0,3.29],rotation=[-8,0,-11]),
        c([-5.20,16.20,-3.28],[1.20,3.45,0.38],"cobalt",pivot=[-4.60,19.0,-3.09],rotation=[0,0,-9]),
        c([4.00,16.80,-3.28],[1.20,2.85,0.38],"gold",pivot=[4.60,19.0,-3.09],rotation=[0,0,9]),
    ]}


def relic_fin() -> dict:
    return {"name":"ouros_aura_relic_fin","parent":"torso3","pivot":[-8.0,31.0,2.6],"cubes":[
        c([-9.10,28.50,1.65],[3.40,4.05,2.15],"void",pivot=[-7.4,30.53,2.73],rotation=[0,0,-18]),
        c([-12.55,30.25,2.00],[5.00,2.05,1.65],"indigo",pivot=[-10.05,31.28,2.83],rotation=[0,0,-35]),
        c([-16.30,32.55,2.15],[5.15,1.55,1.45],"amethyst",pivot=[-13.73,33.33,2.88],rotation=[0,0,-49]),
        c([-19.10,35.25,2.30],[4.45,1.08,1.20],"cobalt",pivot=[-16.88,35.79,2.90],rotation=[0,0,-61]),
        c([-16.25,34.00,3.68],[3.05,0.30,0.18],"aura",pivot=[-14.73,34.15,3.77],rotation=[0,0,-52]),
    ]}


def greave(name:str,parent:str,left:bool) -> dict:
    x=1.55 if left else -5.55
    pivot=[3.55 if left else -3.55,5.5,-1.3]
    sign=-1 if left else 1
    return {"name":name,"parent":parent,"pivot":pivot,"cubes":[
        c([x,-1.15,-1.95],[4.00,5.85,0.65],"void",pivot=pivot,rotation=[-4,0,sign*4]),
        c([x+(3.05 if left else -0.18),0.10,-1.35],[0.72,4.25,2.15],"indigo",pivot=pivot,rotation=[0,0,sign*8]),
        c([x+0.45,4.10,-2.08],[3.10,1.05,0.88],"cobalt" if left else "amethyst",pivot=pivot,rotation=[-10,0,sign*7]),
        c([x+0.90,1.75,-2.18],[2.10,0.25,0.16],"aura",pivot=pivot,rotation=[-4,0,sign*4]),
    ]}


def build_model(source:Path)->tuple[dict,int]:
    data=json.loads(source.read_text(encoding="utf-8"))
    geo=data["minecraft:geometry"][0]
    original=len(geo["bones"])
    geo["description"]["identifier"]="geometry.ouros_aura_sentinel_lucario"
    geo["bones"].extend([
        helm_system(),mantle_shell(),breastplate(),shrine_frame(),
        armguard("ouros_aura_left_armguard","arm_left2",True),
        armguard("ouros_aura_right_armguard","arm_right2",False),
        waistcoat(),relic_fin(),
        greave("ouros_aura_left_greave","leg_left4",True),
        greave("ouros_aura_right_greave","leg_right4",False),
    ])
    return data,original

base.helm_system=helm_system
base.mantle_shell=mantle_shell
base.breastplate=breastplate
base.shrine_frame=shrine_frame
base.armguard=armguard
base.waistcoat=waistcoat
base.relic_fin=relic_fin
base.build_model=build_model

if __name__=="__main__":
    base.main()
