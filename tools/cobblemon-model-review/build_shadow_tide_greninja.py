#!/usr/bin/env python3
"""Build Shadow Tide around exact official Cobblemon Greninja and Ash-Greninja geometry."""
from __future__ import annotations
import argparse, copy, json
from pathlib import Path
from PIL import Image

PALETTE={
    'abyss':(18,25,39,255),'indigo':(41,48,92,255),'steel':(94,119,137,255),'silver':(185,205,214,255),
    'cyan':(64,214,239,225),'glass':(105,226,255,155),'violet':(109,66,151,255),'foam':(207,242,244,255)
}
FACES=('north','east','south','west','up','down')
PIXELS={}

def mark_uv_usage(g):
    w=int(g['description']['texture_width']); h=int(g['description']['texture_height']); used=set()
    def mark(x,y,ww,hh):
        x0,x1=sorted((int(x),int(x+ww))); y0,y1=sorted((int(y),int(y+hh)))
        for yy in range(max(0,y0),min(h,y1)):
            for xx in range(max(0,x0),min(w,x1)): used.add((xx,yy))
    for b in g.get('bones',[]):
        for c in b.get('cubes',[]):
            dx,dy,dz=c.get('size',[0,0,0]); uv=c.get('uv',[0,0])
            if isinstance(uv,list):
                u,v=uv
                for r in ((u+dz,v,dx,dz),(u+dz+dx,v,dx,dz),(u,v+dz,dz,dy),(u+dz,v+dz,dx,dy),(u+dz+dx,v+dz,dz,dy),(u+2*dz+dx,v+dz,dx,dy)): mark(*r)
            elif isinstance(uv,dict):
                for f in uv.values():
                    if isinstance(f,dict):
                        p=f.get('uv',[0,0]); e=f.get('uv_size',[1,1]); mark(p[0],p[1],e[0],e[1])
    return used

def choose_pixels(*geos):
    w=int(geos[0]['description']['texture_width']); h=int(geos[0]['description']['texture_height'])
    assert all(int(g['description']['texture_width'])==w and int(g['description']['texture_height'])==h for g in geos)
    used=set().union(*(mark_uv_usage(g) for g in geos))
    free=[(x,y) for y in range(h-1,-1,-1) for x in range(w) if (x,y) not in used]
    if len(free)<len(PALETTE): raise RuntimeError('insufficient shared free UV texels')
    return {k:free[i] for i,k in enumerate(PALETTE)}

def suv(mat):
    x,y=PIXELS[mat]; return {f:{'uv':[x,y],'uv_size':[1,1]} for f in FACES}

def cube(origin,size,mat,pivot=None,rotation=None):
    d={'origin':origin,'size':size,'uv':suv(mat)}
    if pivot is not None:d['pivot']=pivot
    if rotation is not None:d['rotation']=rotation
    return d

def cowl():
    # Open rear cowl and broken crescent blades frame the head without masking eyes, fins, jaw, or tongue.
    return {'name':'ouros_shadow_tide_cowl','parent':'head','pivot':[0,29.5,2.0],'cubes':[
        cube([-5.3,28.0,2.7],[1.15,4.9,1.0],'abyss',[-4.7,30.4,3.2],[0,0,-13]),
        cube([4.15,28.0,2.7],[1.15,4.9,1.0],'abyss',[4.7,30.4,3.2],[0,0,13]),
        cube([-6.5,31.6,2.5],[3.1,0.55,1.15],'indigo',[-4.95,31.9,3.1],[0,0,-24]),
        cube([3.4,31.6,2.5],[3.1,0.55,1.15],'indigo',[4.95,31.9,3.1],[0,0,24]),
        cube([-7.2,32.7,2.6],[3.7,0.38,0.95],'steel',[-5.35,32.9,3.08],[0,0,-38]),
        cube([3.5,32.7,2.6],[3.7,0.38,0.95],'steel',[5.35,32.9,3.08],[0,0,38]),
        cube([-6.9,33.5,2.72],[2.6,0.18,0.62],'cyan',[-5.6,33.6,3.03],[0,0,-48]),
        cube([4.3,33.5,2.72],[2.6,0.18,0.62],'cyan',[5.6,33.6,3.03],[0,0,48]),
        cube([-4.65,27.4,2.9],[9.3,0.55,0.9],'violet'),
    ]}

def gorget():
    return {'name':'ouros_shadow_tide_gorget','parent':'chest','pivot':[0,22.2,-1.5],'cubes':[
        cube([-6.0,23.1,-2.8],[3.8,1.65,4.2],'abyss',[-4.1,23.9,-0.7],[0,0,-13]),
        cube([2.2,23.1,-2.8],[3.8,1.65,4.2],'abyss',[4.1,23.9,-0.7],[0,0,13]),
        cube([-5.8,24.5,-2.65],[3.4,0.28,3.8],'steel',[-4.1,24.65,-0.75],[0,0,-13]),
        cube([2.4,24.5,-2.65],[3.4,0.28,3.8],'steel',[4.1,24.65,-0.75],[0,0,13]),
        cube([-2.15,20.1,-2.75],[4.3,4.1,0.52],'indigo'),
        cube([-1.45,20.8,-3.02],[2.9,2.9,0.18],'cyan',[0,22.25,-2.93],[0,0,45]),
        cube([-0.83,21.42,-3.18],[1.66,1.66,0.12],'glass',[0,22.25,-3.12],[0,0,45]),
        cube([-3.6,19.55,-2.35],[7.2,0.38,1.1],'violet'),
    ]}

def pauldron_right():
    return {'name':'ouros_shadow_tide_pauldron_right','parent':'arm_right','pivot':[-4,24.8,1.5],'cubes':[
        cube([-10.7,23.9,-1.7],[6.7,1.8,5.8],'abyss',[-7.4,24.8,1.2],[0,0,-16]),
        cube([-11.3,25.45,-1.3],[7.2,0.38,5.0],'steel',[-7.7,25.65,1.2],[0,0,-16]),
        cube([-12.5,24.9,2.2],[7.2,0.45,2.5],'indigo',[-8.9,25.1,3.45],[16,0,-13]),
        cube([-13.1,26.0,2.9],[4.8,0.28,1.4],'cyan',[-10.7,26.15,3.6],[16,0,-24]),
        cube([-10.3,23.2,-2.0],[1.1,3.5,0.4],'silver',[-9.75,24.95,-1.8],[0,0,-14]),
        cube([-12.0,24.5,-2.05],[1.4,1.4,0.18],'glass',[-11.3,25.2,-1.96],[0,0,45]),
    ]}

def pauldron_left():
    return {'name':'ouros_shadow_tide_pauldron_left','parent':'arm_left','pivot':[4,24.8,1.5],'cubes':[
        cube([4.0,24.0,-1.5],[5.7,1.5,5.1],'abyss',[6.85,24.75,1.05],[0,0,10]),
        cube([4.1,25.3,-1.2],[5.8,0.32,4.5],'steel',[7.0,25.46,1.05],[0,0,10]),
        cube([4.5,24.9,2.0],[5.4,0.40,2.1],'violet',[7.2,25.1,3.05],[14,0,8]),
        cube([6.0,25.8,2.65],[3.5,0.24,1.1],'foam',[7.75,25.92,3.2],[14,0,12]),
        cube([8.6,23.4,-1.9],[0.9,3.0,0.35],'silver',[9.05,24.9,-1.72],[0,0,11]),
    ]}

def bracer_right():
    return {'name':'ouros_shadow_tide_bracer_right','parent':'arm_right2','pivot':[-10,23.8,2.5],'cubes':[
        cube([-14.7,22.3,0.5],[5.0,2.8,3.8],'indigo',[-12.2,23.7,2.4],[0,0,-7]),
        cube([-14.9,24.75,0.65],[5.2,0.30,3.5],'steel',[-12.3,24.9,2.4],[0,0,-7]),
        cube([-15.5,23.0,-0.2],[5.2,0.30,1.2],'cyan',[-12.9,23.15,0.4],[0,0,-18]),
        cube([-14.1,21.95,1.3],[0.35,3.4,2.2],'silver'),
    ]}

def bracer_left():
    return {'name':'ouros_shadow_tide_bracer_left','parent':'arm_left2','pivot':[10,23.8,2.5],'cubes':[
        cube([9.7,22.3,0.5],[5.0,2.8,3.8],'abyss',[12.2,23.7,2.4],[0,0,7]),
        cube([9.7,24.75,0.65],[5.2,0.30,3.5],'steel',[12.3,24.9,2.4],[0,0,7]),
        cube([10.3,23.0,-0.2],[5.2,0.30,1.2],'foam',[12.9,23.15,0.4],[0,0,18]),
        cube([13.75,21.95,1.3],[0.35,3.4,2.2],'silver'),
    ]}

def back_frame():
    # Diagonal tide-blade frame sits behind the torso and reads as a signature shuriken/sheath silhouette.
    return {'name':'ouros_shadow_tide_back_frame','parent':'chest','pivot':[0,23.0,5.3],'cubes':[
        cube([-0.45,16.7,5.6],[0.9,13.3,0.9],'steel',[0,23.35,6.05],[0,0,42]),
        cube([-0.45,16.7,5.6],[0.9,13.3,0.9],'steel',[0,23.35,6.05],[0,0,-42]),
        cube([-7.8,28.3,5.5],[5.0,0.55,1.1],'indigo',[-5.3,28.6,6.05],[0,0,-22]),
        cube([2.8,28.3,5.5],[5.0,0.55,1.1],'abyss',[5.3,28.6,6.05],[0,0,22]),
        cube([-8.6,29.5,5.62],[4.2,0.25,0.72],'cyan',[-6.5,29.63,5.98],[0,0,-34]),
        cube([4.4,29.5,5.62],[4.2,0.25,0.72],'cyan',[6.5,29.63,5.98],[0,0,34]),
        cube([-1.5,21.7,5.25],[3.0,3.0,0.35],'violet',[0,23.2,5.43],[0,0,45]),
        cube([-0.82,22.38,5.05],[1.64,1.64,0.18],'glass',[0,23.2,5.14],[0,0,45]),
    ]}

def mantle():
    return {'name':'ouros_shadow_tide_split_mantle','parent':'waist','pivot':[0,17.8,4.2],'cubes':[
        cube([-5.8,11.3,4.8],[5.0,7.1,0.45],'indigo',[-3.3,17.5,5.03],[-9,0,9]),
        cube([0.8,11.3,4.8],[5.0,7.1,0.45],'abyss',[3.3,17.5,5.03],[-9,0,-9]),
        cube([-5.55,11.0,5.2],[4.4,0.24,0.18],'cyan',[-3.35,11.12,5.29],[-9,0,9]),
        cube([1.15,11.0,5.2],[4.4,0.24,0.18],'foam',[3.35,11.12,5.29],[-9,0,-9]),
        cube([-6.2,17.6,4.55],[5.7,0.8,0.7],'steel',[-3.35,18.0,4.9],[0,0,-5]),
        cube([0.5,17.6,4.55],[5.7,0.8,0.7],'steel',[3.35,18.0,4.9],[0,0,5]),
        cube([-1.0,15.6,5.22],[2.0,2.0,0.18],'cyan',[0,16.6,5.31],[0,0,45]),
    ]}

EXTRA_BUILDERS=(cowl,gorget,pauldron_right,pauldron_left,bracer_right,bracer_left,back_frame,mantle)
EXPECTED_NAMES=['ouros_shadow_tide_cowl','ouros_shadow_tide_gorget','ouros_shadow_tide_pauldron_right','ouros_shadow_tide_pauldron_left','ouros_shadow_tide_bracer_right','ouros_shadow_tide_bracer_left','ouros_shadow_tide_back_frame','ouros_shadow_tide_split_mantle']
REQUIRED={'head','chest','waist','arm_right','arm_left','arm_right2','arm_left2'}

def append_cosmetics(data,identifier):
    d=copy.deepcopy(data); g=d['minecraft:geometry'][0]; names={b['name'] for b in g['bones']}; missing=REQUIRED-names
    if missing: raise RuntimeError(f'missing official parents {sorted(missing)}')
    original=len(g['bones']); extras=[f() for f in EXTRA_BUILDERS]
    g['description']['identifier']=identifier; g['bones'].extend(extras)
    return d,original,extras

def main():
    p=argparse.ArgumentParser(); p.add_argument('--official-normal',required=True); p.add_argument('--official-ash',required=True); p.add_argument('--normal-out',required=True); p.add_argument('--ash-out',required=True); p.add_argument('--overlay-out',required=True); p.add_argument('--metadata-out',required=True); a=p.parse_args()
    normal=json.loads(Path(a.official_normal).read_text()); ash=json.loads(Path(a.official_ash).read_text()); ng=normal['minecraft:geometry'][0]; ag=ash['minecraft:geometry'][0]
    global PIXELS; PIXELS=choose_pixels(ng,ag)
    nout,nn,nextra=append_cosmetics(normal,'geometry.ouros_shadow_tide_greninja'); aout,an,aextra=append_cosmetics(ash,'geometry.ouros_shadow_tide_ash_greninja')
    for path,data in ((a.normal_out,nout),(a.ash_out,aout)):
        Path(path).parent.mkdir(parents=True,exist_ok=True); Path(path).write_text(json.dumps(data,separators=(',',':'))+'\n',encoding='utf-8')
    w=int(ng['description']['texture_width']); h=int(ng['description']['texture_height']); im=Image.new('RGBA',(w,h),(0,0,0,0))
    for mat,rgba in PALETTE.items(): im.putpixel(PIXELS[mat],rgba)
    Path(a.overlay_out).parent.mkdir(parents=True,exist_ok=True); im.save(a.overlay_out,optimize=True)
    meta={'format':'ouros.cobblemon-shadow-tide-build.v1','normalOriginalBoneCount':nn,'ashOriginalBoneCount':an,'normalDerivedBoneCount':nn+len(nextra),'ashDerivedBoneCount':an+len(aextra),'cosmeticBoneCount':len(nextra),'cosmeticBoneNames':EXPECTED_NAMES,'cosmeticCubeCount':sum(len(x['cubes']) for x in nextra),'textureSize':[w,h],'palettePixels':{k:list(v) for k,v in PIXELS.items()}}
    Path(a.metadata_out).parent.mkdir(parents=True,exist_ok=True); Path(a.metadata_out).write_text(json.dumps(meta,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(meta,indent=2))
if __name__=='__main__': main()
