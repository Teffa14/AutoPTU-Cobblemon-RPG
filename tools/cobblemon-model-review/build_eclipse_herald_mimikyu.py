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
 # A single readable broken crescent behind the left ear replaces the noisy all-around ring.
 # Each moon segment has an inner violet/aura edge so the silhouette reads as one relic.
 cs=[]
 segments=[
  (-8.4,10.8,-18,4.8),
  (-10.0,13.5,-38,4.8),
  (-10.4,16.5,-62,4.7),
  (-9.2,19.2,-92,4.6),
  (-6.5,21.0,-124,4.4),
 ]
 for x,y,r,length in segments:
  cs.append(cube([x,y,5.0],[length,0.72,0.92],'moon',[x+length/2,y+0.36,5.46],[0,0,r]))
  cs.append(cube([x+0.28,y+0.70,4.72],[max(2.8,length-0.56),0.18,0.24],'violet',[x+length/2,y+0.79,4.84],[0,0,r]))
 # A small separated fracture on the opposite side makes the eclipse intentionally broken,
 # but stays close to the head instead of reading as random floating debris.
 cs += [
  cube([5.7,14.0,5.0],[3.7,0.66,0.84],'void',[7.55,14.33,5.42],[0,0,42]),
  cube([6.0,14.65,4.72],[3.0,0.16,0.22],'aura',[7.5,14.73,4.83],[0,0,42]),
 ]
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
 # Lower rear standard: visible in 3/4/back, but no longer competes with the ears.
 return {'name':'ouros_eclipse_pennant_left','parent':'torso_top','pivot':[4.2,7.0,3.8],'cubes':[
  cube([4.8,4.9,4.0],[0.48,8.1,0.48],'silver',[5.04,8.95,4.24],[0,0,7]),
  cube([5.0,7.0,4.22],[4.5,4.8,0.38],'violet',[7.25,9.4,4.41],[0,0,-5]),
  cube([5.5,11.0,4.0],[3.3,0.22,0.72],'moon',[7.15,11.11,4.36],[0,0,-5]),
  cube([6.45,8.45,3.98],[1.15,1.15,0.18],'aura',[7.03,9.03,4.07],[0,0,45])
 ]}

def pennant_right():
 return {'name':'ouros_eclipse_pennant_right','parent':'torso_top','pivot':[-4.2,6.5,3.8],'cubes':[
  cube([-5.28,4.8,4.0],[0.48,6.6,0.48],'silver',[-5.04,8.1,4.24],[0,0,-6]),
  cube([-9.1,6.5,4.22],[4.1,3.8,0.38],'void',[-7.05,8.4,4.41],[0,0,6]),
  cube([-8.5,9.55,4.0],[2.9,0.22,0.72],'teal',[-7.05,9.66,4.36],[0,0,6])
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
