#!/usr/bin/env python3
"""Build Omen Regent around exact official Cobblemon 1.7.3 Absol geometry."""
from __future__ import annotations
import argparse, json
from pathlib import Path
from PIL import Image

PALETTE={
    'obsidian':(26,24,34,255),'midnight':(42,38,64,255),'silver':(174,181,198,255),'bone':(221,216,195,255),
    'crimson':(150,45,64,255),'violet':(104,70,154,255),'aura':(174,112,235,190),'gold':(211,164,72,255)
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

def suv(mat):
    x,y=PIXELS[mat]; return {f:{'uv':[x,y],'uv_size':[1,1]} for f in FACES}

def cube(origin,size,mat,pivot=None,rotation=None):
    d={'origin':origin,'size':size,'uv':suv(mat)}
    if pivot is not None:d['pivot']=pivot
    if rotation is not None:d['rotation']=rotation
    return d

def omen_crown():
    # Opposite the official sickle horn: an open broken eclipse that amplifies, never replaces, the head silhouette.
    cs=[]
    segs=[(4.1,31.8,-24,5.2),(6.6,34.2,-48,5.0),(7.5,37.2,-76,4.6),(6.3,39.6,-108,4.2)]
    for x,y,r,l in segs:
        cs += [cube([x,y,-8.2],[l,0.72,0.92],'bone',[x+l/2,y+0.36,-7.74],[0,0,r]),
               cube([x+0.25,y+0.68,-8.42],[max(2.4,l-0.5),0.18,0.22],'violet',[x+l/2,y+0.77,-8.31],[0,0,r])]
    cs += [cube([2.8,30.3,-9.0],[1.5,1.5,1.0],'gold',[3.55,31.05,-8.5],[0,0,45]),
           cube([3.15,30.65,-9.18],[0.8,0.8,0.20],'aura',[3.55,31.05,-9.08],[0,0,45])]
    return {'name':'ouros_omen_crown','parent':'head_angle','pivot':[4.0,32.0,-8.0],'cubes':cs}

def gorget():
    return {'name':'ouros_omen_gorget','parent':'neck','pivot':[0,22.5,-5.0],'cubes':[
        cube([-5.3,20.0,-10.1],[1.25,5.5,6.4],'obsidian',[-4.68,22.75,-6.9],[0,0,-8]),
        cube([4.05,20.0,-10.1],[1.25,5.5,6.4],'midnight',[4.68,22.75,-6.9],[0,0,8]),
        cube([-4.4,19.5,-10.45],[8.8,1.0,1.0],'silver'),
        cube([-2.0,20.25,-10.7],[4.0,2.35,0.52],'gold'),
        cube([-1.25,20.7,-10.88],[2.5,1.45,0.20],'aura'),
        cube([-0.28,20.92,-11.0],[0.56,1.02,0.16],'crimson',[0,21.43,-10.92],[0,0,45])
    ]}

def pauldron_left():
    return {'name':'ouros_omen_pauldron_left','parent':'chest','pivot':[5.2,18.8,-4.0],'cubes':[
        cube([4.1,17.5,-9.0],[4.8,1.6,7.2],'obsidian',[6.5,18.3,-5.4],[0,0,10]),
        cube([4.35,18.95,-8.7],[4.4,0.28,6.6],'silver',[6.55,19.09,-5.4],[0,0,10]),
        cube([7.9,18.4,-8.1],[2.6,0.55,5.2],'bone',[9.2,18.68,-5.5],[0,0,28]),
        cube([8.55,18.75,-7.3],[1.15,0.22,3.7],'violet',[9.13,18.86,-5.45],[0,0,28])
    ]}

def pauldron_right():
    return {'name':'ouros_omen_pauldron_right','parent':'chest','pivot':[-5.2,18.8,-4.0],'cubes':[
        cube([-8.4,17.7,-8.6],[4.2,1.35,6.4],'midnight',[-6.3,18.38,-5.4],[0,0,-8]),
        cube([-8.2,18.9,-8.35],[3.8,0.26,5.9],'gold',[-6.3,19.03,-5.4],[0,0,-8]),
        cube([-9.1,16.8,-7.4],[1.0,3.2,3.7],'crimson',[-8.6,18.4,-5.55],[0,0,-12])
    ]}

def back_frame():
    # Tall unequal omen standards behind the shoulder line create the principal 3/4/rear identity.
    return {'name':'ouros_omen_back_frame','parent':'torso','pivot':[0,22,8.0],'cubes':[
        cube([-5.8,17.0,8.5],[0.55,15.5,0.55],'silver',[-5.53,24.75,8.78],[0,0,-8]),
        cube([5.25,18.5,8.5],[0.55,12.5,0.55],'silver',[5.53,24.75,8.78],[0,0,7]),
        cube([-9.5,25.1,8.75],[4.4,5.7,0.48],'obsidian',[-7.3,27.95,8.99],[0,0,8]),
        cube([5.4,24.0,8.75],[4.0,4.8,0.48],'midnight',[7.4,26.4,8.99],[0,0,-7]),
        cube([-8.9,29.6,8.55],[3.0,0.26,0.88],'bone',[-7.4,29.73,8.99],[0,0,8]),
        cube([5.9,27.7,8.55],[2.8,0.26,0.88],'gold',[7.3,27.83,8.99],[0,0,-7]),
        cube([-7.9,26.7,8.45],[1.25,1.25,0.20],'aura',[-7.28,27.33,8.55],[0,0,45])
    ]}

def split_mantle():
    return {'name':'ouros_omen_split_mantle','parent':'torso','pivot':[0,17.5,7.5],'cubes':[
        cube([-6.3,14.0,8.0],[5.4,8.6,0.55],'obsidian',[-3.6,20.0,8.28],[-9,0,9]),
        cube([0.9,14.0,8.0],[5.4,8.6,0.55],'midnight',[3.6,20.0,8.28],[-9,0,-9]),
        cube([-6.0,14.15,8.5],[4.8,0.28,0.20],'silver',[-3.6,14.29,8.6],[-9,0,9]),
        cube([1.2,14.15,8.5],[4.8,0.28,0.20],'crimson',[3.6,14.29,8.6],[-9,0,-9]),
        cube([-5.2,12.2,8.35],[2.3,3.5,0.32],'violet',[-4.05,14.0,8.51],[-9,0,9]),
        cube([2.9,12.2,8.35],[2.3,3.5,0.32],'obsidian',[4.05,14.0,8.51],[-9,0,-9])
    ]}

def tail_reliquary():
    return {'name':'ouros_omen_tail_reliquary','parent':'tail','pivot':[0,22.0,12.0],'cubes':[
        cube([-2.2,20.7,11.0],[4.4,2.6,2.6],'obsidian'),
        cube([-1.75,21.15,10.72],[3.5,1.7,0.30],'gold'),
        cube([-0.6,21.4,10.50],[1.2,1.2,0.18],'aura',[0,22.0,10.59],[0,0,45]),
        cube([-3.0,20.1,12.4],[0.45,4.6,0.45],'silver',[-2.78,22.4,12.63],[0,0,-18]),
        cube([2.55,20.1,12.4],[0.45,4.6,0.45],'silver',[2.78,22.4,12.63],[0,0,18])
    ]}

def omen_blade():
    # A single asymmetric ceremonial blade, deliberately offset from Absol's natural horn.
    return {'name':'ouros_omen_blade','parent':'torso','pivot':[-6.5,22.0,7.5],'cubes':[
        cube([-8.1,15.0,7.0],[0.65,17.0,0.65],'silver',[-7.78,23.5,7.33],[0,0,-18]),
        cube([-12.4,28.0,6.95],[7.2,1.1,0.85],'bone',[-8.8,28.55,7.38],[0,0,-34]),
        cube([-13.6,30.0,6.95],[6.0,0.9,0.85],'bone',[-10.6,30.45,7.38],[0,0,-56]),
        cube([-12.9,31.6,6.78],[4.6,0.22,0.22],'crimson',[-10.6,31.71,6.89],[0,0,-56]),
        cube([-8.7,24.0,6.7],[1.8,1.8,1.1],'gold',[-7.8,24.9,7.25],[0,0,45]),
        cube([-8.25,24.45,6.48],[0.9,0.9,0.20],'aura',[-7.8,24.9,6.58],[0,0,45])
    ]}

def build(src):
    global PIXELS
    data=json.loads(Path(src).read_text()); g=data['minecraft:geometry'][0]; PIXELS=choose_pixels(g)
    original=len(g['bones']); extras=[omen_crown(),gorget(),pauldron_left(),pauldron_right(),back_frame(),split_mantle(),tail_reliquary(),omen_blade()]
    g['description']['identifier']='geometry.ouros_omen_regent_absol'; g['bones'].extend(extras)
    return data,original,extras

def main():
    p=argparse.ArgumentParser(); p.add_argument('--official',required=True); p.add_argument('--model-out',required=True); p.add_argument('--overlay-out',required=True); p.add_argument('--metadata-out',required=True); a=p.parse_args()
    data,n,extras=build(a.official); g=data['minecraft:geometry'][0]; w=int(g['description']['texture_width']); h=int(g['description']['texture_height'])
    Path(a.model_out).parent.mkdir(parents=True,exist_ok=True); Path(a.model_out).write_text(json.dumps(data,separators=(',',':'))+'\n')
    im=Image.new('RGBA',(w,h),(0,0,0,0))
    for k,rgba in PALETTE.items(): im.putpixel(PIXELS[k],rgba)
    Path(a.overlay_out).parent.mkdir(parents=True,exist_ok=True); im.save(a.overlay_out,optimize=True)
    meta={'format':'ouros.cobblemon-skin-build.v1','species':'cobblemon:absol','concept':'Omen Regent','originalBoneCount':n,'derivedBoneCount':len(g['bones']),'cosmeticBones':[x['name'] for x in extras],'cosmeticCubeCount':sum(len(x.get('cubes',[])) for x in extras),'palettePixels':PIXELS,'textureSize':[w,h]}
    Path(a.metadata_out).parent.mkdir(parents=True,exist_ok=True); Path(a.metadata_out).write_text(json.dumps(meta,indent=2)+'\n')
    print(json.dumps(meta,indent=2))
if __name__=='__main__': main()
