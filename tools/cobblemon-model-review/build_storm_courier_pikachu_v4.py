#!/usr/bin/env python3
from __future__ import annotations
import argparse, copy, hashlib, json
from pathlib import Path
from PIL import Image

PALETTE={
 'stormcloth':(32,42,54,255),'stormcloth_hi':(55,70,86,255),'ceramic':(190,202,206,255),
 'copper':(184,112,54,255),'copper_dark':(105,62,35,255),'energy':(90,220,255,255),
 'energy_hi':(216,250,255,255),'rubber':(20,24,29,255),'signal':(245,196,45,255),'shadow':(9,12,16,255)
}
PIX={k:(i,63) for i,k in enumerate(PALETTE)}
FACES=('north','east','south','west','up','down')

def solid_uv(mat):
 x,y=PIX[mat]; return {f:{'uv':[x,y],'uv_size':[1,1]} for f in FACES}

def cube(origin,size,mat,**kw):
 d={'origin':origin,'size':size,'uv':solid_uv(mat)}
 d.update({k:v for k,v in kw.items() if v is not None}); return d

def visor_cowl():
 return {'name':'ouros_v4_storm_visor_cowl','parent':'head_angle','pivot':[0,16,0],'cubes':[
  cube([-5.35,16.3,-4.55],[10.7,3.8,1.0],'stormcloth'),
  cube([-5.8,18.65,-3.8],[11.6,2.25,5.4],'stormcloth_hi'),
  cube([-6.65,17.25,-2.8],[1.55,3.5,4.6],'ceramic',pivot=[-5.9,19.0,-.5],rotation=[0,0,-12]),
  cube([5.10,17.55,-2.8],[1.35,3.15,4.6],'ceramic',pivot=[5.78,19.0,-.5],rotation=[0,0,9]),
  cube([-4.2,15.35,-5.05],[8.4,1.1,.55],'rubber'),
  cube([-3.65,15.55,-5.35],[2.85,.58,.22],'energy'), cube([.8,15.55,-5.35],[2.85,.58,.22],'energy_hi'),
  cube([-5.1,20.1,-1.8],[2.6,1.05,3.2],'copper',pivot=[-3.8,20.6,-.2],rotation=[0,-8,-8]),
  cube([2.55,20.15,-1.5],[2.3,.85,3.0],'copper_dark',pivot=[3.7,20.55,0],rotation=[0,8,7]),
 ]}

def mantle_shell():
 return {'name':'ouros_v4_storm_mantle_shell','parent':'torso2','pivot':[0,11,0.5],'cubes':[
  cube([-6.2,10.0,-3.9],[12.4,4.8,8.4],'stormcloth'),
  cube([-8.3,11.7,-3.0],[3.0,3.5,6.7],'ceramic',pivot=[-6.8,13.3,.3],rotation=[0,0,-12]),
  cube([5.25,12.0,-2.8],[2.5,3.05,6.3],'ceramic',pivot=[6.5,13.4,.3],rotation=[0,0,9]),
  cube([-6.4,9.45,-4.3],[12.8,.7,.55],'copper_dark'),
  cube([-5.1,14.45,-3.5],[10.2,.65,7.4],'stormcloth_hi'),
  cube([-7.9,14.3,1.8],[2.1,.5,2.2],'energy',pivot=[-6.85,14.55,2.9],rotation=[0,0,-12]),
  cube([5.55,14.2,1.9],[1.7,.45,2.0],'signal',pivot=[6.4,14.42,2.9],rotation=[0,0,9]),
 ]}

def power_frame():
 return {'name':'ouros_v4_storm_power_frame','parent':'torso2','pivot':[0,10,4],'cubes':[
  cube([-4.6,7.0,4.0],[9.2,7.7,3.0],'rubber'), cube([-4.0,7.7,6.85],[8.0,6.0,1.5],'stormcloth_hi'),
  cube([-2.25,8.9,8.1],[4.5,3.6,1.4],'copper'), cube([-1.55,9.55,9.45],[3.1,2.2,.45],'energy'),
  cube([-6.1,8.2,5.3],[1.2,8.5,1.2],'copper_dark',pivot=[-5.5,8.2,5.9],rotation=[-7,0,-6]),
  cube([5.05,9.1,5.45],[1.0,7.6,1.0],'copper',pivot=[5.55,9.1,5.95],rotation=[-5,0,7]),
  cube([-7.15,15.6,5.0],[2.0,.65,2.1],'energy',pivot=[-6.15,15.9,6.05],rotation=[0,0,-14]),
  cube([5.25,15.35,5.15],[1.75,.6,1.9],'signal',pivot=[6.12,15.65,6.1],rotation=[0,0,10]),
 ]}

def conductor(side):
 left=side=='left'; x=-7.1 if left else 5.9; s=-1 if left else 1
 return {'name':f'ouros_v4_storm_field_coil_{side}','parent':'torso2','pivot':[x,9,3.0],'cubes':[
  cube([x,7.8,2.55],[1.25,5.3,1.25],'copper'), cube([x-.25,8.6,2.25],[1.75,.45,1.85],'energy' if left else 'signal'),
  cube([x-.2,10.1,2.3],[1.65,.45,1.75],'copper_dark'), cube([x-.15,12.0,2.4],[1.55,.5,1.55],'energy_hi'),
  cube([x+.25*s,12.45,2.65],[.7,3.2,.7],'ceramic',pivot=[x+.6*s,12.6,3.0],rotation=[0,0,10*s])
 ]}

def tail_system():
 return [
  {'name':'ouros_v4_tail_bus','parent':'tail2','pivot':[0,10,10],'cubes':[
   cube([-.65,8.0,5.0],[1.3,4.0,8.3],'rubber'), cube([-.9,9.1,6.0],[1.8,.7,6.1],'copper'),
   cube([-.95,11.1,7.0],[1.9,.55,4.7],'energy'), cube([-1.15,11.65,11.3],[2.3,1.15,2.2],'ceramic')
  ]},
  {'name':'ouros_v4_tail_fin','parent':'tail3','pivot':[0,16,15],'cubes':[
   cube([-1.0,12.8,9.8],[2.0,5.8,7.4],'stormcloth_hi'),
   cube([-1.35,14.0,14.7],[2.7,.8,4.9],'copper',pivot=[0,14.4,15.0],rotation=[0,0,0]),
   cube([-1.55,17.7,15.4],[3.1,.65,4.1],'energy_hi')
  ]}
 ]

def cosmetics(): return [visor_cowl(),mantle_shell(),power_frame(),conductor('left'),conductor('right'),*tail_system()]

def recolor(src:Path,dst:Path,shiny=False):
 im=Image.open(src).convert('RGBA'); px=im.load()
 for y in range(im.height):
  for x in range(im.width):
   r,g,b,a=px[x,y]
   if a==0: continue
   # Preserve blacks/eye features and saturated red cheeks; transform fur into storm-suit material blocking.
   if max(r,g,b)<55 or (r>150 and g<90 and b<90): continue
   if r>150 and g>105 and b<105:
    base=(42,56,70,255) if not shiny else (57,48,72,255)
    if (x//8+y//8)%3==0: base=(65,82,98,255) if not shiny else (82,65,96,255)
    px[x,y]=base
   elif r>120 and g>95:
    px[x,y]=(180,190,194,255) if not shiny else (160,170,186,255)
 for name,(x,y) in PIX.items(): px[x,y]=PALETTE[name]
 im.save(dst)

def build(model:Path,texture:Path,out_model:Path,out_tex:Path,ident:str,shiny=False):
 data=json.loads(model.read_text()); original=copy.deepcopy(data['minecraft:geometry'][0]['bones'])
 geo=data['minecraft:geometry'][0]; geo['description']['identifier']=ident; geo['bones']=original+cosmetics()
 out_model.write_text(json.dumps(data,separators=(',',':'))+'\n')
 recolor(texture,out_tex,shiny)
 assert geo['bones'][:len(original)]==original
 assert [b['name'] for b in geo['bones'][:len(original)]]==[b['name'] for b in original]

def main():
 p=argparse.ArgumentParser(); p.add_argument('--male-model',type=Path,required=True); p.add_argument('--female-model',type=Path,required=True)
 p.add_argument('--texture',type=Path,required=True); p.add_argument('--shiny',type=Path,required=True); p.add_argument('--output-root',type=Path,required=True); a=p.parse_args()
 a.output_root.mkdir(parents=True,exist_ok=True)
 for sex,src in [('male',a.male_model),('female',a.female_model)]:
  build(src,a.texture,a.output_root/f'ouros_storm_courier_v4_pikachu_{sex}.geo.json',a.output_root/'ouros_storm_courier_v4_pikachu.png',f'geometry.ouros_storm_courier_v4_pikachu_{sex}')
 build(a.male_model,a.shiny,a.output_root/'_verify_shiny.geo.json',a.output_root/'ouros_storm_courier_v4_pikachu_shiny.png','geometry.ouros_storm_courier_v4_pikachu_male',True)
 (a.output_root/'_verify_shiny.geo.json').unlink()
 print('cosmetic bones',len(cosmetics()),'cosmetic cubes',sum(len(b.get('cubes',[])) for b in cosmetics()))
if __name__=='__main__': main()
