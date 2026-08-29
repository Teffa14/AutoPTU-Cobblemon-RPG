#!/usr/bin/env python3
"""Epic v2 Omen Regent over exact official Cobblemon 1.7.3 Absol geometry."""
from __future__ import annotations
import argparse,json
from pathlib import Path
from PIL import Image
PALETTE={'obsidian':(24,22,34,255),'midnight':(39,34,62,255),'silver':(172,180,201,255),'bone':(224,218,195,255),'crimson':(158,46,67,255),'violet':(110,70,168,255),'aura':(186,116,245,190),'gold':(216,166,72,255)}
FACES=('north','east','south','west','up','down'); PIXELS={}
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
     if isinstance(f,dict): p=f.get('uv',[0,0]); e=f.get('uv_size',[1,1]); mark(p[0],p[1],e[0],e[1])
 return used
def choose_pixels(g):
 w=int(g['description']['texture_width']); h=int(g['description']['texture_height']); used=mark_uv_usage(g); free=[(x,y) for y in range(h-1,-1,-1) for x in range(w) if (x,y) not in used]
 if len(free)<8: raise RuntimeError('insufficient free UV texels')
 return {k:free[i] for i,k in enumerate(PALETTE)}
def suv(m):
 x,y=PIXELS[m]; return {f:{'uv':[x,y],'uv_size':[1,1]} for f in FACES}
def cube(o,s,m,p=None,r=None):
 d={'origin':o,'size':s,'uv':suv(m)}
 if p is not None:d['pivot']=p
 if r is not None:d['rotation']=r
 return d

def crown():
 cs=[]
 for x,y,rot,l in [(3.2,31.0,-18,6.8),(6.8,33.7,-40,6.6),(8.8,37.2,-65,6.1),(8.5,40.9,-92,5.8),(6.1,43.2,-122,5.2)]:
  cs += [cube([x,y,-7.7],[l,1.05,1.25],'bone',[x+l/2,y+.52,-7.08],[0,0,rot]),cube([x+.32,y+.92,-7.98],[max(3.2,l-.64),.25,.28],'violet',[x+l/2,y+1.04,-7.84],[0,0,rot])]
 cs += [cube([2.8,29.5,-9.35],[2.5,2.5,1.2],'gold',[4.05,30.75,-8.75],[0,0,45]),cube([3.35,30.05,-9.60],[1.4,1.4,.22],'aura',[4.05,30.75,-9.49],[0,0,45])]
 return {'name':'ouros_omen_crown','parent':'head_angle','pivot':[4,34,-8],'cubes':cs}
def gorget():
 return {'name':'ouros_omen_gorget','parent':'neck','pivot':[0,22,-5],'cubes':[cube([-6.1,18.7,-10.6],[2.0,7.2,7.8],'obsidian',[-5.1,22.3,-6.7],[0,0,-11]),cube([4.1,18.7,-10.6],[2.0,7.2,7.8],'midnight',[5.1,22.3,-6.7],[0,0,11]),cube([-5.0,18.6,-11.0],[10.0,1.3,1.2],'silver'),cube([-2.6,19.3,-11.45],[5.2,3.2,.72],'gold'),cube([-1.85,20.0,-11.72],[3.7,1.8,.24],'aura'),cube([-.55,20.35,-11.86],[1.1,1.1,.16],'crimson',[0,20.9,-11.78],[0,0,45])]}
def left_pauldron():
 return {'name':'ouros_omen_pauldron_left','parent':'chest','pivot':[6.2,19,-4],'cubes':[cube([3.8,16.7,-10.4],[7.8,2.7,9.0],'obsidian',[7.7,18.05,-5.9],[0,0,13]),cube([4.1,19.0,-10.0],[7.2,.42,8.3],'silver',[7.7,19.21,-5.85],[0,0,13]),cube([9.3,18.0,-9.2],[5.4,.9,6.6],'bone',[12,18.45,-5.9],[0,0,28]),cube([10.2,18.65,-8.4],[3.7,.28,4.9],'violet',[12.05,18.79,-5.95],[0,0,28]),cube([10.7,15.4,-7.8],[1.1,4.0,4.0],'crimson',[11.25,17.4,-5.8],[0,0,17])]}
def right_pauldron():
 return {'name':'ouros_omen_pauldron_right','parent':'chest','pivot':[-6,19,-4],'cubes':[cube([-10.5,17.0,-9.8],[6.5,2.2,8.0],'midnight',[-7.25,18.1,-5.8],[0,0,-10]),cube([-10.2,19.0,-9.45],[5.9,.38,7.3],'gold',[-7.25,19.19,-5.8],[0,0,-10]),cube([-12.3,16.2,-8.0],[2.0,4.8,4.7],'obsidian',[-11.3,18.6,-5.65],[0,0,-16]),cube([-12.7,20.1,-7.5],[2.8,.45,3.8],'bone',[-11.3,20.33,-5.6],[0,0,-16])]}
def mantle():
 return {'name':'ouros_omen_split_mantle','parent':'torso','pivot':[0,18,8],'cubes':[cube([-10.2,12.0,7.6],[8.7,10.8,.85],'obsidian',[-5.8,20.0,8.0],[-10,0,12]),cube([1.5,12.0,7.6],[8.7,10.8,.85],'midnight',[5.8,20.0,8.0],[-10,0,-12]),cube([-9.7,12.3,8.35],[7.8,.4,.28],'silver',[-5.8,12.5,8.5],[-10,0,12]),cube([1.9,12.3,8.35],[7.8,.4,.28],'crimson',[5.8,12.5,8.5],[-10,0,-12]),cube([-9.0,9.0,8.1],[4.2,5.2,.55],'violet',[-6.9,13.2,8.4],[-10,0,12]),cube([4.8,9.0,8.1],[4.2,5.2,.55],'obsidian',[6.9,13.2,8.4],[-10,0,-12]),cube([-7.5,18.5,8.45],[15.0,1.3,.55],'silver')]}
def eclipse_frame():
 cs=[cube([-7.8,16.0,9.0],[1.0,18.0,1.0],'silver',[-7.3,25,9.5],[0,0,-10]),cube([6.8,16.0,9.0],[1.0,18.0,1.0],'silver',[7.3,25,9.5],[0,0,10])]
 for x,y,rot,l in [(-9.4,30.0,18,6.0),(-6.8,34.4,42,5.7),(-3.1,37.4,67,5.4),(1.0,38.1,94,5.1),(5.1,36.5,120,5.2)]: cs += [cube([x,y,9.2],[l,1.0,.9],'obsidian',[x+l/2,y+.5,9.65],[0,0,rot]),cube([x+.3,y+.88,9.0],[max(2.8,l-.6),.22,.22],'gold',[x+l/2,y+.99,9.11],[0,0,rot])]
 cs += [cube([-1.7,34.3,8.6],[3.4,3.4,1.2],'gold',[0,36,9.2],[0,0,45]),cube([-.9,35.1,8.35],[1.8,1.8,.22],'aura',[0,36,8.46],[0,0,45])]
 return {'name':'ouros_omen_eclipse_frame','parent':'torso','pivot':[0,26,9],'cubes':cs}
def reliquary():
 return {'name':'ouros_omen_tail_reliquary','parent':'tail','pivot':[0,22,12],'cubes':[cube([-3.0,20.0,10.6],[6.0,3.8,3.8],'obsidian'),cube([-2.4,20.7,10.25],[4.8,2.4,.36],'gold'),cube([-1.0,21.25,9.98],[2.0,1.6,.22],'aura',[0,22.05,10.09],[0,0,45]),cube([-4.0,18.8,12.2],[.7,6.2,.7],'silver',[-3.65,21.9,12.55],[0,0,-20]),cube([3.3,18.8,12.2],[.7,6.2,.7],'silver',[3.65,21.9,12.55],[0,0,20])]}
def rear_relic():
 return {'name':'ouros_omen_rear_relic','parent':'torso','pivot':[-8,23,8],'cubes':[cube([-10.0,10.5,7.2],[1.0,25.0,1.0],'silver',[-9.5,23,7.7],[0,0,-20]),cube([-10.8,19.5,6.8],[2.6,2.6,1.8],'gold',[-9.5,20.8,7.7],[0,0,45]),cube([-10.15,20.15,6.48],[1.3,1.3,.22],'aura',[-9.5,20.8,6.59],[0,0,45]),cube([-18.0,31.0,7.0],[10.5,1.8,1.2],'bone',[-12.75,31.9,7.6],[0,0,-30]),cube([-20.8,34.4,7.0],[9.5,1.5,1.2],'bone',[-16.05,35.15,7.6],[0,0,-52]),cube([-21.8,37.3,7.0],[7.2,1.2,1.2],'bone',[-18.2,37.9,7.6],[0,0,-70]),cube([-19.8,35.3,6.66],[7.2,.28,.28],'crimson',[-16.2,35.44,6.8],[0,0,-52]),cube([-20.6,38.1,6.66],[5.5,.28,.28],'violet',[-17.85,38.24,6.8],[0,0,-70])]}
def build(src):
 global PIXELS
 data=json.loads(Path(src).read_text()); g=data['minecraft:geometry'][0]; PIXELS=choose_pixels(g); original=len(g['bones']); extras=[crown(),gorget(),left_pauldron(),right_pauldron(),mantle(),eclipse_frame(),reliquary(),rear_relic()]; g['description']['identifier']='geometry.ouros_omen_regent_absol'; g['bones'].extend(extras); return data,original,extras
def main():
 p=argparse.ArgumentParser(); p.add_argument('--official',required=True); p.add_argument('--model-out',required=True); p.add_argument('--overlay-out',required=True); p.add_argument('--metadata-out',required=True); a=p.parse_args(); data,n,extras=build(a.official); g=data['minecraft:geometry'][0]; w=int(g['description']['texture_width']); h=int(g['description']['texture_height'])
 Path(a.model_out).parent.mkdir(parents=True,exist_ok=True); Path(a.model_out).write_text(json.dumps(data,separators=(',',':'))+'\n'); im=Image.new('RGBA',(w,h),(0,0,0,0))
 for k,v in PALETTE.items(): im.putpixel(PIXELS[k],v)
 Path(a.overlay_out).parent.mkdir(parents=True,exist_ok=True); im.save(a.overlay_out,optimize=True); meta={'format':'ouros.cobblemon-skin-build.v1','species':'cobblemon:absol','concept':'Omen Regent v2','originalBoneCount':n,'derivedBoneCount':len(g['bones']),'cosmeticBones':[x['name'] for x in extras],'cosmeticCubeCount':sum(len(x.get('cubes',[])) for x in extras),'palettePixels':PIXELS,'textureSize':[w,h],'artPass':'v2-epic-silhouette'}; Path(a.metadata_out).parent.mkdir(parents=True,exist_ok=True); Path(a.metadata_out).write_text(json.dumps(meta,indent=2)+'\n'); print(json.dumps(meta,indent=2))
if __name__=='__main__':main()
