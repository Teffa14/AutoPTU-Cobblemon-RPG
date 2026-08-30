#!/usr/bin/env python3
"""Build Abyssal Bastion over exact official Cobblemon 1.7.3 Tyranitar geometry."""
from __future__ import annotations
import argparse, json
from pathlib import Path
from PIL import Image

PALETTE={
    'basalt':(42,45,48,255),
    'obsidian':(25,22,34,255),
    'iron':(139,146,151,255),
    'gold':(201,156,63,255),
    'magma':(212,72,38,255),
    'amber':(246,171,62,230),
    'sand':(188,166,125,255),
    'void':(92,67,126,255),
}
FACES=('north','east','south','west','up','down')
PIXELS={}

def mark_uv_usage(g):
    w=int(g['description']['texture_width']); h=int(g['description']['texture_height']); used=set()
    def mark(x,y,ww,hh):
        x0,x1=sorted((int(x),int(x+ww))); y0,y1=sorted((int(y),int(y+hh)))
        for yy in range(max(0,y0),min(h,y1)):
            for xx in range(max(0,x0),min(w,x1)): used.add((xx,yy))
    for b in g['bones']:
        for c in b.get('cubes',[]):
            dx,dy,dz=c.get('size',[0,0,0]); uv=c.get('uv',[0,0])
            if isinstance(uv,list):
                u,v=uv
                for r in ((u+dz,v,dx,dz),(u+dz+dx,v,dx,dz),(u,v+dz,dz,dy),(u+dz,v+dz,dx,dy),(u+dz+dx,v+dz,dz,dy),(u+2*dz+dx,v+dz,dx,dy)): mark(*r)
            else:
                for f in uv.values():
                    if isinstance(f,dict):
                        p=f.get('uv',[0,0]); e=f.get('uv_size',[1,1]); mark(p[0],p[1],e[0],e[1])
    return used

def choose_pixels(g):
    w=int(g['description']['texture_width']); h=int(g['description']['texture_height']); used=mark_uv_usage(g)
    free=[(x,y) for y in range(h-1,-1,-1) for x in range(w) if (x,y) not in used]
    if len(free)<len(PALETTE): raise RuntimeError('insufficient free UV texels')
    return {k:free[i] for i,k in enumerate(PALETTE)}

def suv(m):
    x,y=PIXELS[m]; return {f:{'uv':[x,y],'uv_size':[1,1]} for f in FACES}

def cube(o,s,m,p=None,r=None):
    d={'origin':o,'size':s,'uv':suv(m)}
    if p is not None:d['pivot']=p
    if r is not None:d['rotation']=r
    return d

def crown():
    cs=[
        cube([-8.5,65.0,-4.0],[17.0,1.4,7.0],'basalt'),
        cube([-8.1,66.2,-3.6],[16.2,.35,6.2],'gold'),
        cube([-8.8,66.0,-2.0],[2.2,8.0,3.0],'obsidian',[-7.7,70.0,-.5],[0,0,-10]),
        cube([6.6,66.0,-2.0],[2.2,8.0,3.0],'obsidian',[7.7,70.0,-.5],[0,0,10]),
        cube([-4.7,66.2,-1.4],[2.0,6.4,2.4],'basalt',[-3.7,69.4,-.2],[0,0,-4]),
        cube([2.7,66.2,-1.4],[2.0,6.4,2.4],'basalt',[3.7,69.4,-.2],[0,0,4]),
        cube([-1.4,66.1,1.2],[2.8,5.8,2.3],'iron'),
        cube([-7.2,70.8,-2.6],[3.6,.34,2.2],'amber',[-5.4,70.97,-1.5],[0,0,-8]),
        cube([3.6,70.8,-2.6],[3.6,.34,2.2],'amber',[5.4,70.97,-1.5],[0,0,8]),
        cube([-.95,69.4,1.0],[1.9,1.9,.35],'magma',[0,70.35,1.18],[0,0,45]),
    ]
    return {'name':'ouros_bastion_crown','parent':'head_rotation','pivot':[0,66,-1],'cubes':cs}

def gorget_core():
    cs=[
        cube([-10.7,41.5,-12.9],[3.0,11.5,9.5],'obsidian',[-9.2,47.25,-8.15],[0,0,-12]),
        cube([7.7,41.5,-12.9],[3.0,11.5,9.5],'basalt',[9.2,47.25,-8.15],[0,0,12]),
        cube([-8.8,41.2,-13.4],[17.6,1.5,1.2],'iron'),
        cube([-7.5,49.9,-11.8],[15.0,1.15,3.2],'gold'),
        cube([-4.2,43.2,-13.9],[8.4,7.2,.9],'basalt'),
        cube([-3.2,44.1,-14.15],[6.4,5.3,.32],'gold'),
        cube([-1.8,45.2,-14.38],[3.6,3.0,.18],'amber',[0,46.7,-14.29],[0,0,45]),
        cube([-.55,45.9,-14.55],[1.1,1.8,.14],'magma'),
    ]
    return {'name':'ouros_bastion_gorget_core','parent':'chest2','pivot':[0,47,-8],'cubes':cs}

def pauldron_left():
    cs=[
        cube([10.2,36.5,-7.3],[12.6,5.4,14.5],'obsidian',[16.5,39.2,-.05],[0,0,13]),
        cube([10.8,41.1,-6.7],[11.6,.65,13.3],'gold',[16.6,41.43,-.05],[0,0,13]),
        cube([18.6,40.0,-5.5],[9.0,3.0,11.0],'basalt',[23.1,41.5,0],[0,0,27]),
        cube([20.5,42.3,-4.2],[7.6,.45,8.5],'iron',[24.3,42.53,.05],[0,0,27]),
        cube([21.8,44.0,-1.8],[2.4,7.2,4.2],'obsidian',[23,47.6,.3],[0,0,18]),
        cube([24.1,43.0,-.9],[1.1,5.6,2.4],'amber',[24.65,45.8,.3],[0,0,18]),
        cube([12.2,34.6,1.8],[7.2,2.2,7.2],'sand',[15.8,35.7,5.4],[0,0,18]),
    ]
    return {'name':'ouros_bastion_pauldron_left','parent':'chest','pivot':[16.5,39.5,0],'cubes':cs}

def pauldron_right():
    cs=[
        cube([-21.0,37.0,-6.7],[10.8,4.7,13.0],'basalt',[-15.6,39.35,-.2],[0,0,-10]),
        cube([-20.5,41.0,-6.1],[9.8,.55,11.8],'iron',[-15.6,41.28,-.2],[0,0,-10]),
        cube([-25.2,38.6,-4.4],[6.5,3.0,9.0],'obsidian',[-21.95,40.1,.1],[0,0,-24]),
        cube([-25.6,41.1,-3.6],[5.9,.4,7.4],'gold',[-22.65,41.3,.1],[0,0,-24]),
        cube([-22.7,34.7,2.2],[7.2,2.0,6.5],'void',[-19.1,35.7,5.45],[0,0,-15]),
    ]
    return {'name':'ouros_bastion_pauldron_right','parent':'chest','pivot':[-15.5,39.5,0],'cubes':cs}

def dorsal_rampart():
    cs=[
        cube([-11.0,28.0,13.0],[2.0,27.0,2.2],'obsidian',[-10,41.5,14.1],[0,0,-7]),
        cube([9.0,28.0,13.0],[2.0,27.0,2.2],'obsidian',[10,41.5,14.1],[0,0,7]),
        cube([-8.8,31.0,13.4],[17.6,3.0,2.8],'basalt'),
        cube([-8.4,34.0,13.6],[3.6,8.0,2.4],'basalt'),
        cube([-1.8,34.0,13.6],[3.6,10.0,2.4],'basalt'),
        cube([4.8,34.0,13.6],[3.6,8.0,2.4],'basalt'),
        cube([-8.2,42.0,13.7],[16.4,1.0,2.1],'gold'),
        cube([-6.8,45.0,13.6],[2.1,8.0,2.2],'iron',[-5.75,49,14.7],[0,0,-8]),
        cube([4.7,45.0,13.6],[2.1,8.0,2.2],'iron',[5.75,49,14.7],[0,0,8]),
        cube([-1.6,44.0,13.4],[3.2,12.0,2.7],'obsidian'),
        cube([-.8,51.2,13.0],[1.6,3.0,3.5],'amber'),
        cube([-7.8,30.2,15.6],[15.6,.35,.35],'magma'),
    ]
    return {'name':'ouros_bastion_dorsal_rampart','parent':'torso','pivot':[0,40,14],'cubes':cs}

def gauntlet_left():
    return {'name':'ouros_bastion_gauntlet_left','parent':'arm_left2','pivot':[23,38,1],'cubes':[
        cube([18.2,33.0,-4.0],[9.5,8.8,9.0],'basalt',[22.95,37.4,.5],[0,0,-4]),
        cube([19.0,33.8,-4.35],[8.0,7.2,.45],'gold',[23,37.4,-4.12],[0,0,-4]),
        cube([21.0,40.5,-3.1],[6.8,1.6,7.0],'iron',[24.4,41.3,.4],[0,0,-8]),
        cube([26.4,35.5,-2.0],[3.8,2.0,5.2],'obsidian',[28.3,36.5,.6],[0,0,-18]),
        cube([27.4,36.0,-2.3],[2.1,.35,5.0],'amber',[28.45,36.18,.2],[0,0,-18]),
    ]}

def gauntlet_right():
    return {'name':'ouros_bastion_gauntlet_right','parent':'arm_right2','pivot':[-23,38,1],'cubes':[
        cube([-27.7,33.2,-3.7],[9.5,8.3,8.6],'obsidian',[-22.95,37.35,.6],[0,0,4]),
        cube([-27.0,33.9,-4.05],[8.0,6.8,.42],'iron',[-23,37.3,-3.84],[0,0,4]),
        cube([-28.0,40.2,-2.9],[6.8,1.5,6.8],'gold',[-24.6,40.95,.5],[0,0,8]),
        cube([-30.0,35.4,-1.8],[3.6,1.8,5.0],'basalt',[-28.2,36.3,.7],[0,0,18]),
    ]}

def tail_bulwark():
    cs=[
        cube([-5.8,11.0,27.5],[11.6,9.0,8.0],'obsidian'),
        cube([-5.2,11.7,27.1],[10.4,7.5,.5],'gold'),
        cube([-3.4,13.0,26.75],[6.8,4.8,.25],'amber'),
        cube([-6.8,18.7,30.0],[4.2,2.0,7.0],'basalt',[-4.7,19.7,33.5],[0,0,-18]),
        cube([2.6,18.7,30.0],[4.2,2.0,7.0],'basalt',[4.7,19.7,33.5],[0,0,18]),
        cube([-7.1,9.0,31.0],[2.2,6.2,5.0],'iron',[-6,12.1,33.5],[0,0,-16]),
        cube([4.9,9.0,31.0],[2.2,6.2,5.0],'iron',[6,12.1,33.5],[0,0,16]),
        cube([-1.0,19.3,33.0],[2.0,7.0,2.0],'magma'),
        cube([-.6,22.0,32.6],[1.2,2.6,2.8],'amber'),
    ]
    return {'name':'ouros_bastion_tail_bulwark','parent':'tail3','pivot':[0,15.5,30],'cubes':cs}

def build(src):
    global PIXELS
    data=json.loads(Path(src).read_text()); g=data['minecraft:geometry'][0]; PIXELS=choose_pixels(g)
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
    for k,v in PALETTE.items(): im.putpixel(PIXELS[k],v)
    Path(a.overlay_out).parent.mkdir(parents=True,exist_ok=True); im.save(a.overlay_out,optimize=True)
    meta={'format':'ouros.cobblemon-skin-build.v1','species':'cobblemon:tyranitar','concept':'Abyssal Bastion','originalBoneCount':n,'derivedBoneCount':len(g['bones']),'cosmeticBones':[x['name'] for x in extras],'cosmeticCubeCount':sum(len(x.get('cubes',[])) for x in extras),'palettePixels':PIXELS,'textureSize':[w,h],'artPass':'v1-epic-silhouette'}
    Path(a.metadata_out).parent.mkdir(parents=True,exist_ok=True); Path(a.metadata_out).write_text(json.dumps(meta,indent=2)+'\n'); print(json.dumps(meta,indent=2))
if __name__=='__main__': main()
