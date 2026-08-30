#!/usr/bin/env python3
"""Second artistic pass for Aura Sentinel v2.

The first structurally valid full-surface pass remained too close to normal
Lucario at gameplay scale. This module deliberately replaces only the appended
presentation groups with much larger connected macro-forms while delegating the
exact official-anatomy and texture derivation contract to build_aura_sentinel_v2.
"""
from __future__ import annotations

import importlib.util
from pathlib import Path

BASE_PATH=Path(__file__).with_name('build_aura_sentinel_v2.py')
spec=importlib.util.spec_from_file_location('aura_v2_base',BASE_PATH)
base=importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(base)


def helm_system()->dict:
    c=base.cube
    return {"name":"ouros_aura_helm_system","parent":"head_angle","pivot":[0,38.5,-2.0],"cubes":[
        # Deep rear cowl and broad temple shells read as one helmet mass.
        c([-5.65,34.8,2.15],[11.3,6.1,1.35],"void"),
        c([-6.15,34.6,-1.95],[1.45,5.9,4.55],"indigo"),c([4.70,34.6,-1.95],[1.45,5.9,4.55],"indigo"),
        c([-6.45,34.15,-3.65],[1.20,3.55,2.65],"amethyst",pivot=[-5.85,35.92,-2.32],rotation=[0,0,-9]),
        c([5.25,34.15,-3.65],[1.20,3.55,2.65],"cobalt",pivot=[5.85,35.92,-2.32],rotation=[0,0,9]),
        # Continuous open-face visor/brow. The biological muzzle stays uncovered.
        c([-5.05,36.75,-5.05],[10.10,1.55,0.30],"silver"),
        c([-5.35,38.22,-5.14],[10.70,0.72,0.40],"gold"),
        c([-4.78,36.98,-5.30],[9.56,0.82,0.18],"aura"),
        c([-0.50,35.75,-5.22],[1.00,1.20,0.24],"gold"),
        # Jawline guards connect visor to cowl without replacing the muzzle.
        c([-6.05,33.72,-4.16],[1.10,2.85,3.32],"void",pivot=[-5.50,35.15,-2.50],rotation=[0,0,-12]),
        c([4.95,33.72,-4.16],[1.10,2.85,3.32],"void",pivot=[5.50,35.15,-2.50],rotation=[0,0,12]),
        c([-6.18,33.96,-4.30],[0.26,2.18,2.78],"aura",pivot=[-6.05,35.05,-2.91],rotation=[0,0,-12]),
        c([5.92,33.96,-4.30],[0.26,2.18,2.78],"aura",pivot=[6.05,35.05,-2.91],rotation=[0,0,12]),
        # Huge asymmetric temple crests stay outside the official sensors/ears.
        c([-9.05,39.10,-2.30],[4.55,0.90,1.90],"cobalt",pivot=[-6.78,39.55,-1.35],rotation=[0,0,-31]),
        c([-8.50,40.00,-2.08],[3.55,0.35,1.46],"aura",pivot=[-6.73,40.18,-1.35],rotation=[0,0,-31]),
        c([4.18,39.28,-2.05],[3.35,0.70,1.58],"amethyst",pivot=[5.86,39.63,-1.26],rotation=[0,0,24]),
        c([4.60,40.00,-1.92],[2.55,0.30,1.26],"gold",pivot=[5.88,40.15,-1.29],rotation=[0,0,24]),
        # Rear crown bridge makes the helmet obvious from back/side views.
        c([-5.10,40.15,1.85],[10.20,0.62,1.38],"indigo"),
        c([-3.80,40.75,2.00],[7.60,0.28,1.05],"aura"),
    ]}


def mantle_shell()->dict:
    c=base.cube
    return {"name":"ouros_aura_mantle_shell","parent":"torso3","pivot":[0,29.5,0.0],"cubes":[
        # One broad yoke spanning far beyond the biological shoulders.
        c([-8.80,28.85,-3.35],[17.60,2.10,6.85],"void"),
        c([-11.35,27.85,-2.90],[5.30,3.45,5.90],"indigo",pivot=[-8.70,29.58,0.05],rotation=[0,0,-13]),
        c([6.05,28.15,-2.70],[4.40,2.85,5.55],"cobalt",pivot=[8.25,29.58,0.08],rotation=[0,0,10]),
        c([-11.65,30.95,-2.55],[5.60,0.48,5.25],"silver",pivot=[-8.85,31.19,0.08],rotation=[0,0,-13]),
        c([5.95,30.78,-2.42],[4.70,0.44,5.05],"gold",pivot=[8.30,31.00,0.10],rotation=[0,0,10]),
        # Tall left ceremonial pauldron-fin becomes a primary 3/4 signature.
        c([-11.20,30.10,-1.10],[1.25,6.10,2.35],"amethyst",pivot=[-10.58,33.15,0.08],rotation=[0,0,-11]),
        c([-10.95,35.85,-0.85],[0.72,3.30,1.85],"cobalt",pivot=[-10.59,37.50,0.08],rotation=[0,0,-18]),
        c([-10.83,36.35,-0.72],[0.28,2.55,1.55],"aura",pivot=[-10.69,37.63,0.06],rotation=[0,0,-18]),
        # Right shoulder has a lower blade for deliberate asymmetry.
        c([9.15,29.65,-0.75],[3.25,0.62,1.95],"indigo",pivot=[10.78,29.96,0.23],rotation=[0,0,32]),
        c([9.48,30.28,-0.62],[2.45,0.26,1.55],"aura",pivot=[10.70,30.41,0.16],rotation=[0,0,32]),
        # High rear collar visually joins helmet and shrine frame.
        c([-6.20,30.15,2.90],[12.40,3.30,1.38],"amethyst"),
        c([-5.15,33.05,3.12],[10.30,0.55,1.02],"gold"),
        c([-4.45,33.55,3.20],[8.90,0.28,0.86],"aura"),
    ]}


def breastplate()->dict:
    c=base.cube
    return {"name":"ouros_aura_breastplate","parent":"torso3","pivot":[0,27.8,-3.0],"cubes":[
        # Full shallow breastplate rather than a small chest frame.
        c([-4.65,24.55,-4.20],[9.30,6.15,0.62],"indigo"),
        c([-4.95,24.40,-3.62],[0.82,6.35,6.35],"void"),c([4.13,24.40,-3.62],[0.82,6.35,6.35],"void"),
        c([-4.10,30.18,-4.42],[8.20,0.68,0.38],"silver"),
        c([-4.10,24.32,-4.40],[8.20,0.62,0.38],"gold"),
        c([-3.80,25.05,-4.48],[0.82,4.75,0.28],"aura"),c([2.98,25.05,-4.48],[0.82,4.75,0.28],"aura"),
        # Central open aura diamond leaves biological chest geometry underneath readable.
        c([-2.00,26.00,-4.65],[4.00,4.00,0.42],"void",pivot=[0,28.00,-4.44],rotation=[0,0,45]),
        c([-1.34,26.66,-4.96],[2.68,2.68,0.26],"aura",pivot=[0,28.00,-4.83],rotation=[0,0,45]),
        c([-4.75,27.45,-4.36],[1.25,0.50,0.38],"gold",pivot=[-4.13,27.70,-4.17],rotation=[0,0,-16]),
        c([3.50,27.45,-4.36],[1.25,0.50,0.38],"silver",pivot=[4.13,27.70,-4.17],rotation=[0,0,16]),
    ]}


def shrine_frame()->dict:
    c=base.cube
    return {"name":"ouros_aura_shrine_frame","parent":"torso3","pivot":[0,32.0,3.5],"cubes":[
        # Monumental open shrine arch. It frames the head instead of hiding it.
        c([-9.10,20.80,3.05],[1.15,16.00,1.25],"void",pivot=[-8.53,28.80,3.68],rotation=[-3,0,-6]),
        c([7.95,20.80,3.05],[1.15,16.00,1.25],"void",pivot=[8.53,28.80,3.68],rotation=[-3,0,6]),
        c([-8.55,36.15,2.90],[17.10,1.10,1.55],"indigo"),
        c([-7.65,37.18,3.00],[15.30,0.42,1.28],"gold"),
        c([-6.80,37.62,3.10],[13.60,0.30,1.02],"aura"),
        # Three stepped halo blades create a recognizable crown silhouette.
        c([-10.90,34.25,3.10],[4.30,0.72,1.48],"cobalt",pivot=[-8.75,34.61,3.84],rotation=[0,0,-35]),
        c([6.60,34.40,3.10],[3.95,0.68,1.42],"amethyst",pivot=[8.58,34.74,3.81],rotation=[0,0,32]),
        c([-12.65,31.75,3.18],[4.65,0.65,1.38],"amethyst",pivot=[-10.33,32.08,3.87],rotation=[0,0,-48]),
        c([8.00,32.05,3.18],[4.25,0.60,1.34],"cobalt",pivot=[10.13,32.35,3.85],rotation=[0,0,45]),
        c([-10.35,34.92,3.28],[3.30,0.28,1.10],"aura",pivot=[-8.70,35.06,3.83],rotation=[0,0,-35]),
        c([7.05,35.02,3.28],[3.00,0.26,1.05],"aura",pivot=[8.55,35.15,3.80],rotation=[0,0,32]),
        # Central aura reliquary above the head.
        c([-1.55,37.65,3.18],[3.10,3.10,0.44],"void",pivot=[0,39.20,3.40],rotation=[0,0,45]),
        c([-0.96,38.24,3.50],[1.92,1.92,0.24],"aura",pivot=[0,39.20,3.62],rotation=[0,0,45]),
        # Heavy lower bridge ties the arch into the torso equipment.
        c([-8.55,20.25,3.20],[17.10,0.82,1.05],"silver"),
        c([-5.50,20.08,4.18],[11.00,0.36,0.28],"gold"),
    ]}


def waistcoat()->dict:
    c=base.cube
    return {"name":"ouros_aura_waistcoat","parent":"torso","pivot":[0,20.6,1.0],"cubes":[
        c([-6.45,19.65,-4.10],[12.90,1.45,7.95],"void"),
        c([-6.65,20.98,-4.18],[13.30,0.42,8.12],"gold"),
        # Long rear split coat and broad side panels create a complete costume mass.
        c([-6.10,10.65,3.35],[5.45,10.25,0.88],"indigo",pivot=[-3.38,20.00,3.79],rotation=[-6,0,8]),
        c([0.55,12.05,3.40],[5.10,8.85,0.88],"amethyst",pivot=[3.10,20.05,3.84],rotation=[-6,0,-7]),
        c([-6.70,12.85,-3.98],[2.45,7.60,0.52],"cobalt",pivot=[-5.48,19.50,-3.72],rotation=[0,0,-5]),
        c([4.25,13.65,-3.98],[2.45,6.80,0.52],"indigo",pivot=[5.48,19.60,-3.72],rotation=[0,0,5]),
        c([-5.78,10.60,4.20],[4.78,0.52,0.28],"silver",pivot=[-3.39,10.86,4.34],rotation=[-6,0,8]),
        c([0.90,12.00,4.25],[4.40,0.48,0.28],"gold",pivot=[3.10,12.24,4.39],rotation=[-6,0,-7]),
        c([-6.85,18.85,-3.92],[2.65,0.50,0.30],"aura",pivot=[-5.52,19.10,-3.77],rotation=[0,0,-5]),
        c([4.20,18.85,-3.92],[2.65,0.50,0.30],"aura",pivot=[5.52,19.10,-3.77],rotation=[0,0,5]),
    ]}


def relic_fin()->dict:
    c=base.cube
    return {"name":"ouros_aura_relic_fin","parent":"torso3","pivot":[-8.0,31.0,4.0],"cubes":[
        # One large asymmetric ceremonial wing/banner built from connected stepped plates.
        c([-15.10,25.50,3.55],[7.80,1.00,1.40],"amethyst",pivot=[-11.20,26.00,4.25],rotation=[0,0,-42]),
        c([-14.20,28.15,3.58],[7.10,0.90,1.35],"cobalt",pivot=[-10.65,28.60,4.26],rotation=[0,0,-36]),
        c([-13.10,30.65,3.62],[6.15,0.82,1.30],"indigo",pivot=[-10.03,31.06,4.27],rotation=[0,0,-29]),
        c([-12.00,32.85,3.66],[5.10,0.72,1.22],"gold",pivot=[-9.45,33.21,4.27],rotation=[0,0,-22]),
        c([-14.55,26.30,3.85],[6.55,0.30,0.94],"aura",pivot=[-11.28,26.45,4.32],rotation=[0,0,-42]),
        c([-13.65,28.90,3.88],[5.90,0.28,0.90],"aura",pivot=[-10.70,29.04,4.33],rotation=[0,0,-36]),
        c([-12.60,31.36,3.90],[5.05,0.26,0.86],"aura",pivot=[-10.08,31.49,4.33],rotation=[0,0,-29]),
        c([-9.10,22.20,3.60],[0.90,12.80,1.30],"void",pivot=[-8.65,28.60,4.25],rotation=[0,0,-6]),
        c([-9.00,23.10,4.85],[0.64,10.70,0.24],"gold",pivot=[-8.68,28.45,4.97],rotation=[0,0,-6]),
    ]}


# Replace only cosmetic macro-form factories. Exact source derivation stays in base.
base.helm_system=helm_system
base.mantle_shell=mantle_shell
base.breastplate=breastplate
base.shrine_frame=shrine_frame
base.waistcoat=waistcoat
base.relic_fin=relic_fin

if __name__=='__main__':
    base.main()
