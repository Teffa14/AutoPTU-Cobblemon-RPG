#!/usr/bin/env python3
"""Build Solar Legion around exact official Cobblemon Charizard geometry."""
from __future__ import annotations
import argparse, json
from pathlib import Path
from PIL import Image

PALETTE={
 "obsidian":(31,32,38,255),"brass":(214,150,52,255),"ivory":(224,207,164,255),"crimson":(128,35,35,255),
 "sun":(255,188,54,255),"glass":(255,218,112,175),"ash":(75,70,76,255),"ember":(255,91,41,225)
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

def crown():
 # Rear/open solar crown: makes the head regal without masking eyes, muzzle or original horns.
 return {'name':'ouros_solar_crown','parent':'head_angle','pivot':[0,45.0,2.6],'cubes':[
  cube([-5.4,47.0,2.7],[1.0,3.8,1.1],'obsidian',[-4.9,48.9,3.25],[0,0,-18]),
  cube([4.4,47.0,2.7],[1.0,3.8,1.1],'obsidian',[4.9,48.9,3.25],[0,0,18]),
  cube([-4.7,50.0,2.8],[2.7,0.55,1.0],'brass',[-3.35,50.28,3.3],[0,0,-22]),
  cube([2.0,50.0,2.8],[2.7,0.55,1.0],'brass',[3.35,50.28,3.3],[0,0,22]),
  cube([-1.0,50.7,2.9],[2.0,4.7,0.95],'sun',[0,51.0,3.38],[0,0,45]),
  cube([-0.55,51.1,2.68],[1.1,3.2,0.20],'glass',[0,51.4,2.78],[0,0,45]),
  cube([-5.8,46.3,2.4],[1.8,0.35,1.8],'crimson',[-4.9,46.48,3.3],[0,0,-12]),
  cube([4.0,46.3,2.4],[1.8,0.35,1.8],'crimson',[4.9,46.48,3.3],[0,0,12])
 ]}

def gorget_core():
 return {'name':'ouros_solar_gorget_core','parent':'torso2','pivot':[0,23.0,-3.8],'cubes':[
  cube([-7.2,22.4,-6.9],[3.8,2.4,4.0],'obsidian',[-5.3,23.6,-4.9],[0,0,-13]),
  cube([3.4,22.4,-6.9],[3.8,2.4,4.0],'obsidian',[5.3,23.6,-4.9],[0,0,13]),
  cube([-6.8,24.3,-7.05],[3.2,0.36,3.6],'brass',[-5.2,24.48,-5.25],[0,0,-13]),
  cube([3.6,24.3,-7.05],[3.2,0.36,3.6],'brass',[5.2,24.48,-5.25],[0,0,13]),
  cube([-2.7,21.4,-7.3],[5.4,5.1,0.65],'ash'),
  cube([-1.85,22.15,-7.72],[3.7,3.7,0.25],'sun',[0,24.0,-7.60],[0,0,45]),
  cube([-1.10,22.90,-7.94],[2.2,2.2,0.18],'glass',[0,24.0,-7.85],[0,0,45]),
  cube([-0.35,25.8,-7.25],[0.7,2.8,0.60],'brass'),
  cube([-3.7,20.55,-6.7],[7.4,0.46,1.1],'crimson'),
  cube([-3.1,20.1,-6.45],[6.2,0.24,0.7],'ivory')
 ]}

def pauldron_right():
 return {'name':'ouros_solar_pauldron_right','parent':'shoulder_right','pivot':[-6,23,0],'cubes':[
  cube([-12.2,23.2,-3.2],[6.5,2.0,6.4],'obsidian',[-8.9,24.2,0],[0,0,-14]),
  cube([-12.4,25.0,-2.8],[6.7,0.42,5.6],'brass',[-9.0,25.21,0],[0,0,-14]),
  cube([-12.8,24.0,1.9],[6.3,0.55,2.6],'crimson',[-9.6,24.28,3.2],[18,0,-10]),
  cube([-13.0,25.3,2.6],[4.2,0.40,1.7],'sun',[-10.9,25.5,3.45],[18,0,-18]),
  cube([-10.1,22.4,-3.5],[1.0,4.2,0.55],'ivory',[-9.6,24.5,-3.23],[0,0,-15]),
  cube([-12.25,24.0,-3.55],[1.3,1.3,0.25],'glass',[-11.6,24.65,-3.43],[0,0,45])
 ]}

def pauldron_left():
 return {'name':'ouros_solar_pauldron_left','parent':'shoulder_left','pivot':[6,23,0],'cubes':[
  cube([5.7,23.2,-3.2],[6.5,2.0,6.4],'obsidian',[8.9,24.2,0],[0,0,10]),
  cube([5.7,25.0,-2.8],[6.7,0.42,5.6],'brass',[9.0,25.21,0],[0,0,10]),
  cube([6.5,24.0,1.9],[5.6,0.55,2.6],'crimson',[9.3,24.28,3.2],[18,0,8]),
  cube([7.9,25.15,2.6],[3.6,0.40,1.7],'ivory',[9.7,25.35,3.45],[18,0,13]),
  cube([9.1,22.4,-3.5],[1.0,4.2,0.55],'brass',[9.6,24.5,-3.23],[0,0,13])
 ]}

def wing_standard_right():
 return {'name':'ouros_solar_wing_standard_right','parent':'wing_right_base','pivot':[-2.5,22.75,5.25],'cubes':[
  cube([-6.6,22.5,5.7],[0.55,11.0,0.55],'brass',[-6.33,28.0,5.98],[0,0,-9]),
  cube([-10.8,29.8,5.9],[4.7,5.4,0.38],'crimson',[-8.45,32.5,6.09],[0,0,-5]),
  cube([-10.7,34.4,5.78],[4.4,0.32,0.78],'sun',[-8.5,34.56,6.17],[0,0,-5]),
  cube([-9.2,31.2,5.62],[1.5,1.5,0.22],'glass',[-8.45,31.95,5.73],[0,0,45]),
  cube([-6.9,21.9,5.4],[1.2,1.2,1.2],'obsidian')
 ]}

def wing_standard_left():
 return {'name':'ouros_solar_wing_standard_left','parent':'wing_left_base','pivot':[2.5,22.75,5.25],'cubes':[
  cube([6.05,22.5,5.7],[0.55,9.0,0.55],'brass',[6.33,27.0,5.98],[0,0,8]),
  cube([6.1,28.2,5.9],[4.2,4.5,0.38],'ash',[8.2,30.45,6.09],[0,0,5]),
  cube([6.3,31.95,5.78],[3.8,0.32,0.78],'ivory',[8.2,32.11,6.17],[0,0,5]),
  cube([7.55,29.25,5.62],[1.35,1.35,0.22],'sun',[8.22,29.92,5.73],[0,0,45]),
  cube([5.7,21.9,5.4],[1.2,1.2,1.2],'obsidian')
 ]}

def tail_brazier():
 # Ring and fins sit around the official fire root; the fire planes stay untouched and visible.
 return {'name':'ouros_solar_tail_brazier','parent':'tail5','pivot':[0,9.5,58.8],'cubes':[
  cube([-3.7,8.15,57.5],[7.4,0.65,0.85],'obsidian'),
  cube([-3.7,11.1,57.5],[7.4,0.65,0.85],'brass'),
  cube([-3.7,8.8,57.5],[0.65,2.4,0.85],'brass'),
  cube([3.05,8.8,57.5],[0.65,2.4,0.85],'brass'),
  cube([-5.2,10.8,57.65],[2.8,0.42,1.1],'crimson',[-3.8,11.0,58.2],[0,0,-28]),
  cube([2.4,10.8,57.65],[2.8,0.42,1.1],'crimson',[3.8,11.0,58.2],[0,0,28]),
  cube([-4.8,12.5,57.7],[2.6,0.34,0.9],'sun',[-3.5,12.67,58.15],[0,0,-43]),
  cube([2.2,12.5,57.7],[2.6,0.34,0.9],'sun',[3.5,12.67,58.15],[0,0,43])
 ]}

def mantle():
 return {'name':'ouros_solar_legion_mantle','parent':'torso','pivot':[0,15,6.8],'cubes':[
  cube([-7.5,13.3,7.5],[6.6,8.0,0.55],'crimson',[-4.2,20.5,7.78],[-8,0,8]),
  cube([0.9,13.3,7.5],[6.6,8.0,0.55],'ash',[4.2,20.5,7.78],[-8,0,-8]),
  cube([-7.2,12.9,7.95],[5.9,0.28,0.24],'brass',[-4.25,13.04,8.07],[-8,0,8]),
  cube([1.3,12.9,7.95],[5.9,0.28,0.24],'brass',[4.25,13.04,8.07],[-8,0,-8]),
  cube([-7.8,20.4,7.35],[7.1,1.15,0.80],'obsidian',[-4.25,20.98,7.75],[0,0,-4]),
  cube([0.7,20.4,7.35],[7.1,1.15,0.80],'obsidian',[4.25,20.98,7.75],[0,0,4]),
  cube([-1.15,18.1,8.05],[2.3,2.3,0.24],'sun',[0,19.25,8.17],[0,0,45]),
  cube([-0.62,18.63,7.84],[1.24,1.24,0.18],'glass',[0,19.25,7.93],[0,0,45])
 ]}

def build(src):
 global PIXELS
 data=json.loads(Path(src).read_text()); g=data['minecraft:geometry'][0]; PIXELS=choose_pixels(g)
 parents={b['name'] for b in g['bones']}
 required={'head_angle','torso2','torso','shoulder_right','shoulder_left','wing_right_base','wing_left_base','tail5'}
 missing=required-parents
 if missing: raise RuntimeError(f'missing official parents: {sorted(missing)}')
 original=len(g['bones']); extras=[crown(),gorget_core(),pauldron_right(),pauldron_left(),wing_standard_right(),wing_standard_left(),tail_brazier(),mantle()]
 g['description']['identifier']='geometry.ouros_solar_legion_charizard'; g['bones'].extend(extras)
 return data,original,extras

def main():
 p=argparse.ArgumentParser(); p.add_argument('--official',required=True); p.add_argument('--model-out',required=True); p.add_argument('--overlay-out',required=True); p.add_argument('--metadata-out',required=True); a=p.parse_args()
 data,n,extras=build(a.official); g=data['minecraft:geometry'][0]; w=int(g['description']['texture_width']); h=int(g['description']['texture_height'])
 Path(a.model_out).parent.mkdir(parents=True,exist_ok=True); Path(a.model_out).write_text(json.dumps(data,separators=(',',':'))+'\n')
 im=Image.new('RGBA',(w,h),(0,0,0,0))
 for k,rgba in PALETTE.items(): im.putpixel(PIXELS[k],rgba)
 Path(a.overlay_out).parent.mkdir(parents=True,exist_ok=True); im.save(a.overlay_out,optimize=True)
 meta={'format':'ouros.cobblemon-skin-build.v1','species':'cobblemon:charizard','concept':'Solar Legion','originalBoneCount':n,'derivedBoneCount':len(g['bones']),'cosmeticBones':[x['name'] for x in extras],'cosmeticCubeCount':sum(len(x.get('cubes',[])) for x in extras),'palettePixels':PIXELS,'textureSize':[w,h]}
 Path(a.metadata_out).parent.mkdir(parents=True,exist_ok=True); Path(a.metadata_out).write_text(json.dumps(meta,indent=2)+'\n')
 print(json.dumps(meta,indent=2))
if __name__=='__main__': main()
