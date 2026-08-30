#!/usr/bin/env python3
"""Epic v2 Abyssal Bastion over exact official Cobblemon 1.7.3 Tyranitar geometry."""
from __future__ import annotations
import argparse,json
from pathlib import Path
from PIL import Image
import build_abyssal_bastion_tyranitar as base

C=base.cube

def crown():
    cs=[
        C([-8.5,65.0,-4.0],[17.0,1.25,7.0],'basalt'),
        C([-8.0,66.0,-3.5],[16.0,.35,6.0],'gold'),
        C([-10.3,66.0,-1.8],[2.3,9.4,3.1],'obsidian',[-9.15,70.7,-.25],[0,0,-18]),
        C([8.0,66.0,-1.8],[2.3,9.4,3.1],'obsidian',[9.15,70.7,-.25],[0,0,18]),
        C([-5.3,66.2,-1.2],[2.0,7.2,2.5],'basalt',[-4.3,69.8,.05],[0,0,-7]),
        C([3.3,66.2,-1.2],[2.0,7.2,2.5],'basalt',[4.3,69.8,.05],[0,0,7]),
        C([-1.4,66.0,1.2],[2.8,6.8,2.4],'iron'),
        C([-10.9,72.2,-1.4],[4.3,.42,2.2],'amber',[-8.75,72.4,-.3],[0,0,-18]),
        C([6.6,72.2,-1.4],[4.3,.42,2.2],'amber',[8.75,72.4,-.3],[0,0,18]),
        C([-.95,70.0,1.0],[1.9,1.9,.35],'magma',[0,70.95,1.18],[0,0,45]),
        C([-12.2,67.0,.4],[5.0,.65,1.2],'iron',[-9.7,67.3,1.0],[0,0,-27]),
        C([7.2,67.0,.4],[5.0,.65,1.2],'iron',[9.7,67.3,1.0],[0,0,27]),
    ]
    return {'name':'ouros_bastion_crown','parent':'head_rotation','pivot':[0,66,-1],'cubes':cs}

def gorget_core():
    # Open-neck V architecture: the original jaw/neck stays visually dominant.
    cs=[
        C([-9.2,41.0,-13.0],[8.7,1.7,1.15],'obsidian',[-4.85,41.85,-12.42],[0,0,-28]),
        C([.5,41.0,-13.0],[8.7,1.7,1.15],'basalt',[4.85,41.85,-12.42],[0,0,28]),
        C([-8.0,43.3,-12.7],[7.2,1.05,1.0],'iron',[-4.4,43.82,-12.2],[0,0,-24]),
        C([.8,43.3,-12.7],[7.2,1.05,1.0],'gold',[4.4,43.82,-12.2],[0,0,24]),
        C([-5.0,40.0,-13.4],[10.0,1.2,1.15],'iron'),
        C([-3.3,42.0,-13.75],[6.6,5.8,.72],'basalt'),
        C([-2.6,42.7,-14.02],[5.2,4.3,.26],'gold'),
        C([-1.65,43.6,-14.22],[3.3,2.5,.16],'amber',[0,44.85,-14.14],[0,0,45]),
        C([-.48,44.25,-14.38],[.96,1.35,.12],'magma'),
        C([-8.8,46.8,-9.8],[3.1,1.0,4.0],'obsidian',[-7.25,47.3,-7.8],[0,0,-18]),
        C([5.7,46.8,-9.8],[3.1,1.0,4.0],'basalt',[7.25,47.3,-7.8],[0,0,18]),
    ]
    return {'name':'ouros_bastion_gorget_core','parent':'chest2','pivot':[0,44,-9],'cubes':cs}

def pauldron_left():
    # Dominant siege tower shoulder.
    return {'name':'ouros_bastion_pauldron_left','parent':'chest','pivot':[17,40,0],'cubes':[
        C([9.8,35.8,-7.4],[13.8,5.2,14.8],'obsidian',[16.7,38.4,0],[0,0,14]),
        C([10.5,40.4,-6.8],[12.4,.68,13.5],'gold',[16.7,40.74,-.05],[0,0,14]),
        C([18.2,39.2,-5.3],[10.2,3.2,11.2],'basalt',[23.3,40.8,.3],[0,0,29]),
        C([20.4,41.8,-4.1],[8.2,.48,8.8],'iron',[24.5,42.04,.3],[0,0,29]),
        C([21.2,43.0,-1.9],[3.0,10.5,4.4],'obsidian',[22.7,48.25,.3],[0,0,16]),
        C([24.0,44.0,-.9],[1.25,7.4,2.5],'amber',[24.6,47.7,.35],[0,0,16]),
        C([19.0,50.6,-1.2],[8.6,1.4,3.3],'iron',[23.3,51.3,.45],[0,0,16]),
        C([21.0,52.0,-.7],[2.0,4.0,2.4],'basalt',[22,54,.5],[0,0,10]),
        C([25.0,51.1,-.7],[2.0,3.4,2.4],'basalt',[26,52.8,.5],[0,0,20]),
        C([12.0,33.9,1.7],[8.0,2.4,7.6],'sand',[16,35.1,5.5],[0,0,18]),
    ]}

def pauldron_right():
    # Lower shield-like counterweight for deliberate asymmetry.
    return {'name':'ouros_bastion_pauldron_right','parent':'chest','pivot':[-16,39.5,0],'cubes':[
        C([-21.2,36.7,-6.7],[11.0,4.5,13.2],'basalt',[-15.7,38.95,-.1],[0,0,-11]),
        C([-20.7,40.6,-6.1],[10.0,.55,12.0],'iron',[-15.7,40.88,-.1],[0,0,-11]),
        C([-26.2,37.9,-4.5],[7.2,3.2,9.3],'obsidian',[-22.6,39.5,.15],[0,0,-27]),
        C([-26.5,40.6,-3.7],[6.4,.42,7.7],'gold',[-23.3,40.81,.15],[0,0,-27]),
        C([-24.0,33.8,1.7],[8.0,2.3,7.0],'void',[-20,34.95,5.2],[0,0,-16]),
        C([-27.5,42.0,-1.6],[5.0,1.0,4.2],'iron',[-25,42.5,.5],[0,0,-18]),
        C([-26.8,42.8,-1.0],[3.8,.28,3.2],'amber',[-24.9,42.94,.6],[0,0,-18]),
    ]}

def dorsal_rampart():
    # Side towers sit outside the biological back spikes so they read in 3/4.
    cs=[
        C([-16.8,29.0,10.8],[2.2,27.0,2.4],'obsidian',[-15.7,42.5,12],[0,0,-10]),
        C([14.6,29.0,10.8],[2.2,27.0,2.4],'obsidian',[15.7,42.5,12],[0,0,10]),
        C([-14.8,32.0,11.2],[5.0,3.0,3.2],'basalt'),
        C([9.8,32.0,11.2],[5.0,3.0,3.2],'basalt'),
        C([-15.3,35.0,11.4],[4.2,10.0,2.8],'basalt'),
        C([11.1,35.0,11.4],[4.2,10.0,2.8],'basalt'),
        C([-17.0,45.0,11.2],[7.0,1.15,3.2],'gold',[-13.5,45.58,12.8],[0,0,-8]),
        C([10.0,45.0,11.2],[7.0,1.15,3.2],'gold',[13.5,45.58,12.8],[0,0,8]),
        C([-15.5,46.0,11.4],[2.4,10.5,2.8],'iron',[-14.3,51.25,12.8],[0,0,-8]),
        C([13.1,46.0,11.4],[2.4,10.5,2.8],'iron',[14.3,51.25,12.8],[0,0,8]),
        C([-12.0,29.5,13.4],[24.0,2.5,2.7],'obsidian'),
        C([-9.8,31.8,13.6],[4.0,6.0,2.3],'basalt'),
        C([-2.0,31.8,13.6],[4.0,8.0,2.3],'basalt'),
        C([5.8,31.8,13.6],[4.0,6.0,2.3],'basalt'),
        C([-10.8,39.8,13.7],[21.6,.9,2.2],'gold'),
        C([-.9,51.0,12.9],[1.8,4.0,3.4],'magma'),
        C([-.55,52.0,12.55],[1.1,2.0,.24],'amber'),
        C([-14.0,29.3,15.8],[28.0,.35,.35],'magma'),
    ]
    return {'name':'ouros_bastion_dorsal_rampart','parent':'torso','pivot':[0,41,13],'cubes':cs}

def gauntlet_left(): return base.gauntlet_left()
def gauntlet_right(): return base.gauntlet_right()
def tail_bulwark():
    b=base.tail_bulwark()
    b['cubes'].extend([
        C([-8.4,14.0,29.0],[3.4,1.1,9.0],'basalt',[-6.7,14.55,33.5],[0,0,-25]),
        C([5.0,14.0,29.0],[3.4,1.1,9.0],'basalt',[6.7,14.55,33.5],[0,0,25]),
        C([-7.8,14.8,28.7],[2.7,.3,8.2],'gold',[-6.45,14.95,32.8],[0,0,-25]),
        C([5.1,14.8,28.7],[2.7,.3,8.2],'gold',[6.45,14.95,32.8],[0,0,25]),
    ])
    return b

def build(src):
    data=json.loads(Path(src).read_text()); g=data['minecraft:geometry'][0]
    base.PIXELS=base.choose_pixels(g)
    original=len(g['bones'])
    extras=[crown(),gorget_core(),pauldron_left(),pauldron_right(),dorsal_rampart(),gauntlet_left(),gauntlet_right(),tail_bulwark()]
    g['description']['identifier']='geometry.ouros_abyssal_bastion_tyranitar'
    g['bones'].extend(extras)
    return data,original,extras

def main():
    p=argparse.ArgumentParser(); p.add_argument('--official',required=True); p.add_argument('--model-out',required=True); p.add_argument('--overlay-out',required=True); p.add_argument('--metadata-out',required=True); a=p.parse_args()
    data,n,extras=build(a.official); g=data['minecraft:geometry'][0]; w=int(g['description']['texture_width']); h=int(g['description']['texture_height'])
    Path(a.model_out).parent.mkdir(parents=True,exist_ok=True); Path(a.model_out).write_text(json.dumps(data,separators=(',',':'))+'\n')
    im=Image.new('RGBA',(w,h),(0,0,0,0))
    for k,v in base.PALETTE.items(): im.putpixel(base.PIXELS[k],v)
    Path(a.overlay_out).parent.mkdir(parents=True,exist_ok=True); im.save(a.overlay_out,optimize=True)
    meta={'format':'ouros.cobblemon-skin-build.v1','species':'cobblemon:tyranitar','concept':'Abyssal Bastion','originalBoneCount':n,'derivedBoneCount':len(g['bones']),'cosmeticBones':[x['name'] for x in extras],'cosmeticCubeCount':sum(len(x.get('cubes',[])) for x in extras),'palettePixels':base.PIXELS,'textureSize':[w,h],'artPass':'v2-epic-fortress-silhouette'}
    Path(a.metadata_out).parent.mkdir(parents=True,exist_ok=True); Path(a.metadata_out).write_text(json.dumps(meta,indent=2)+'\n'); print(json.dumps(meta,indent=2))
if __name__=='__main__': main()
