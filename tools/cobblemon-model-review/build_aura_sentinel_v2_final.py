#!/usr/bin/env python3
"""Final macro-silhouette pass for Aura Sentinel v2.

The refined pass improved armor coverage but the 3/4 render still read too much
like Lucario wearing plates. This pass keeps every official bone untouched and
turns the appended rear presentation system into a monumental aura shrine plus
large split ceremonial cloak so the fantasy survives gameplay-scale reduction.
"""
from __future__ import annotations

import importlib.util
from pathlib import Path

REFINED_PATH=Path(__file__).with_name('build_aura_sentinel_v2_refined.py')
spec=importlib.util.spec_from_file_location('aura_v2_refined',REFINED_PATH)
refined=importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(refined)
base=refined.base


def mantle_shell()->dict:
    c=base.cube
    return {"name":"ouros_aura_mantle_shell","parent":"torso3","pivot":[0,29.5,0.0],"cubes":[
        # Thick continuous shoulder mantle: one large garment mass, not separate pads.
        c([-9.20,28.55,-3.55],[18.40,2.45,7.10],"void"),
        c([-11.80,27.55,-3.05],[5.55,3.75,6.15],"indigo",pivot=[-9.03,29.43,0.03],rotation=[0,0,-13]),
        c([6.30,27.95,-2.85],[4.85,3.20,5.85],"cobalt",pivot=[8.72,29.55,0.08],rotation=[0,0,10]),
        c([-12.05,30.95,-2.68],[5.80,0.55,5.48],"silver",pivot=[-9.15,31.23,0.06],rotation=[0,0,-13]),
        c([6.18,30.82,-2.55],[5.05,0.50,5.25],"gold",pivot=[8.70,31.07,0.08],rotation=[0,0,10]),
        # Large left tower-pauldron and shorter right blade establish asymmetry.
        c([-11.65,29.75,-1.35],[1.55,7.05,2.70],"amethyst",pivot=[-10.88,33.28,0.00],rotation=[0,0,-10]),
        c([-11.20,36.45,-1.05],[0.92,4.15,2.10],"cobalt",pivot=[-10.74,38.53,0.00],rotation=[0,0,-18]),
        c([-11.05,37.00,-0.92],[0.32,3.20,1.70],"aura",pivot=[-10.89,38.60,-0.07],rotation=[0,0,-18]),
        c([9.45,29.45,-0.95],[3.75,0.82,2.35],"indigo",pivot=[11.33,29.86,0.23],rotation=[0,0,33]),
        c([9.82,30.28,-0.80],[2.85,0.32,1.88],"aura",pivot=[11.24,30.44,0.14],rotation=[0,0,33]),
        # High gorget connects the helmet, shoulders and rear shrine visually.
        c([-6.55,29.92,2.92],[13.10,3.65,1.55],"amethyst"),
        c([-5.55,33.10,3.15],[11.10,0.62,1.18],"gold"),
        c([-4.75,33.68,3.28],[9.50,0.32,0.92],"aura"),
        # Massive rear cloak shoulder blades begin immediately below the yoke.
        c([-8.85,23.10,3.72],[8.10,6.35,0.95],"indigo",pivot=[-4.80,28.65,4.20],rotation=[-5,0,4]),
        c([0.65,23.65,3.74],[7.70,5.80,0.95],"amethyst",pivot=[4.50,28.70,4.22],rotation=[-5,0,-4]),
        c([-8.50,22.95,4.60],[7.45,0.48,0.28],"silver",pivot=[-4.78,23.19,4.74],rotation=[-5,0,4]),
        c([0.95,23.50,4.62],[7.10,0.44,0.28],"gold",pivot=[4.50,23.72,4.76],rotation=[-5,0,-4]),
    ]}


def shrine_frame()->dict:
    c=base.cube
    return {"name":"ouros_aura_shrine_frame","parent":"torso3","pivot":[0,32.0,4.2],"cubes":[
        # Monumental open arch: thick cobalt/gold structure with cyan inner channel.
        c([-11.10,19.40,3.25],[2.05,18.90,1.70],"cobalt",pivot=[-10.08,28.85,4.10],rotation=[-2,0,-5]),
        c([9.05,19.40,3.25],[2.05,18.90,1.70],"amethyst",pivot=[10.08,28.85,4.10],rotation=[-2,0,5]),
        c([-10.55,20.15,4.90],[0.55,17.10,0.30],"aura",pivot=[-10.28,28.70,5.05],rotation=[-2,0,-5]),
        c([10.00,20.15,4.90],[0.55,17.10,0.30],"aura",pivot=[10.28,28.70,5.05],rotation=[-2,0,5]),
        c([-10.35,37.45,3.02],[20.70,1.60,2.15],"gold"),
        c([-9.20,39.02,3.18],[18.40,0.65,1.85],"indigo"),
        c([-8.20,39.65,3.38],[16.40,0.38,1.42],"aura"),
        c([-10.20,18.85,3.35],[20.40,1.20,1.55],"silver"),
        # Staggered side fins make the frame read as a fantasy shrine, not a rectangle.
        c([-14.25,32.70,3.20],[5.05,1.00,1.75],"amethyst",pivot=[-11.73,33.20,4.08],rotation=[0,0,-43]),
        c([-15.60,29.15,3.28],[5.60,0.90,1.68],"cobalt",pivot=[-12.80,29.60,4.12],rotation=[0,0,-54]),
        c([-13.35,35.55,3.25],[4.35,0.78,1.58],"gold",pivot=[-11.18,35.94,4.04],rotation=[0,0,-32]),
        c([9.15,33.00,3.20],[4.65,0.92,1.70],"cobalt",pivot=[11.48,33.46,4.05],rotation=[0,0,40]),
        c([9.95,29.60,3.28],[5.15,0.84,1.62],"amethyst",pivot=[12.53,30.02,4.09],rotation=[0,0,51]),
        c([8.95,35.72,3.25],[3.95,0.74,1.54],"silver",pivot=[10.93,36.09,4.02],rotation=[0,0,30]),
        # Bright channels ensure the signature survives at 160px.
        c([-13.60,33.40,3.58],[3.80,0.36,1.10],"aura",pivot=[-11.70,33.58,4.13],rotation=[0,0,-43]),
        c([-14.80,29.95,3.62],[4.20,0.34,1.04],"aura",pivot=[-12.70,30.12,4.14],rotation=[0,0,-54]),
        c([9.75,33.72,3.58],[3.45,0.34,1.08],"aura",pivot=[11.48,33.89,4.12],rotation=[0,0,40]),
        c([10.55,30.36,3.62],[3.85,0.32,1.02],"aura",pivot=[12.48,30.52,4.13],rotation=[0,0,51]),
        # Large central reliquary floats within the open top frame but is rigidly attached to torso3.
        c([-2.20,38.15,3.35],[4.40,4.40,0.62],"void",pivot=[0,40.35,3.66],rotation=[0,0,45]),
        c([-1.48,38.87,3.78],[2.96,2.96,0.34],"aura",pivot=[0,40.35,3.95],rotation=[0,0,45]),
        c([-0.42,40.95,3.82],[0.84,3.00,0.32],"gold"),
    ]}


def waistcoat()->dict:
    c=base.cube
    return {"name":"ouros_aura_waistcoat","parent":"torso","pivot":[0,20.6,1.0],"cubes":[
        c([-6.75,19.30,-4.20],[13.50,1.65,8.30],"void"),
        c([-6.95,20.82,-4.30],[13.90,0.48,8.50],"gold"),
        # Long split coat continues the cloak down the full body height.
        c([-6.45,8.75,3.30],[5.85,11.95,1.00],"indigo",pivot=[-3.53,19.90,3.80],rotation=[-6,0,8]),
        c([0.55,10.55,3.35],[5.50,10.15,1.00],"amethyst",pivot=[3.30,19.95,3.85],rotation=[-6,0,-7]),
        c([-7.10,11.15,-4.05],[2.70,9.30,0.62],"cobalt",pivot=[-5.75,18.90,-3.74],rotation=[0,0,-5]),
        c([4.40,12.30,-4.05],[2.70,8.15,0.62],"indigo",pivot=[5.75,19.05,-3.74],rotation=[0,0,5]),
        c([-6.10,8.68,4.26],[5.15,0.58,0.32],"silver",pivot=[-3.53,8.97,4.42],rotation=[-6,0,8]),
        c([0.95,10.48,4.31],[4.75,0.54,0.32],"gold",pivot=[3.33,10.75,4.47],rotation=[-6,0,-7]),
        c([-6.90,17.80,-4.08],[2.85,0.58,0.34],"aura",pivot=[-5.48,18.09,-3.91],rotation=[0,0,-5]),
        c([4.05,17.80,-4.08],[2.85,0.58,0.34],"aura",pivot=[5.48,18.09,-3.91],rotation=[0,0,5]),
        # Two broad back tabs bridge upper mantle and lower coat.
        c([-5.95,18.20,4.10],[5.15,2.85,0.72],"cobalt",pivot=[-3.38,20.10,4.46],rotation=[-4,0,5]),
        c([0.70,18.55,4.12],[4.85,2.50,0.72],"amethyst",pivot=[3.13,20.15,4.48],rotation=[-4,0,-5]),
    ]}

base.mantle_shell=mantle_shell
base.shrine_frame=shrine_frame
base.waistcoat=waistcoat

if __name__=='__main__':
    base.main()
