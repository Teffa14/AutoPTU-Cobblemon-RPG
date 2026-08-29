#!/usr/bin/env python3
"""Build Eclipse Herald around exact official Cobblemon 1.7.3 Mimikyu geometry."""
from __future__ import annotations
import argparse, json
from pathlib import Path
from PIL import Image

PALETTE={
 "void":(24,20,35,255),"cloth":(58,44,72,255),"moon":(224,215,165,255),"silver":(160,166,190,255),
 "violet":(110,67,164,255),"aura":(184,112,255,180),"ember":(244,108,94,230),"teal":(76,190,178,220)
}
FACES=("north","east","south","west","up","down")
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

def eclipse_halo():
 cs=[]
 # broken crescent/frame behind ears, wide enough to change 3/4 silhouette
 seg=[(-9.5,13.3,-38),(-11.7,16.2,-62),(-9.0,19.0,-112),(5.5,13.3,38),(7.2,16.2,62),(5.0,19.0,112)]
 for i,(x,y,r) in enumerate(seg):
  mat='void' if i%2==0 else 'violet'; cs.append(cube([x,y,4.8],[5.8,0.55,0.8],mat,[x+2.9,y+0.275,5.2],[0,0,r]))
  cs.append(cube([x+0.35,y+0.58,4.55],[4.9,0.16,0.22],'aura',[x+2.8,y+0.66,4.66],[0,0,r]))
 cs += [cube([-1.0,21.1,4.7],[2.0,1.7,0.35],'moon',[0,21.95,4.88],[0,0,45]), cube([-0.6,20.0,4.45],[1.2,0.5,0.22],'teal')]
 return {'name':'ouros_eclipse_halo','parent':'torso_top','pivot':[0,14,4.8],'cubes':cs}

def ritual_cowl():
 return {'name':'ouros_eclipse_cowl','parent':'head','pivot':[0,11,2.5],'cubes':[
  cube([-5.2,9.0,2.9],[1.4,5.0,2.0],'cloth',[-4.5,11.5,3.9],[0,0,-12]), cube([3.8,9.0,2.9],[1.4,5.0,2.0],'void',[4.5,11.5,3.9],[0,0,12]),
  cube([-4.9,13.4,2.7],[2.2,0.35,2.4],'silver',[-3.8,13.58,3.9],[0,0,-12]), cube([2.7,13.4,2.7],[2.2,0.35,2.4],'moon',[3.8,13.58,3.9],[0,0,12]),
  cube([-5.5,8.2,1.4],[0.35,2.8,1.3],'aura'), cube([5.15,8.2,1.4],[0.35,2.8,1.3],'ember')
 ]}

def mantle():
 return {'name':'ouros_eclipse_mantle','parent':'torso_top','pivot':[0,7,0],'cubes':[
  cube([-7.1,6.6,-3.7],[4.4,1.15,7.4],'void',[-4.9,7.18,0],[0,0,-9]), cube([2.7,6.6,-3.7],[4.4,1.15,7.4],'cloth',[4.9,7.18,0],[0,0,9]),
  cube([-7.3,7.55,-3.45],[4.7,0.25,6.9],'silver',[-4.95,7.68,0],[0,0,-9]), cube([2.6,7.55,-3.45],[4.7,0.25,6.9],'violet',[4.95,7.68,0],[0,0,9]),
  cube([-6.8,5.9,3.35],[3.4,5.3,0.42],'cloth',[-5.1,7.8,3.56],[-8,0,8]), cube([3.4,5.9,3.35],[3.4,5.3,0.42],'void',[5.1,7.8,3.56],[-8,0,-8])
 ]}

def pennant_left():
 return {'name':'ouros_eclipse_pennant_left','parent':'torso_top','pivot':[4.2,8.0,2.8],'cubes':[
  cube([4.5,7.2,3.0],[0.55,8.4,0.55],'silver',[4.78,11.4,3.28],[0,0,12]), cube([4.8,11.7,3.25],[4.6,5.8,0.36],'violet',[7.1,14.6,3.43],[0,0,-9]),
  cube([5.4,16.6,3.0],[3.1,0.22,0.72],'moon',[6.95,16.71,3.36],[0,0,-9]), cube([6.6,13.2,2.95],[1.2,1.2,0.18],'aura',[7.2,13.8,3.04],[0,0,45])
 ]}

def pennant_right():
 return {'name':'ouros_eclipse_pennant_right','parent':'torso_top','pivot':[-4.2,8.0,2.8],'cubes':[
  cube([-5.05,7.0,3.0],[0.55,6.8,0.55],'silver',[-4.78,10.4,3.28],[0,0,-9]), cube([-9.0,10.3,3.25],[4.2,4.4,0.36],'void',[-6.9,12.5,3.43],[0,0,11]),
  cube([-8.3,13.8,3.0],[2.8,0.22,0.72],'teal',[-6.9,13.91,3.36],[0,0,11])
 ]}

def tail_reliquary():
 return {'name':'ouros_eclipse_tail_reliquary','parent':'tail1','pivot':[0,3,5.5],'cubes':[
  cube([-1.8,2.4,5.0],[3.6,2.0,1.8],'void'), cube([-1.45,2.75,4.72],[2.9,1.3,0.28],'moon'), cube([-0.45,3.0,4.5],[0.9,0.9,0.20],'aura',[0,3.45,4.6],[0,0,45]),
  cube([-2.3,1.7,5.5],[0.45,3.8,0.45],'silver',[-2.08,3.6,5.73],[0,0,-14]), cube([1.85,1.7,5.5],[0.45,3.8,0.45],'silver',[2.08,3.6,5.73],[0,0,14])
 ]}

def hand_charm_right():
 return {'name':'ouros_eclipse_hand_charm_right','parent':'hand_right3','pivot':[-15.5,0,0],'cubes':[
  cube([-17.3,0.6,-0.45],[3.4,0.34,0.9],'silver',[-15.6,0.77,0],[0,0,-12]), cube([-18.0,1.1,-0.22],[1.4,3.7,0.44],'cloth',[-17.3,1.3,0],[0,0,-18]),
  cube([-17.65,4.25,-0.3],[0.75,0.75,0.6],'ember',[-17.28,4.62,0],[0,0,45])
 ]}

def hand_charm_left():
 return {'name':'ouros_eclipse_hand_charm_left','parent':'hand_left3','pivot':[15.5,0,0],'cubes':[
  cube([13.9,0.6,-0.45],[3.4,0.34,0.9],'silver',[15.6,0.77,0],[0,0,12]), cube([16.6,1.1,-0.22],[1.4,4.8,0.44],'void',[17.3,1.3,0],[0,0,18]),
  cube([16.9,5.35,-0.3],[0.75,0.75,0.6],'aura',[17.28,5.72,0],[0,0,45])
 ]}

def build(src):
 global PIXELS
 data=json.loads(Path(src).read_text()); g=data['minecraft:geometry'][0]; PIXELS=choose_pixels(g)
 original=len(g['bones']); extras=[eclipse_halo(),ritual_cowl(),mantle(),pennant_left(),pennant_right(),tail_reliquary(),hand_charm_right(),hand_charm_left()]
 g['description']['identifier']='geometry.ouros_eclipse_herald_mimikyu'; g['bones'].extend(extras)
 return data,original,extras

def main():
 p=argparse.ArgumentParser(); p.add_argument('--official',required=True); p.add_argument('--model-out',required=True); p.add_argument('--overlay-out',required=True); p.add_argument('--metadata-out',required=True); a=p.parse_args()
 data,n,extras=build(a.official); g=data['minecraft:geometry'][0]; w=int(g['description']['texture_width']); h=int(g['description']['texture_height'])
 Path(a.model_out).parent.mkdir(parents=True,exist_ok=True); Path(a.model_out).write_text(json.dumps(data,separators=(',',':'))+'\n')
 im=Image.new('RGBA',(w,h),(0,0,0,0))
 for k,rgba in PALETTE.items(): im.putpixel(PIXELS[k],rgba)
 Path(a.overlay_out).parent.mkdir(parents=True,exist_ok=True); im.save(a.overlay_out,optimize=True)
 meta={'format':'ouros.cobblemon-skin-build.v1','species':'cobblemon:mimikyu','concept':'Eclipse Herald','originalBoneCount':n,'derivedBoneCount':len(g['bones']),'cosmeticBones':[x['name'] for x in extras],'cosmeticCubeCount':sum(len(x.get('cubes',[])) for x in extras),'palettePixels':PIXELS,'textureSize':[w,h]}
 Path(a.metadata_out).parent.mkdir(parents=True,exist_ok=True); Path(a.metadata_out).write_text(json.dumps(meta,indent=2)+'\n')
 print(json.dumps(meta,indent=2))
if __name__=='__main__': main()
